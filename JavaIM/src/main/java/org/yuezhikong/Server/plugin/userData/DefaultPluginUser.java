package org.yuezhikong.Server.plugin.userData;

import org.jetbrains.annotations.Nullable;
import org.yuezhikong.CodeDynamicConfig;
import org.yuezhikong.Server.ServerTools;
import org.yuezhikong.Server.UserData.Authentication.IUserAuthentication;
import org.yuezhikong.Server.UserData.Permission;
import org.yuezhikong.Server.UserData.user;
import org.yuezhikong.utils.DataBase.Database;
import org.yuezhikong.utils.SaveStackTrace;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

@SuppressWarnings("unused")
public abstract class DefaultPluginUser implements PluginUser{

    private IUserAuthentication authentication;
    @Override
    public String getUserName() {
        return authentication.getUserName();
    }

    @Override
    public boolean isUserLogined() {
        return authentication.isLogin();
    }

    private boolean Disconnected = false;
    @Override
    public synchronized user UserDisconnect() {
        if (Disconnected)
            return this;
        Disconnected = true;
        if (authentication != null)
            authentication.DoLogout();
        if (!isUserLogined())
        {
            ServerTools.getServerInstanceOrThrow().getLogger().info("一个客户端已经断开连接");
        }
        else
        {
            ServerTools.getServerInstanceOrThrow().getLogger().info("用户："+getUserName()+"已经断开连接");
        }
        return this;
    }

    private Permission permission;
    @Override
    public user SetUserPermission(int permissionLevel, boolean FlashPermission) {
        if (!FlashPermission)
        {
            ServerTools.getServerInstanceOrThrow().getIOThreadPool().execute(() -> {
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
    public user SetUserPermission(Permission permission) {
        this.permission = permission;
        return this;
    }

    @Override
    public Permission getUserPermission() {
        return permission;
    }

    @Override
    public user addLoginRecall(IUserAuthentication.UserRecall code) {
        authentication.RegisterLoginRecall(code);
        return this;
    }

    @Override
    public user addDisconnectRecall(IUserAuthentication.UserRecall code) {
        authentication.RegisterLogoutRecall(code);
        return this;
    }

    @Override
    public user setUserAuthentication(@Nullable IUserAuthentication Authentication) {
        authentication = Authentication;
        return this;
    }

    @Override
    public @Nullable IUserAuthentication getUserAuthentication() {
        return authentication;
    }
}
