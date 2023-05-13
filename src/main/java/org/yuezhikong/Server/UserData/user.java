package org.yuezhikong.Server.UserData;

import cn.hutool.crypto.symmetric.AES;
import org.yuezhikong.CodeDynamicConfig;
import org.yuezhikong.Server.plugin.load.PluginManager;
import org.yuezhikong.utils.CustomExceptions.ModeDisabledException;

import java.io.IOException;
import java.net.Socket;
import java.util.Objects;

import static org.yuezhikong.CodeDynamicConfig.GetRSA_Mode;
import static org.yuezhikong.CodeDynamicConfig.isAES_Mode;

public class user {
    private int PermissionLevel = 0;
    private String UserName = "";
    private boolean UserLogined;
    private boolean Server;
    private Socket UserSocket;
    private final int ClientID;
    private String UserPublicKey;
    private boolean PublicKeyChanged = false;
    private long MuteTime = 0;
    private boolean Muted = false;
    private AES UserAES;
    private boolean AESChanged = false;

    /**
     * 设置用户是否已被禁言
     * @param muted 是/否
     */
    public void setMuted(boolean muted) {
        if (CodeDynamicConfig.GetPluginSystemMode()) {
            try {
                if (!muted) {
                    if (!Objects.requireNonNull(PluginManager.getInstance("./plugins")).OnUserUnMute(this)) {
                        Muted = false;
                    } else {
                        org.yuezhikong.utils.Logger logger = org.yuezhikong.Server.Server.GetInstance().logger;
                        logger.info("插件系统阻止了解除禁言操作！");
                    }
                } else {
                    if (!Objects.requireNonNull(PluginManager.getInstance("./plugins")).OnUserMute(this)) {
                        Muted = true;
                    } else {
                        org.yuezhikong.utils.Logger logger = org.yuezhikong.Server.Server.GetInstance().logger;
                        logger.info("插件系统阻止了禁言操作");
                    }
                }
            } catch (Throwable e)
            {
                Muted = muted;
            }
        }
        else
        {
            Muted = muted;
        }
    }

    /**
     * 设置用户禁言时长
     * @param muteTime 禁言时长
     * @apiNote 如果禁言时长为-1，且Muted为true，则认为为永久禁言
     */
    public void setMuteTime(long muteTime)
    {
        MuteTime = muteTime;
    }

    /**
     * 获取用户是否处于禁言状态
     * @return 是/否
     */
    public boolean isMuted() {
        return Muted;
    }

    /**
     * 获取用户禁言时长
     * @return 禁言时长
     * @apiNote 如果返回为-1，请将其处理为永久禁言
     */
    public long getMuteTime() {
        return MuteTime;
    }

    public user(Socket socket, int clientid)
    {
        UserSocket = socket;
        ClientID = clientid;
        UserLogined = false;
    }

    /**
     * 设置是否是服务端虚拟账户
     * @param server 是/否
     */
    public void setServer(boolean server) {
        Server = server;
    }

    /**
     * 查询是否是服务端虚拟账户
     * @return 是/否
     */
    public boolean isServer() {
        return Server;
    }

    public String GetUserName()
    {
        return UserName;
    }
    public boolean GetUserLogined()
    {
        return UserLogined;
    }
    public void UserLogin(String Username)
    {
        UserName = Username;
        UserLogined = true;
    }

    /**
     * 设置用户的公钥
     * @param UserPublickey 用户的公钥
     * @throws ModeDisabledException RSA功能被禁用时抛出此异常
     */
    public void SetUserPublicKey(String UserPublickey) throws ModeDisabledException {
        if (!GetRSA_Mode())
        {
            throw new ModeDisabledException("RSA Mode Has Disabled!");
        }
        if (!PublicKeyChanged) {
            UserPublicKey = UserPublickey;
            PublicKeyChanged = true;
        }
    }

    /**
     * 设置用户的AES
     * @param InputUserAES 用户的AES
     * @throws ModeDisabledException RSA功能/AES功能被禁用时抛出此异常
     */
    public void SetUserAES(AES InputUserAES) throws ModeDisabledException {
        if (!GetRSA_Mode())
        {
            throw new ModeDisabledException("RSA Mode Has Disabled!");
        }
        else if (!isAES_Mode())
        {
            throw new ModeDisabledException("AES Mode Has Disabled!");
        }
        else
        {
            if (!AESChanged)
            {
                UserAES = InputUserAES;
                AESChanged = true;
            }
        }
    }

    /**
     * 获取用户的AES
     * @return 用户的AES
     * @throws RuntimeException 用户是服务端虚拟用户
     */
    public AES GetUserAES() throws RuntimeException{
        if (Server)
        {
            throw new RuntimeException(new ModeDisabledException("Getting a socket does not apply to server-side virtual accounts (translated by cn.bing.com/translator)"));
        }
        else {
            return UserAES;
        }
    }

    /**
     * 设置用户权限级别
     * @param permissionLevel 权限等级
     * @param IsItARefresh 是否是刷新权限
     */
    public void SetUserPermission(int permissionLevel,boolean IsItARefresh)
    {
        if (CodeDynamicConfig.GetPluginSystemMode()) {
            if (!IsItARefresh)//如果不是刷新登录
            {
                try {
                    if (!Objects.requireNonNull(PluginManager.getInstance("./plugins")).OnUserPermissionChange(this, permissionLevel))//通知插件系统，发生权限更改
                    {
                        PermissionLevel = permissionLevel;//如果插件系统没有阻止操作，则进行设定
                    } else {
                        org.yuezhikong.utils.Logger logger = org.yuezhikong.Server.Server.GetInstance().logger;
                        logger.info("插件系统阻止了权限更改操作！");
                    }
                } catch (ModeDisabledException e) {
                    PermissionLevel = permissionLevel;
                }
            } else
                PermissionLevel = permissionLevel;
        }
        else
        {
            PermissionLevel = permissionLevel;
        }
    }
    public void UserDisconnect()
    {
        try {
            UserSocket.close();
        } catch (IOException | NullPointerException e)
        {
            org.yuezhikong.utils.SaveStackTrace.saveStackTrace(e);
        }
        UserSocket = null;
        UserPublicKey = null;
    }

    /**
     * 获取用户Socket
     * @return Socket
     * @throws RuntimeException 此账户是服务端虚拟账户
     */
    public Socket GetUserSocket() throws RuntimeException {
        if (!Server) {
            return UserSocket;
        } else {
            throw new RuntimeException(new ModeDisabledException("Getting a socket does not apply to server-side virtual accounts (translated by cn.bing.com/translator)"));
        }
    }
    public int GetUserPermission()
    {
        return PermissionLevel;
    }
    public int GetUserClientID()
    {
        return ClientID;
    }

    /**
     * 获取用户公钥
     * @return 用户公钥
     * @throws ModeDisabledException 用户是服务端虚拟账户
     */
    public String GetUserPublicKey() throws ModeDisabledException {
        if (!GetRSA_Mode())
        {
            throw new ModeDisabledException("RSA Mode Has Disabled!");
        }
        if (Server)
        {
            throw new ModeDisabledException("Getting a socket does not apply to server-side virtual accounts (translated by cn.bing.com/translator)");
        }
        if (UserPublicKey != null) {
            return UserPublicKey;
        }
        else
        {
            return null;
        }
    }
}
