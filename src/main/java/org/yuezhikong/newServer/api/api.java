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
package org.yuezhikong.newServer.api;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.yuezhikong.newServer.ServerMain;
import org.yuezhikong.newServer.UserData.user;
import org.yuezhikong.utils.CustomVar;

import javax.security.auth.login.AccountNotFoundException;
import java.util.List;

public interface api {
    /**
     * 将命令进行格式化
     * @param Command 原始命令信息
     * @return 命令和参数
     */
    @Contract("_ -> new")
    @NotNull
    CustomVar.Command CommandFormat(@NotNull @Nls String Command);
    /**
     * 将聊天消息转换为聊天协议格式
     * @param Message 原始信息
     * @param Version 协议版本
     * @return 转换为的聊天协议格式
     */
    @NotNull String ChatProtocolRequest(@NotNull @Nls String Message, int Version);
    /**
     * 为指定用户发送消息
     * @param user 发信的目标用户
     * @param inputMessage 发信的信息
     */
    void SendMessageToUser(@NotNull user user, @NotNull @Nls String inputMessage);
    /**
     * 新的向所有客户端发信api
     * @param inputMessage 要发信的信息
     * @param ServerInstance 服务器实例
     */
    void SendMessageToAllClient(@NotNull @Nls String inputMessage,@NotNull ServerMain ServerInstance);
    /**
     * 获取有效的客户端列表
     * @param ServerInstance 服务端实例
     * @param DetectLoginStatus 是否检测已登录
     * @apiNote 用户列表更新后，您获取到的list不会被更新！请勿长时间保存此数据，长时间保存将变成过期数据
     * @return 有效的客户端列表
     */
    @NotNull List<user> GetValidClientList(@NotNull ServerMain ServerInstance, boolean DetectLoginStatus);
    /**
     * 新的获取用户User Data Class api
     * @param UserName 用户名
     * @param ServerInstance 服务器实例
     * @param DetectLoginStatus 是否检测已登录
     * @return 用户User Data Class
     * @exception AccountNotFoundException 无法根据指定的用户名找到用户时抛出此异常
     */
    @NotNull user GetUserByUserName(@NotNull @Nls String UserName, @NotNull ServerMain ServerInstance, boolean DetectLoginStatus) throws AccountNotFoundException;

    /**
     * 修改用户的密码
     * @param User 用户
     * @param password 密码
     */
    void ChangeUserPassword(user User, String password);
}
