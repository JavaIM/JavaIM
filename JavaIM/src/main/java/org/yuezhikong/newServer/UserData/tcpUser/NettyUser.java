package org.yuezhikong.newServer.UserData.tcpUser;

import cn.hutool.crypto.symmetric.AES;
import io.netty.channel.Channel;
import org.jetbrains.annotations.Nullable;
import org.yuezhikong.CodeDynamicConfig;
import org.yuezhikong.newServer.NettyNetwork;
import org.yuezhikong.newServer.ServerTools;
import org.yuezhikong.newServer.UserData.Authentication.IUserAuthentication;
import org.yuezhikong.newServer.UserData.Permission;
import org.yuezhikong.utils.DataBase.Database;
import org.yuezhikong.utils.SaveStackTrace;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class NettyUser implements INettyUser
{
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
        return id;
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
        if (authentication != null)
            authentication.DoLogout();
        if (!isUserLogined())
        {
            ServerTools.getServerInstance().getLogger().info("客户端:"+getChannel().remoteAddress()+"已经断开连接");
        }
        else
        {
            String message = "用户："+getUserName()+"("+getChannel().remoteAddress()+")已经断开连接";
            ServerTools.getServerInstance().getLogger().info(message);
            ServerTools.getServerInstance().getServerAPI().SendMessageToAllClient(message);
        }
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

    private final int id;
    public NettyUser(Channel channel, NettyNetwork network,int ClientID) { ConnectChannel = channel; this.network = network; id = ClientID; }

    public NettyUser(boolean ServerSpicialUserStatus, NettyNetwork network) { server = ServerSpicialUserStatus; ConnectChannel = null; this.network = network; id = 0; }

    @Override
    public Channel getChannel() {
        return ConnectChannel;
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