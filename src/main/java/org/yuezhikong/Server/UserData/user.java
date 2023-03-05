package org.yuezhikong.Server.UserData;

import org.yuezhikong.utils.CustomExceptions.ModeDisabledException;

import java.io.IOException;
import java.net.Socket;

import static org.yuezhikong.CodeDynamicConfig.GetRSA_Mode;

public class user {
    private int PermissionLevel = 0;
    private String UserName = "";
    private boolean UserLogined;
    private Socket UserSocket;
    private final int ClientID;
    private String UserPublicKey;
    private boolean PublicKeyChanged = false;
    private long MuteTime = 0;
    private boolean Muted = false;

    /**
     * 设置用户是否已被禁言
     * @param muted 是/否
     */
    public void setMuted(boolean muted)
    {
        Muted = muted;
        /*
        if (!muted) {
            if (!PluginManager.getInstance("./plugins").OnUserUnMute(this))
            {
                Muted = false;
            }
            else
            {
                org.yuezhikong.utils.Logger logger = new org.yuezhikong.utils.Logger();
                logger.info("插件系统阻止了解除禁言操作！");
            }
        }
        else
        {
            if (!PluginManager.getInstance("./plugins").OnUserMute(this))
            {
                Muted = true;
            }
            else
            {
                org.yuezhikong.utils.Logger logger = new org.yuezhikong.utils.Logger();
                logger.info("插件系统阻止了禁言操作");
            }
        }
        */
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
     * 设置用户权限级别
     * @param permissionLevel 权限等级
     * @param IsItARefresh 是否是刷新权限
     */
    public void SetUserPermission(int permissionLevel,boolean IsItARefresh)
    {
        PermissionLevel = permissionLevel;
        /*
        if (!IsItARefresh)//如果不是刷新登录
        {
            if (!PluginManager.getInstance("./plugins").OnUserPermissionChange(this,permissionLevel))//通知插件系统，发生权限更改
            {
                PermissionLevel = permissionLevel;//如果插件系统没有阻止操作，则进行设定
            }
            else
            {
                org.yuezhikong.utils.Logger logger = new org.yuezhikong.utils.Logger();
                logger.info("插件系统阻止了权限更改操作！");
            }
        }
        else
            PermissionLevel = permissionLevel;
         */
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
    public Socket GetUserSocket()
    {
        return UserSocket;
    }
    public int GetUserPermission()
    {
        return PermissionLevel;
    }
    public int GetUserClientID()
    {
        return ClientID;
    }
    public String GetUserPublicKey() throws Exception {
        if (!GetRSA_Mode())
        {
            throw new Exception("RSA Mode Has Disabled!");
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
