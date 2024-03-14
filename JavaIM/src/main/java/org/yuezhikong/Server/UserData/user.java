package org.yuezhikong.Server.UserData;

import org.jetbrains.annotations.Nullable;
import org.yuezhikong.Server.UserData.Authentication.IUserAuthentication;

public interface user {

    /**
     * 获取客户端ID
     * @return 客户端ID
     * @apiNote 客户端ID每次重新连入均会更改，建议使用用户名
     */
    int getClientID();

    /**
     * 获取用户名
     * @return 用户名
     */
    String getUserName();

    /**
     * 使用户登录
     * @param UserName 用户名
     * @apiNote 请注意，此方法将会触发UserLoginEvent
     */
    user UserLogin(String UserName);

    /**
     * 获取用户登录状态
     * @return {@code true} 已登录, {@code false} 未登录
     */
    boolean isUserLogined();

    /**
     * 使用户离线（踢出用户）
     */
    user UserDisconnect();

    /**
     * 设置用户权限级别
     * @param permissionLevel 权限级别
     * @param FlashPermission 是否为刷新权限
     * @apiNote 如果是刷新权限，权限信息将不会记录到数据库中
     */
    user SetUserPermission(int permissionLevel, boolean FlashPermission);

    /**
     * 设置用户权限级别
     * @param permission 权限
     * @apiNote 通过此方法，将不会记录到数据库中
     */
    user SetUserPermission(Permission permission);

    /**
     * 获取用户的权限级别
     * @return 权限级别
     */
    Permission getUserPermission();

    /**
     * 获取此用户是/否是服务端虚拟用户
     * @return {@code true} 是服务端虚拟账户 {@code false} 不是服务端虚拟账户
     */
    boolean isServer();

    /**
     * 是否允许TransferProtocol
     * @return {@code true} 允许 {@code false} 不允许
     */
    boolean isAllowedTransferProtocol();

    /**
     * 设置允许TransferProtocol
     * @param allowedTransferProtocol {@code true} 允许 {@code false} 不允许
     */
    user setAllowedTransferProtocol(boolean allowedTransferProtocol);

    /**
     * 添加用户登录回调函数
     * @param code 函数
     * @apiNote 如果已经登录了，则使用当前线程直接调用回调函数
     */
    user addLoginRecall(IUserAuthentication.UserRecall code);

    /**
     * 添加用户离线回调函数
     * @param code 函数
     * @apiNote 如果已经登录了，则使用当前线程直接调用回调函数
     */
    user addDisconnectRecall(IUserAuthentication.UserRecall code);

    /**
     * 设置用户Authentication实例
     * @param Authentication 实例
     */
    user setUserAuthentication(@Nullable IUserAuthentication Authentication);

    /**
     * 获取用户Authentication实例
     * @return Authentication实例
     */
    @Nullable
    IUserAuthentication getUserAuthentication();
}
