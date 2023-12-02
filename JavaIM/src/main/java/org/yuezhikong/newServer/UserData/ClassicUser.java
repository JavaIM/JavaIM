package org.yuezhikong.newServer.UserData;

import cn.hutool.crypto.symmetric.AES;
import org.jetbrains.annotations.Nullable;
import org.yuezhikong.CodeDynamicConfig;
import org.yuezhikong.NetworkManager;
import org.yuezhikong.newServer.ServerMain;
import org.yuezhikong.newServer.UserData.Authentication.IUserAuthentication;
import org.yuezhikong.newServer.plugin.event.events.UserLoginEvent;
import org.yuezhikong.utils.DataBase.Database;
import org.yuezhikong.utils.SaveStackTrace;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class ClassicUser implements user {
    //初始化
    /**
     * 创建一个新的用户
     * @param NetworkData 客户端网络数据
     * @param ClientID 客户端ID
     * @param isServer 是否是服务端
     */
    public ClassicUser(NetworkManager.NetworkData NetworkData, int ClientID, boolean isServer)
    {
        UserNetworkData = NetworkData;
        this.ClientID = ClientID;
        Server = isServer;
    }

    //用户通讯相关
    private NetworkManager.NetworkData UserNetworkData;
    private String PublicKey;
    private AES UserAES;
    private ServerMain.RecvMessageThread recvMessageThread;
    private final int ClientID;

    @Override
    public ClassicUser setRecvMessageThread(ServerMain.RecvMessageThread thread) {
        recvMessageThread = thread;
        return this;
    }

    @Override
    public ServerMain.RecvMessageThread getRecvMessageThread() {
        return recvMessageThread;
    }

    @Override
    public NetworkManager.NetworkData getUserNetworkData() {
        return UserNetworkData;
    }

    @Override
    public ClassicUser setPublicKey(String publicKey) {
        PublicKey = publicKey;
        return this;
    }

    @Override
    public String getPublicKey() {
        return PublicKey;
    }

    @Override
    public ClassicUser setUserAES(AES userAES) {
        UserAES = userAES;
        return this;
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
    public ClassicUser SetUserPermission(int permissionLevel, boolean FlashPermission) {
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
                    ps.setString(2, ClassicUser.this.getUserName());
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
        return this;
    }

    @Override
    public Permission getUserPermission() {
        return PermissionLevel;
    }

    @Override
    public ClassicUser SetUserPermission(Permission permission)
    {
        PermissionLevel = permission;
        return this;
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
    public ClassicUser setAllowedTransferProtocol(boolean allowedTransferProtocol) {
        TransferProtocol = allowedTransferProtocol;
        return this;
    }

    //暂缓
    @Override
    public ClassicUser setMuteTime(long muteTime) {
        return this;
    }

    @Override
    public ClassicUser setMuted(boolean Muted) {
        return this;
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
    public ClassicUser UserDisconnect() {
        if (Disconnected)
        {
            return this;
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
        if (UserNetworkData != null)
        {
            try {
                NetworkManager.ShutdownTCPConnection(UserNetworkData);
            } catch (IOException e) {
                SaveStackTrace.saveStackTrace(e);
            }
        }
        UserNetworkData = null;
        PublicKey = null;
        UserAES = null;
        recvMessageThread.interrupt();
        recvMessageThread = null;
        return this;
    }
    //用户登录完成的event call
    @Override
    public ClassicUser UserLogin(String UserName) {
        UserLoginEvent userLoginEvent = new UserLoginEvent(UserName);
        ServerMain.getServer().getPluginManager().callEvent(userLoginEvent);
        return this;
    }
    //重定向到UserAuthentication
    @Override
    public ClassicUser addLoginRecall(IUserAuthentication.UserRecall code) {
        authentication.RegisterLoginRecall(code);
        return this;
    }

    @Override
    public ClassicUser addDisconnectRecall(IUserAuthentication.UserRecall code) {
        authentication.RegisterLogoutRecall(code);
        return this;
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
    public ClassicUser setUserAuthentication(@Nullable IUserAuthentication Authentication) {
        authentication = Authentication;
        return this;
    }

    @Override
    public @Nullable IUserAuthentication getUserAuthentication() {
        return authentication;
    }
}
