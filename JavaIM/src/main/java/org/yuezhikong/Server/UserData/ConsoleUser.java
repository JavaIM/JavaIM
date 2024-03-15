package org.yuezhikong.Server.UserData;

import org.jetbrains.annotations.Nullable;
import org.yuezhikong.Server.IServer;
import org.yuezhikong.Server.UserData.Authentication.IUserAuthentication;

public class ConsoleUser implements user {

    public ConsoleUser() {
        try {
            Class.forName(new Throwable().getStackTrace()[1].getClassName()).asSubclass(IServer.class);
        } catch (ClassCastException | ClassNotFoundException e)
        {
            throw new UnsupportedOperationException("only Server can create Console User!");
        }
    }

    @Override
    public String getUserName() {
        return "Server";
    }

    @Override
    public user UserLogin(String UserName) {
        throw new UnsupportedOperationException("Server can not login");
    }

    @Override
    public boolean isUserLogged() {
        return true;
    }

    @Override
    public user UserDisconnect() {
        throw new UnsupportedOperationException("Server can not disconnect");
    }

    @Override
    public user SetUserPermission(int permissionLevel, boolean FlashPermission) {
        throw new UnsupportedOperationException("Server can not set permission");
    }

    @Override
    public user SetUserPermission(Permission permission) {
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
}
