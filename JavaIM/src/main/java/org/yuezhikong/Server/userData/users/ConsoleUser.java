package org.yuezhikong.Server.userData.users;

import org.jetbrains.annotations.Nullable;
import org.yuezhikong.Server.IServer;
import org.yuezhikong.Server.userData.Permission;
import org.yuezhikong.Server.userData.auth.IUserAuthentication;
import org.yuezhikong.Server.userData.user;
import org.yuezhikong.Server.userData.userInformation;

public class ConsoleUser implements user {

    public ConsoleUser() {
        try {
            Class.forName(new Throwable().getStackTrace()[1].getClassName()).asSubclass(IServer.class);
        } catch (ClassCastException | ClassNotFoundException e) {
            throw new UnsupportedOperationException("only Server can create Console User!");
        }
    }

    @Override
    public String getUserName() {
        return "Server";
    }

    @Override
    public user onUserLogin(String UserName) {
        throw new UnsupportedOperationException("Server can not login");
    }

    @Override
    public boolean isUserLogged() {
        return true;
    }

    @Override
    public user disconnect() {
        throw new UnsupportedOperationException("Server can not disconnect");
    }

    @Override
    public user setUserPermission(int permissionLevel) {
        throw new UnsupportedOperationException("Server can not set permission");
    }

    @Override
    public Permission getUserPermission() {
        return Permission.ADMIN;
    }

    @Override
    public boolean isServer() {
        return true;
    }

    @Override
    public boolean isAllowedTransferProtocol() {
        return false;
    }

    @Override
    public user setAllowedTransferProtocol(boolean allowedTransferProtocol) {
        throw new UnsupportedOperationException("Server can not set TransferProtocol");
    }

    @Override
    public user addLoginRecall(IUserAuthentication.UserRecall code) {
        throw new UnsupportedOperationException("Server can not login");
    }

    @Override
    public user addDisconnectRecall(IUserAuthentication.UserRecall code) {
        throw new UnsupportedOperationException("Server can not disconnect");
    }

    @Override
    public user setUserAuthentication(@Nullable IUserAuthentication Authentication) {
        throw new UnsupportedOperationException("Server can not use authentication");
    }

    @Override
    public @Nullable IUserAuthentication getUserAuthentication() {
        throw new UnsupportedOperationException("Server can not use authentication");
    }

    @Override
    public void setUserInformation(userInformation userInformation) {
        throw new UnsupportedOperationException("Server not in database");
    }

    @Override
    public userInformation getUserInformation() {
        throw new UnsupportedOperationException("server not in database");
    }
}
