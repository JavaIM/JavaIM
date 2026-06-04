package org.yuezhikong.Server.userData.auth;

import com.google.gson.Gson;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.session.SqlSession;
import org.jetbrains.annotations.NotNull;
import org.yuezhikong.Server.IServer;
import org.yuezhikong.Server.ServerTools;
import org.yuezhikong.Server.userData.Permission;
import org.yuezhikong.Server.userData.user;
import org.yuezhikong.Server.userData.userInformation;
import org.yuezhikong.Server.api.api;
import org.yuezhikong.Server.plugin.PluginManager;
import org.yuezhikong.Server.plugin.event.events.User.auth.PreLoginEvent;
import org.yuezhikong.utils.protocol.SystemProtocol;
import org.yuezhikong.utils.SHA256;
import org.yuezhikong.utils.checks;
import org.yuezhikong.utils.database.dao.userInformationDao;
import org.yuezhikong.utils.totp.RecoveryCode;
import org.yuezhikong.utils.totp.TOTPCode;

import javax.security.auth.login.AccountNotFoundException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Slf4j
public final class UserAuthentication implements IUserAuthentication {

    //用户数据区
    private volatile boolean UserLogged = false;
    private String UserName = "";
    private final user User;

    //回调区
    private final List<UserRecall> LoginRecalls = new ArrayList<>();
    private final Object LoginRecallLock = new Object();

    private final PluginManager pluginManager;
    private final api serverAPI;
    private boolean requiredExtendLoginInformation = false;

    /**
     * 实例化用户Auth实现
     *
     * @param User   用户
     * @param server 服务器实例
     */
    public UserAuthentication(user User, IServer server) {
        this.User = User;
        pluginManager = server.getPluginManager();
        serverAPI = server.getServerAPI();
    }

    @Override
    public boolean doLogin(String Token) {
        try {
            if (!DoTokenLogin0(Token))
                return false;

            if (Logouted)
                return false;
            synchronized (LoginRecalls) {
                for (UserRecall recall : LoginRecalls) {
                    recall.run(User);
                }
                LoginRecalls.clear();
            }
            return true;
        } catch (Throwable throwable) {
            log.error("用户登录流程出错，出现异常", throwable);
            serverAPI.sendMessageToUser(User, "执行登录时出现内部错误，当前Unix时间：" + System.currentTimeMillis() + "请联系服务器管理员");
            return false;
        }
    }

    private boolean DoTokenLogin0(String Token) {
        try {
            SqlSession sqlSession = ServerTools.getServerInstanceOrThrow().getSqlSession();
            userInformationDao mapper = sqlSession.getMapper(userInformationDao.class);
            userInformation information = mapper.getUser(null, null, Token, null);
            if (information != null) {
                UserName = information.getUserName();
                try {
                    serverAPI.getUserByUserName(UserName);
                    //说明目前是已经有同一名字的用户登录了
                    //因此，禁止登录
                    SystemProtocol protocol = new SystemProtocol();
                    protocol.setType("Login");
                    protocol.setMessage("Already Logged");
                    String json = new Gson().toJson(protocol);
                    serverAPI.sendJsonToClient(User, json, "SystemProtocol");
                    return false;
                } catch (AccountNotFoundException ignored) {
                }

                //检查更新
                CheckDatabaseUpgrade(mapper, information);

                //插件处理
                PreLoginEvent event = new PreLoginEvent(UserName, true);
                pluginManager.callEvent(event);
                if (event.isCancelled()) {
                    //插件要求禁止登录，所以直接关闭连接
                    SystemProtocol protocol = new SystemProtocol();
                    protocol.setType("Login");
                    protocol.setMessage("Authentication Failed");
                    String json = new Gson().toJson(protocol);
                    serverAPI.sendJsonToClient(User, json, "SystemProtocol");
                    return false;
                }
                UserLogged = true;
                User.onUserLogin(UserName);
                User.setUserInformation(information);

                SystemProtocol protocol = new SystemProtocol();
                protocol.setType("Login");
                protocol.setMessage("Success");
                String json = new Gson().toJson(protocol);
                serverAPI.sendJsonToClient(User, json, "SystemProtocol");

                if (User.getUserPermission().equals(Permission.BAN)) {
                    serverAPI.sendMessageToUser(User, "登录失败，此用户已被永久封禁");
                    return false;
                }
                return true;
            } else {
                SystemProtocol protocol = new SystemProtocol();
                protocol.setType("Login");
                protocol.setMessage("Authentication Failed");
                String json = new Gson().toJson(protocol);
                serverAPI.sendJsonToClient(User, json, "SystemProtocol");
                return false;
            }
        } catch (Throwable t) {
            log.error("出现错误!", t);
            SystemProtocol protocol = new SystemProtocol();
            protocol.setType("Login");
            protocol.setMessage("Authentication Failed");
            String json = new Gson().toJson(protocol);
            serverAPI.sendJsonToClient(User, json, "SystemProtocol");
            return false;
        }
    }

    private boolean PostUserNameAndPasswordLogin(String UserName, userInformation information) {
        this.UserName = UserName;
        if (Permission.toPermission(information.getPermission()).equals(Permission.BAN)) {
            serverAPI.sendMessageToUser(User, "登录失败，此用户已被永久封禁");
            return false;
        }
        SqlSession sqlSession = ServerTools.getServerInstance().getSqlSession();
        userInformationDao mapper = sqlSession.getMapper(userInformationDao.class);

        //插件处理
        PreLoginEvent event = new PreLoginEvent(UserName, false);
        pluginManager.callEvent(event);
        if (event.isCancelled()) {
            //插件要求禁止登录，所以直接关闭连接
            SystemProtocol protocol = new SystemProtocol();
            protocol.setType("Login");
            protocol.setMessage("Authentication Failed");
            String json = new Gson().toJson(protocol);
            serverAPI.sendJsonToClient(User, json, "SystemProtocol");
            return false;
        }

        String token;
        userInformation tempInformation;
        do {
            //寻找一个安全的，不重复的token
            token = UUID.randomUUID().toString();
            tempInformation = mapper.getUser(null, null, token, null);
        } while (tempInformation != null);
        information.setToken(token);
        User.setUserInformation(information);

        String totpSecret = User.getUserInformation().getTotpSecret();
        if (totpSecret != null && !totpSecret.isEmpty()) {
            // TOTP 2fa 增强安全性
            //发送给用户
            SystemProtocol protocolData = new SystemProtocol();
            protocolData.setType("TOTP");
            protocolData.setMessage("Require TOTP Code");
            String json = new Gson().toJson(protocolData);
            serverAPI.sendJsonToClient(User, json, "SystemProtocol");
            requiredExtendLoginInformation = true;
            return true;
        }

        //发送给用户
        SystemProtocol protocolData = new SystemProtocol();
        protocolData.setType("Login");
        protocolData.setMessage(token);
        String json = new Gson().toJson(protocolData);
        serverAPI.sendJsonToClient(User, json, "SystemProtocol");
        //设置登录成功
        UserLogged = true;
        User.onUserLogin(UserName);
        return true;
    }

    private boolean DoPasswordLogin0(String UserName, String Password) {
        if ("Server".equals(UserName)) {
            serverAPI.sendMessageToUser(User, "禁止使用受保护的用户名：Server");
            SystemProtocol protocol = new SystemProtocol();
            protocol.setType("Login");
            protocol.setMessage("Authentication Failed");
            String json = new Gson().toJson(protocol);
            serverAPI.sendJsonToClient(User, json, "SystemProtocol");
            return false;
        }
        if (UserName == null || Password == null || UserName.isEmpty() || Password.isEmpty()) {
            serverAPI.sendMessageToUser(User, "禁止使用空字符串！");
            SystemProtocol protocol = new SystemProtocol();
            protocol.setType("Login");
            protocol.setMessage("Authentication Failed");
            String json = new Gson().toJson(protocol);
            serverAPI.sendJsonToClient(User, json, "SystemProtocol");
            return false;
        }
        try {
            serverAPI.getUserByUserName(UserName);
            //说明目前是已经有同一名字的用户登录了
            //因此，禁止登录
            SystemProtocol protocol = new SystemProtocol();
            protocol.setType("Login");
            protocol.setMessage("Already Logged");
            String json = new Gson().toJson(protocol);
            serverAPI.sendJsonToClient(User, json, "SystemProtocol");
            return false;
        } catch (AccountNotFoundException ignored) {
        }
        try {
            SqlSession sqlSession = ServerTools.getServerInstance().getSqlSession();
            userInformationDao mapper = sqlSession.getMapper(userInformationDao.class);
            userInformation userInformation = mapper.getUser(null, UserName, null, null);
            if (userInformation != null) {
                //登录代码
                String salt;
                String sha256;
                salt = userInformation.getSalt();
                //为保护安全，保存密码是加盐sha256，只有对密码处理后，才能进行比较
                sha256 = SHA256.sha256(Password + salt);
                if (userInformation.getPasswd().equals(sha256)) {
                    // 检查数据库更新
                    CheckDatabaseUpgrade(mapper, userInformation);
                    return PostUserNameAndPasswordLogin(UserName, userInformation);
                } else {
                    serverAPI.sendMessageToUser(User, "登录失败，用户名或密码错误");
                    SystemProtocol protocol = new SystemProtocol();
                    protocol.setType("Login");
                    protocol.setMessage("Authentication Failed");
                    String json = new Gson().toJson(protocol);
                    serverAPI.sendJsonToClient(User, json, "SystemProtocol");
                    return false;
                }
            } else {
                //注册代码
                String salt;
                userInformation tempInformation;
                do {
                    //寻找一个安全的盐
                    salt = UUID.randomUUID().toString();
                    tempInformation = mapper.getUser(null, null, null, salt);
                } while (tempInformation != null);
                //密码加盐并保存
                String sha256 = SHA256.sha256(Password + salt);
                userInformation = new userInformation();
                userInformation.setPasswd(sha256);
                userInformation.setPermission(0);
                userInformation.setSalt(salt);
                userInformation.setToken("");
                userInformation.setUserId("");
                userInformation.setAvatar("");
                userInformation.setUserName(UserName);

                mapper.addUser(userInformation);
                CheckDatabaseUpgrade(mapper, userInformation);
                return PostUserNameAndPasswordLogin(UserName, userInformation);
            }
        } catch (Throwable t) {
            log.error("出现错误!", t);
            SystemProtocol protocol = new SystemProtocol();
            protocol.setType("Login");
            protocol.setMessage("Authentication Failed");
            String json = new Gson().toJson(protocol);
            serverAPI.sendJsonToClient(User, json, "SystemProtocol");
            return false;
        }
    }

    /**
     * 检查数据库更新
     *
     * @param mapper          dao层操作方法
     * @param userInformation 用户信息
     */
    private void CheckDatabaseUpgrade(@NotNull userInformationDao mapper, @NotNull userInformation userInformation) {
        if (userInformation.getUserId() == null || userInformation.getUserId().isEmpty()) {// 如果没有分配用户ID
            String randomUUID = null;
            do {
                String tmpUUID = UUID.randomUUID().toString();
                if (mapper.getUser(tmpUUID, null, null, null) != null)
                    continue;
                randomUUID = tmpUUID;
            } while (randomUUID == null);
            userInformation.setUserId(randomUUID);
        }

        // 如果是null，就给他更新到空文本
        if (userInformation.getAvatar() == null)
            userInformation.setAvatar("");
        mapper.updateUser(userInformation);
    }

    @Override
    public boolean doLogin(String UserName, String Password) {
        try {
            if (!DoPasswordLogin0(UserName, Password))
                return false;

            if (Logouted)
                return false;
            synchronized (LoginRecalls) {
                for (UserRecall recall : LoginRecalls) {
                    recall.run(User);
                }
                LoginRecalls.clear();
            }
            return true;
        } catch (Throwable throwable) {
            log.error("用户登录流程出错，出现异常", throwable);
            serverAPI.sendMessageToUser(User, "执行登录时出现内部错误，当前Unix时间：" + System.currentTimeMillis() + "请联系服务器管理员");
            return false;
        }
    }


    @Override
    public boolean isLogin() {
        return UserLogged;
    }

    @Override
    public void registerLoginRecall(UserRecall runnable) {
        if (!UserLogged) {
            synchronized (LoginRecallLock) {
                if (!UserLogged) {
                    LoginRecalls.add(runnable);
                }
            }
        } else {
            runnable.run(User);
        }
    }

    @Override
    public String getUserName() {
        return UserName;
    }

    private volatile boolean Logouted = false;
    private final List<UserRecall> DisconnectRecall = new ArrayList<>();
    private final Object DisconnectRecallLock = new Object();

    @Override
    public void registerLogoutRecall(UserRecall runnable) {
        if (!Logouted) {
            synchronized (DisconnectRecallLock) {
                if (!Logouted) {
                    DisconnectRecall.add(runnable);
                    return;
                }
            }
        }
        runnable.run(User);
    }

    @Override
    public boolean doLogout() {
        if (!UserLogged || Logouted) {
            return false;
        }
        synchronized (DisconnectRecallLock) {
            if (!Logouted) {
                for (UserRecall runnable : DisconnectRecall) {
                    runnable.run(User);
                }
                DisconnectRecall.clear();
            } else {
                return false;
            }
        }
        Logouted = true;
        UserLogged = false;
        return true;
    }

    @Override
    public void extendSecurity(String totpCode) throws IllegalStateException {
        checks.checkState(UserLogged, "user already logged!");
        checks.checkState(!requiredExtendLoginInformation, "Not required extend login information!");
        requiredExtendLoginInformation = false;
        if (TOTPCode.verifyTOTPCode(User, totpCode)) {
            serverAPI.sendMessageToUser(User, "TOTP一次性代码验证成功");
        }
        else if (RecoveryCode.verifyRecoveryCode(totpCode, User)) {
            serverAPI.sendMessageToUser(User, "恢复代码验证成功");
            serverAPI.sendMessageToUser(User, "此恢复代码已失效");
        }
        else {
            serverAPI.sendMessageToUser(User, "无效的一次性代码/恢复代码");
            User.disconnect();
        }
        SystemProtocol protocolData = new SystemProtocol();
        protocolData.setType("Login");
        protocolData.setMessage(User.getUserInformation().getToken());
        String json = new Gson().toJson(protocolData);
        serverAPI.sendJsonToClient(User, json, "SystemProtocol");
        //设置登录成功
        UserLogged = true;
        User.onUserLogin(UserName);
    }
}
