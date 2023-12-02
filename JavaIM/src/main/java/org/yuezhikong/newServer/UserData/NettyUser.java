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
    public NettyUser setRecvMessageThread(ServerMain.RecvMessageThread thread) {
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
    public NettyUser setPublicKey(String publicKey) {
        PublicKey = publicKey;
        return this;
    }

    @Override
    public String getPublicKey() {
        return PublicKey;
    }

    @Override
    public NettyUser setUserAES(AES userAES) {
        aes = userAES;
        return this;
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
    public NettyUser UserLogin(String UserName) {
        return this;
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
    public synchronized NettyUser UserDisconnect() {
        if (Disconnected)
            return this;
        Disconnected = true;
        if (!isServer())
            getChannel().close();
        authentication.DoLogout();
        return this;
    }
    private Permission permission;
    @Override
    public NettyUser SetUserPermission(int permissionLevel, boolean FlashPermission) {
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
        return this;
    }

    @Override
    public NettyUser SetUserPermission(Permission permission) {
        this.permission = permission;
        return this;
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
    public NettyUser setMuteTime(long muteTime) {
        throw new RuntimeException("Deprecated");
    }

    @Override
    public NettyUser setMuted(boolean Muted) {
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
    public NettyUser setAllowedTransferProtocol(boolean allowedTransferProtocol) {
        TransferProtocol = allowedTransferProtocol;
        return this;
    }

    @Override
    public NettyUser addLoginRecall(IUserAuthentication.UserRecall code) {
        authentication.RegisterLoginRecall(code);
        return this;
    }

    @Override
    public NettyUser addDisconnectRecall(IUserAuthentication.UserRecall code) {
        authentication.RegisterLogoutRecall(code);
        return this;
    }

    @Override
    public NettyUser setUserAuthentication(@Nullable IUserAuthentication Authentication) {
        authentication = Authentication;
        return this;
    }

    @Override
    public @Nullable IUserAuthentication getUserAuthentication() {
        return authentication;
    }
}