package org.yuezhikong.newServer.UserData;

import cn.hutool.crypto.symmetric.AES;
import org.yuezhikong.CodeDynamicConfig;
import org.yuezhikong.newServer.ServerMain;
import org.yuezhikong.newServer.ServerMain.RecvMessageThread;
import org.yuezhikong.utils.DataBase.Database;
import org.yuezhikong.utils.SaveStackTrace;

import java.net.Socket;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

@SuppressWarnings("unused")
public class user {
    private String UserName;
    private String PublicKey;
    private final int ClientID;
    private Socket UserSocket;
    private boolean UserLogined;
    private RecvMessageThread recvMessageThread;
    private Permission PermissionLevel;
    private AES UserAES;
    private final boolean Server;
    public user(Socket socket, int ClientID,boolean isServer)
    {
        UserSocket = socket;
        this.ClientID = ClientID;
        UserLogined = false;
        Server = isServer;
    }

    public void setRecvMessageThread(RecvMessageThread thread)
    {
        recvMessageThread = thread;
    }

    public RecvMessageThread getRecvMessageThread() {
        return recvMessageThread;
    }

    public Socket getUserSocket() {
        return UserSocket;
    }

    public void setPublicKey(String publicKey) {
        PublicKey = publicKey;
    }

    public String getPublicKey() {
        return PublicKey;
    }

    public void setUserAES(AES userAES) {
        UserAES = userAES;
    }

    public AES getUserAES() {
        return UserAES;
    }

    public int getClientID() {
        return ClientID;
    }

    public String getUserName() {
        return UserName;
    }
    public void UserLogin(String UserName)
    {
        this.UserName = UserName;
        UserLogined = true;
    }

    public boolean isUserLogined() {
        return UserLogined;
    }

    public void UserDisconnect() {
        recvMessageThread.interrupt();
        UserSocket = null;
        UserName = null;
        PublicKey = null;
        UserLogined = false;
        UserAES = null;
    }

    public void SetUserPermission(int permissionLevel, boolean FlashPermission) {
        if (!FlashPermission)
        {
            ServerMain.getServer().getLogger().info("权限发生更新，用户："+getUserName()+"获得了"+permissionLevel+"级别权限");
            new Thread()
            {
                @Override
                public void run() {
                    this.setName("SQL Worker");
                    try (Connection DatabaseConnection = Database.Init(CodeDynamicConfig.GetMySQLDataBaseHost(), CodeDynamicConfig.GetMySQLDataBasePort(), CodeDynamicConfig.GetMySQLDataBaseName(), CodeDynamicConfig.GetMySQLDataBaseUser(), CodeDynamicConfig.GetMySQLDataBasePasswd()))
                    {
                        String sql = "UPDATE UserData SET Permission = ? where UserName = ?";
                        PreparedStatement ps = DatabaseConnection.prepareStatement(sql);
                        ps.setInt(1,permissionLevel);
                        ps.setString(2,user.this.getUserName());
                        ps.executeUpdate();
                    } catch (ClassNotFoundException e)
                    {
                        ServerMain.getServer().getLogger().error("错误，无法加载数据库的JDBC");
                        ServerMain.getServer().getLogger().error("请检查您的数据库！");
                        SaveStackTrace.saveStackTrace(e);
                    }
                    catch (SQLException e)
                    {
                        SaveStackTrace.saveStackTrace(e);
                    }
                }
            }.start();
        }
        PermissionLevel = Permission.ToPermission(permissionLevel);
    }

    public Permission getUserPermission() {
        return PermissionLevel;
    }

    public boolean isServer() {
        return Server;
    }

    //被暂缓的服务端管理功能
    public void setMuteTime(long muteTime) {
    }

    public void setMuted(boolean Muted) {
    }

}
