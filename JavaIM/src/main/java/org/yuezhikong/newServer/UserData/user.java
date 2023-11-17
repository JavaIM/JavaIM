/*
 * Simplified Chinese (简体中文)
 *
 * 版权所有 (C) 2023 QiLechan <qilechan@outlook.com> 和本程序的贡献者
 *
 * 本程序是自由软件：你可以再分发之和/或依照由自由软件基金会发布的 GNU 通用公共许可证修改之，无论是版本 3 许可证，还是 3 任何以后版都可以。
 * 发布该程序是希望它能有用，但是并无保障;甚至连可销售和符合某个特定的目的都不保证。请参看 GNU 通用公共许可证，了解详情。
 * 你应该随程序获得一份 GNU 通用公共许可证的副本。如果没有，请看 <https://www.gnu.org/licenses/>。
 * English (英语)
 *
 * Copyright (C) 2023 QiLechan <qilechan@outlook.com> and contributors to this program
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or 3 any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program. If not, see <https://www.gnu.org/licenses/>.
 */
package org.yuezhikong.newServer.UserData;

import cn.hutool.crypto.symmetric.AES;
import org.jetbrains.annotations.Nullable;
import org.yuezhikong.NetworkManager;
import org.yuezhikong.newServer.ServerMain;
import org.yuezhikong.newServer.UserData.Authentication.IUserAuthentication;

@SuppressWarnings("unused")
public interface user {
    /**
     * 设置接收消息线程
     * @param thread 线程
     */
    void setRecvMessageThread(ServerMain.RecvMessageThread thread);

    /**
     * 返回用于接收消息的线程
     * @return 线程
     */
    ServerMain.RecvMessageThread getRecvMessageThread();

    /**
     * 返回用户的网络数据
     * @return 用户网络数据
     */
    NetworkManager.NetworkData getUserNetworkData();

    /**
     * 设置用户公钥
     * @param publicKey 用户公钥
     */
    void setPublicKey(String publicKey);

    /**
     * 获取用户公钥
     * @return 用户公钥
     */
    String getPublicKey();

    /**
     * 设置用户的AES加密器
     * @param userAES AES加密器
     */
    void setUserAES(AES userAES);

    /**
     * 返回用户的AES加密器
     * @return AES加密器
     */
    AES getUserAES();

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
    void UserLogin(String UserName);

    /**
     * 获取用户登录状态
     * @return {@code true} 已登录, {@code false} 未登录
     */
    boolean isUserLogined();

    /**
     * 使用户离线（踢出用户）
     */
    void UserDisconnect();

    /**
     * 设置用户权限级别
     * @param permissionLevel 权限级别
     * @param FlashPermission 是否为刷新权限
     * @apiNote 如果是刷新权限，权限信息将不会记录到数据库中
     */
    void SetUserPermission(int permissionLevel, boolean FlashPermission);

    /**
     * 设置用户权限级别
     * @param permission 权限
     * @apiNote 通过此方法，将不会记录到数据库中
     */
    void SetUserPermission(Permission permission);

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
     * 设置禁言时间
     * @param muteTime 禁言时间
     * @apiNote 暂时未使用
     */
    void setMuteTime(long muteTime);

    /**
     * 设置是否被禁言
     * @param Muted 是否被禁言
     * @apiNote 暂时未使用
     */
    void setMuted(boolean Muted);

    /**
     * 获取禁言时间
     * @apiNote 暂时未使用
     */
    long getMuteTime();

    /**
     * 获取是否被禁言
     * @apiNote 暂时未使用
     */
    boolean getMuted();

    /**
     * 是否允许TransferProtocol
     * @return {@code true} 允许 {@code false} 不允许
     */
    boolean isAllowedTransferProtocol();

    /**
     * 设置允许TransferProtocol
     * @param allowedTransferProtocol {@code true} 允许 {@code false} 不允许
     */
    void setAllowedTransferProtocol(boolean allowedTransferProtocol);

    /**
     * 添加用户登录回调函数
     * @param code 函数
     * @apiNote 如果已经登录了，则使用当前线程直接调用回调函数
     */
    void addLoginRecall(IUserAuthentication.UserRecall code);

    /**
     * 添加用户离线回调函数
     * @param code 函数
     * @apiNote 如果已经登录了，则使用当前线程直接调用回调函数
     */
    void addDisconnectRecall(IUserAuthentication.UserRecall code);

    /**
     * 设置用户Authentication实例
     * @param Authentication 实例
     */
    void setUserAuthentication(@Nullable IUserAuthentication Authentication);

    /**
     * 获取用户Authentication实例
     * @return Authentication实例
     */
    @Nullable
    IUserAuthentication getUserAuthentication();
}
