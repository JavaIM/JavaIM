package org.yuezhikong.Server.UserData.Authentication;

import cn.hutool.crypto.SecureUtil;
import com.google.gson.Gson;
import org.yuezhikong.CodeDynamicConfig;
import org.yuezhikong.Server.IServer;
import org.yuezhikong.Server.UserData.Permission;
import org.yuezhikong.Server.UserData.user;
import org.yuezhikong.Server.api.api;
import org.yuezhikong.Server.plugin.PluginManager;
import org.yuezhikong.Server.plugin.event.events.User.auth.PreLoginEvent;
import org.yuezhikong.utils.DataBase.Database;
import org.yuezhikong.utils.Logger;
import org.yuezhikong.utils.Protocol.NormalProtocol;
import org.yuezhikong.utils.SaveStackTrace;

import javax.security.auth.login.AccountNotFoundException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import cn.hutool.core.lang.UUID;

public final class UserAuthentication implements IUserAuthentication{

    //用户数据区
    private volatile boolean UserLogged = false;
    private String UserName = "";
    private final user User;

    //回调区
    private final List<UserRecall> LoginRecalls = new ArrayList<>();
    private final Object LoginRecallLock = new Object();

    private final PluginManager pluginManager;

    private final Logger logger;
    private final api serverAPI;
    /**
     * 实例化用户Auth实现
     * @param User 用户
     * @param server 服务器实例
     */
    public UserAuthentication(user User, IServer server)
    {
        this.User = User;
        pluginManager = server.getPluginManager();
        serverAPI = server.getServerAPI();
        logger = server.getLogger();
    }

    @Override
    public boolean DoLogin(String Token) {
        try
        {
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
        } catch (Throwable throwable)
        {
            SaveStackTrace.saveStackTrace(throwable);
            logger.error("用户登录流程出错，出现异常，详情请查看日志文件");
            serverAPI.SendMessageToUser(User,"执行登录时出现内部错误，当前Unix时间："+System.currentTimeMillis()+"请联系服务器管理员");
            return false;
        }
    }

    private boolean DoTokenLogin0(String Token)
    {
        try
        {
            Connection DatabaseConnection = Database.Init(CodeDynamicConfig.GetMySQLDataBaseHost(), CodeDynamicConfig.GetMySQLDataBasePort(), CodeDynamicConfig.GetMySQLDataBaseName(), CodeDynamicConfig.GetMySQLDataBaseUser(), CodeDynamicConfig.GetMySQLDataBasePasswd());
            PreparedStatement ps = DatabaseConnection.prepareStatement("select * from UserData where token = ?");
            ps.setString(1,Token);
            ResultSet rs = ps.executeQuery();
            if (rs.next())
            {
                UserName = rs.getString("UserName");
                try {

                    serverAPI.GetUserByUserName(UserName);
                    //说明目前是已经有同一名字的用户登录了
                    //因此，禁止登录
                    NormalProtocol protocol = new NormalProtocol();
                    protocol.setType("Login");
                    protocol.setType("This user is currently online");
                    String json = new Gson().toJson(protocol);
                    serverAPI.SendJsonToClient(User,json, "NormalProtocol");
                    return false;
                } catch (AccountNotFoundException ignored) {}
                //插件处理
                PreLoginEvent event = new PreLoginEvent(UserName,true);
                pluginManager.callEvent(event);
                if (event.isCancel())
                {
                    //插件要求禁止登录，所以直接关闭连接
                    NormalProtocol protocol = new NormalProtocol();
                    protocol.setType("Login");
                    protocol.setMessage("Authentication Failed");
                    String json = new Gson().toJson(protocol);
                    serverAPI.SendJsonToClient(User,json, "NormalProtocol");
                    return false;
                }
                UserLogged = true;
                User.onUserLogin(UserName);
                NormalProtocol protocol = new NormalProtocol();
                protocol.setType("Login");
                protocol.setMessage("Success");
                String json = new Gson().toJson(protocol);
                serverAPI.SendJsonToClient(User,json, "NormalProtocol");
                return true;
            }
            else
            {
                NormalProtocol protocol = new NormalProtocol();
                protocol.setType("Login");
                protocol.setMessage("Authentication Failed");
                String json = new Gson().toJson(protocol);
                serverAPI.SendJsonToClient(User,json, "NormalProtocol");
                return false;
            }
        } catch (Database.DatabaseException | SQLException e) {
            SaveStackTrace.saveStackTrace(e);
            NormalProtocol protocol = new NormalProtocol();
            protocol.setType("Login");
            protocol.setMessage("Authentication Failed");
            String json = new Gson().toJson(protocol);
            serverAPI.SendJsonToClient(User,json, "NormalProtocol");
            return false;
        } finally {
            Database.close();
        }
    }

    private boolean PostUserNameAndPasswordLogin(String UserName,Connection DatabaseConnection,int PermissionLevel) throws SQLException {
        this.UserName = UserName;
        User.SetUserPermission(PermissionLevel,true);
        if (User.getUserPermission().equals(Permission.BAN))
        {
            serverAPI.SendMessageToUser(User,"登录失败，此用户已被永久封禁");
            return false;
        }
        String token;
        PreparedStatement ps;
        ResultSet rs;
        do {
            //获取一个安全的，不重复的token
            token = UUID.randomUUID(true).toString();
            ps = DatabaseConnection.prepareStatement("select * from UserData where token = ?");
            ps.setString(1, token);
            rs = ps.executeQuery();
        } while (rs.next());
        //将这个token填入数据库
        ps = DatabaseConnection.prepareStatement("UPDATE UserData SET token = ? where UserName = ?;");
        ps.setString(1, token);
        ps.setString(2, UserName);
        ps.executeUpdate();

        //插件处理
        PreLoginEvent event = new PreLoginEvent(UserName,false);
        pluginManager.callEvent(event);
        if (event.isCancel())
        {
            //插件要求禁止登录，所以直接关闭连接
            NormalProtocol protocol = new NormalProtocol();
            protocol.setType("Login");
            protocol.setMessage("Authentication Failed");
            String json = new Gson().toJson(protocol);
            serverAPI.SendJsonToClient(User,json, "NormalProtocol");
            return false;
        }

        //发送给用户
        NormalProtocol protocolData = new NormalProtocol();
        protocolData.setType("Login");
        protocolData.setMessage(token);
        String json = new Gson().toJson(protocolData);
        serverAPI.SendJsonToClient(User,json, "NormalProtocol");
        //设置登录成功
        UserLogged = true;
        User.onUserLogin(UserName);
        return true;
    }
    private boolean DoPasswordLogin0(String UserName, String Password)
    {
        if ("Server".equals(UserName))
        {
            serverAPI.SendMessageToUser(User,"禁止使用受保护的用户名：Server");
            NormalProtocol protocol = new NormalProtocol();
            protocol.setType("Login");
            protocol.setMessage("Authentication Failed");
            String json = new Gson().toJson(protocol);
            serverAPI.SendJsonToClient(User,json, "NormalProtocol");
            return false;
        }
        if (UserName == null || Password == null || UserName.isEmpty() || Password.isEmpty())
        {
            serverAPI.SendMessageToUser(User,"禁止使用空字符串！");
            NormalProtocol protocol = new NormalProtocol();
            protocol.setType("Login");
            protocol.setMessage("Authentication Failed");
            String json = new Gson().toJson(protocol);
            serverAPI.SendJsonToClient(User,json, "NormalProtocol");
            return false;
        }
        try {
            serverAPI.GetUserByUserName(UserName);
            //说明目前是已经有同一名字的用户登录了
            //因此，禁止登录
            NormalProtocol protocol = new NormalProtocol();
            protocol.setType("Login");
            protocol.setType("This user is currently online");
            String json = new Gson().toJson(protocol);
            serverAPI.SendJsonToClient(User,json, "NormalProtocol");
            return false;
        } catch (AccountNotFoundException ignored) {}
        try
        {
            Connection DatabaseConnection = Database.Init(CodeDynamicConfig.GetMySQLDataBaseHost(), CodeDynamicConfig.GetMySQLDataBasePort(), CodeDynamicConfig.GetMySQLDataBaseName(), CodeDynamicConfig.GetMySQLDataBaseUser(), CodeDynamicConfig.GetMySQLDataBasePasswd());
            PreparedStatement ps = DatabaseConnection.prepareStatement("select * from UserData where UserName = ?");
            ps.setString(1,UserName);
            ResultSet rs = ps.executeQuery();
            if (rs.next())
            {
                //登录代码
                String salt;
                String sha256;
                salt = rs.getString("salt");
                //为保护安全，保存密码是加盐sha256，只有对密码处理后，才能进行比较
                sha256 = SecureUtil.sha256(Password + salt);
                if (rs.getString("Passwd").equals(sha256))
                {
                    return PostUserNameAndPasswordLogin(UserName,DatabaseConnection,rs.getInt("Permission"));
                }
                else
                {
                    serverAPI.SendMessageToUser(User,"登录失败，用户名或密码错误");
                    NormalProtocol protocol = new NormalProtocol();
                    protocol.setType("Login");
                    protocol.setMessage("Authentication Failed");
                    String json = new Gson().toJson(protocol);
                    serverAPI.SendJsonToClient(User,json, "NormalProtocol");
                    return false;
                }
            }
            else
            {
                //注册代码
                String salt;
                do {
                    //寻找一个安全的盐
                    salt = UUID.randomUUID(true).toString();
                    ps = DatabaseConnection.prepareStatement("select * from UserData where salt = ?");
                    ps.setString(1, salt);
                    rs = ps.executeQuery();
                } while (rs.next());
                //密码加盐并保存
                String sha256 = SecureUtil.sha256(Password + salt);
                ps = DatabaseConnection.prepareStatement
                        ("INSERT INTO `UserData` (`Permission`,`UserName`, `Passwd`,`salt`) VALUES (0,?, ?, ?);");
                ps.setString(1, UserName);
                ps.setString(2, sha256);
                ps.setString(3, salt);
                ps.executeUpdate();
                return PostUserNameAndPasswordLogin(UserName,DatabaseConnection,0);
            }
        } catch (Database.DatabaseException | SQLException e) {
            SaveStackTrace.saveStackTrace(e);
            NormalProtocol protocol = new NormalProtocol();
            protocol.setType("Login");
            protocol.setMessage("Authentication Failed");
            String json = new Gson().toJson(protocol);
            serverAPI.SendJsonToClient(User,json, "NormalProtocol");
            return false;
        } finally {
            Database.close();
        }
    }

    @Override
    public boolean DoLogin(String UserName, String Password) {
        try
        {
            if (!DoPasswordLogin0(UserName,Password))
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
        } catch (Throwable throwable)
        {
            SaveStackTrace.saveStackTrace(throwable);
            logger.error("用户登录流程出错，出现异常，详情请查看日志文件");
            serverAPI.SendMessageToUser(User,"执行登录时出现内部错误，当前Unix时间："+System.currentTimeMillis()+"请联系服务器管理员");
            return false;
        }
    }


    @Override
    public boolean isLogin() {
        return UserLogged;
    }

    @Override
    public void RegisterLoginRecall(UserRecall runnable) {
        if (!UserLogged)
        {
            synchronized (LoginRecallLock)
            {
                if (!UserLogged)
                {
                    LoginRecalls.add(runnable);
                }
            }
        }
        else
        {
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
    public void RegisterLogoutRecall(UserRecall runnable) {
        if (!Logouted) {
            synchronized (DisconnectRecallLock) {
                if (!Logouted)
                {
                    DisconnectRecall.add(runnable);
                    return;
                }
            }
        }
        runnable.run(User);
    }

    @Override
    public boolean DoLogout() {
        if (!UserLogged || Logouted) {
            return false;
        }
        synchronized (DisconnectRecallLock)
        {
            if (!Logouted) {
                for (UserRecall runnable : DisconnectRecall) {
                    runnable.run(User);
                }
                DisconnectRecall.clear();
            }
            else
            {
                return false;
            }
        }
        Logouted = true;
        UserLogged = false;
        return true;
    }
}
