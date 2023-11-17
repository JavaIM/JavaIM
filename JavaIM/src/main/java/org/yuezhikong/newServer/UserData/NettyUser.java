package org.yuezhikong.newServer.UserData;

import cn.hutool.crypto.symmetric.AES;
import io.netty.channel.Channel;
import org.jetbrains.annotations.Nullable;
import org.yuezhikong.CodeDynamicConfig;
import org.yuezhikong.NetworkManager;
import org.yuezhikong.newServer.NettyNetwork;
import org.yuezhikong.newServer.ServerMain;
import org.yuezhikong.newServer.UserData.Authentication.IUserAuthentication;
import org.yuezhikong.utils.DataBase.Database;
import org.yuezhikong.utils.SaveStackTrace;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class NettyUser implements user
{
    @Override
    public void setRecvMessageThread(ServerMain.RecvMessageThread thread) {
        throw new RuntimeException("Deprecated");
    }

    @Override
    public ServerMain.RecvMessageThread getRecvMessageThread() {
        throw new RuntimeException("Deprecated");
    }

    @Override
    public NetworkManager.NetworkData getUserNetworkData() {
        throw new RuntimeException("Deprecated");
    }

    private String PublicKey;
    private AES aes;
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
        aes = userAES;
    }

    @Override
    public AES getUserAES() {
        return aes;
    }

    @Override
    public int getClientID() {
        throw new RuntimeException("Deprecated");
    }

    private IUserAuthentication authentication;
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
    public void UserLogin(String UserName) {

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

    private boolean Disconnected = false;
    @Override
    public synchronized void UserDisconnect() {
        if (Disconnected)
            return;
        Disconnected = true;
        if (!isServer())
            getChannel().close();
        authentication.DoLogout();
    }
    private Permission permission;
    @Override
    public void SetUserPermission(int permissionLevel, boolean FlashPermission) {
        if (!FlashPermission)
        {
            network.getIOThreadPool().execute(() -> {
                try
                {
                    Connection DatabaseConnection = Database.Init(CodeDynamicConfig.GetMySQLDataBaseHost(), CodeDynamicConfig.GetMySQLDataBasePort(), CodeDynamicConfig.GetMySQLDataBaseName(), CodeDynamicConfig.GetMySQLDataBaseUser(), CodeDynamicConfig.GetMySQLDataBasePasswd());
                    String sql = "UPDATE UserData SET Permission = ? where UserName = ?";
                    PreparedStatement ps = DatabaseConnection.prepareStatement(sql);
                    ps.setInt(1,permissionLevel);
                    ps.setString(2, getUserName());
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
        permission = Permission.ToPermission(permissionLevel);
    }

    @Override
    public void SetUserPermission(Permission permission) {
        this.permission = permission;
    }

    @Override
    public Permission getUserPermission() {
        return permission;
    }

    private boolean server = false;
    @Override
    public boolean isServer() {
        return server;
    }

    private final Channel ConnectChannel;
    private final NettyNetwork network;

    @SuppressWarnings("unused")
    public NettyUser(Channel channel, NettyNetwork network) { ConnectChannel = channel; this.network = network; }

    @SuppressWarnings("unused")
    public NettyUser(boolean ServerSpicialUserStatus, NettyNetwork network) { server = ServerSpicialUserStatus; ConnectChannel = null; this.network = network; }

    @SuppressWarnings("unused")
    public Channel getChannel() {
        return ConnectChannel;
    }

    @Override
    public void setMuteTime(long muteTime) {
        throw new RuntimeException("Deprecated");
    }

    @Override
    public void setMuted(boolean Muted) {
        throw new RuntimeException("Deprecated");
    }

    @Override
    public long getMuteTime() {
        throw new RuntimeException("Deprecated");
    }

    @Override
    public boolean getMuted() {
        throw new RuntimeException("Deprecated");
    }

    private boolean TransferProtocol;
    @Override
    public boolean isAllowedTransferProtocol() {
        return TransferProtocol;
    }

    @Override
    public void setAllowedTransferProtocol(boolean allowedTransferProtocol) {
        TransferProtocol = allowedTransferProtocol;
    }

    @Override
    public void addLoginRecall(IUserAuthentication.UserRecall code) {
        authentication.RegisterLoginRecall(code);
    }

    @Override
    public void addDisconnectRecall(IUserAuthentication.UserRecall code) {
        authentication.RegisterLogoutRecall(code);
    }

    @Override
    public void setUserAuthentication(@Nullable IUserAuthentication Authentication) {
        authentication = Authentication;
    }

    @Override
    public @Nullable IUserAuthentication getUserAuthentication() {
        return authentication;
    }
}