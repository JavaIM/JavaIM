package org.yuezhikong.newServer.UserData;

import cn.hutool.crypto.symmetric.AES;
import org.jetbrains.annotations.Nullable;
import org.yuezhikong.CodeDynamicConfig;
import org.yuezhikong.newServer.ServerMain;
import org.yuezhikong.newServer.UserData.Authentication.IUserAuthentication;
import org.yuezhikong.newServer.plugin.event.events.UserLoginEvent;
import org.yuezhikong.utils.DataBase.Database;
import org.yuezhikong.utils.SaveStackTrace;

import java.io.IOException;
import java.net.Socket;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class SimpleUser implements user {
    //初始化
    /**
     * 创建一个新的用户
     * @param socket 客户端套链字
     * @param ClientID 客户端ID
     * @param isServer 是否是服务端
     */
    public SimpleUser(Socket socket, int ClientID,boolean isServer)
    {
        UserSocket = socket;
        this.ClientID = ClientID;
        Server = isServer;
    }

    //用户通讯相关
    private Socket UserSocket;
    private String PublicKey;
    private AES UserAES;
    private ServerMain.RecvMessageThread recvMessageThread;
    private final int ClientID;

    @Override
    public void setRecvMessageThread(ServerMain.RecvMessageThread thread) {
        recvMessageThread = thread;
    }

    @Override
    public ServerMain.RecvMessageThread getRecvMessageThread() {
        return recvMessageThread;
    }

    @Override
    public Socket getUserSocket() {
        return UserSocket;
    }

    @Override
    public void setPublicKey(String publicKey) {
        PublicKey = publicKey;
    }

    @Override
    public String getPublicKey() {
        return PublicKey;
    }

    @Override
    public void setUserAES(AES userAES) {
        UserAES = userAES;
    }

    @Override
    public AES getUserAES() {
        return UserAES;
    }

    @Override
    public int getClientID() {
        return ClientID;
    }
    //用户权限相关
    private Permission PermissionLevel;
    @Override
    public void SetUserPermission(int permissionLevel, boolean FlashPermission) {
        ServerMain.getServer().getLogger().info("权限发生更新，用户："+getUserName()+"获得了"+permissionLevel+"级别权限");
        if (!FlashPermission)
        {
            ServerMain.getServer().getIOThreadPool().execute(() -> {
                try
                {
                    Connection DatabaseConnection = Database.Init(CodeDynamicConfig.GetMySQLDataBaseHost(), CodeDynamicConfig.GetMySQLDataBasePort(), CodeDynamicConfig.GetMySQLDataBaseName(), CodeDynamicConfig.GetMySQLDataBaseUser(), CodeDynamicConfig.GetMySQLDataBasePasswd());
                    String sql = "UPDATE UserData SET Permission = ? where UserName = ?";
                    PreparedStatement ps = DatabaseConnection.prepareStatement(sql);
                    ps.setInt(1,permissionLevel);
                    ps.setString(2,SimpleUser.this.getUserName());
                    ps.executeUpdate();
                } catch (Database.DatabaseException | SQLException e)
                {
                    SaveStackTrace.saveStackTrace(e);
                }
                finally {
                    Database.close();
                }
            });
        }
        PermissionLevel = Permission.ToPermission(permissionLevel);
    }

    @Override
    public Permission getUserPermission() {
        return PermissionLevel;
    }

    @Override
    public void SetUserPermission(Permission permission)
    {
        PermissionLevel = permission;
    }
    //用户特殊用户标识符
    private final boolean Server;

    @Override
    public boolean isServer() {
        return Server;
    }

    //TransferProtocol
    private boolean TransferProtocol = false;
    @Override
    public boolean isAllowedTransferProtocol() {
        return TransferProtocol;
    }

    @Override
    public void setAllowedTransferProtocol(boolean allowedTransferProtocol) {
        TransferProtocol = allowedTransferProtocol;
    }

    //暂缓
    @Override
    public void setMuteTime(long muteTime) {

    }

    @Override
    public void setMuted(boolean Muted) {

    }

    @Override
    public long getMuteTime() {
        return 0;
    }

    @Override
    public boolean getMuted() {
        return false;
    }

    //退出登录资源释放
    private boolean Disconnected = false;
    @Override
    public void UserDisconnect() {
        if (Disconnected)
        {
            return;
        }
        Disconnected = true;
        if (!isUserLogined())
        {
            ServerMain.getServer().getLogger().info("一个客户端已经断开连接");
        }
        else
        {
            ServerMain.getServer().getLogger().info("用户："+getUserName()+"已经断开连接");
        }
        authentication.DoLogout();
        if (UserSocket != null && !UserSocket.isClosed())
        {
            try {
                UserSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
                SaveStackTrace.saveStackTrace(e);
            }
        }
        UserSocket = null;
        PublicKey = null;
        UserAES = null;
        recvMessageThread.interrupt();
        recvMessageThread = null;
    }
    //用户登录完成的event call
    @Override
    public void UserLogin(String UserName) {
        UserLoginEvent userLoginEvent = new UserLoginEvent(UserName);
        ServerMain.getServer().getPluginManager().callEvent(userLoginEvent);
    }
    //重定向到UserAuthentication
    @Override
    public void addLoginRecall(Runnable code) {
        authentication.RegisterLoginRecall(code);
    }

    @Override
    public void addDisconnectRecall(Runnable code) {
        authentication.RegisterLogoutRecall(code);
    }

    @Override
    public String getUserName() {
        if (isServer())
        {
            //服务端虚拟用户特别处理
            return "Server";
        }
        return authentication.getUserName();
    }

    @Override
    public boolean isUserLogined() {
        if (isServer())
        {
            //服务端虚拟用户特别处理
            return true;
        }
        return authentication.isLogin();
    }

    //用户登录处理器
    private IUserAuthentication authentication;
    @Override
    public void setUserAuthentication(@Nullable IUserAuthentication Authentication) {
        authentication = Authentication;
    }

    @Override
    public @Nullable IUserAuthentication getUserAuthentication() {
        return authentication;
    }
}
