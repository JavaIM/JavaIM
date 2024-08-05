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
package org.yuezhikong.Server.api;

import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.yuezhikong.Server.userData.user;

import javax.security.auth.login.AccountNotFoundException;
import java.util.List;

public interface api {

    /**
     * 为指定用户发送消息
     *
     * @param user         发信的目标用户
     * @param inputMessage 发信的信息
     */
    void sendMessageToUser(@NotNull user user, @NotNull @Nls String inputMessage);

    /**
     * 新的向所有客户端发信api
     *
     * @param inputMessage 要发信的信息
     */
    void sendMessageToAllClient(@NotNull @Nls String inputMessage);

    /**
     * 获取有效的客户端列表
     *
     * @param CheckLoginStatus 是否检查登录状态
     * @return 有效的客户端列表
     * @apiNote 用户列表更新后，您获取到的list不会被更新！请勿长时间保存此数据，长时间保存将变成过期数据
     */
    @NotNull
    List<user> getValidUserList(boolean CheckLoginStatus);

    /**
     * 新的获取用户User Data Class api
     *
     * @param UserName 用户名
     * @return 用户User Data Class
     * @throws AccountNotFoundException 无法根据指定的用户名找到用户时抛出此异常
     */
    @NotNull
    user getUserByUserName(@NotNull @Nls String UserName) throws AccountNotFoundException;

    /**
     * 修改用户的密码
     *
     * @param User     用户
     * @param password 密码
     */
    void changeUserPassword(user User, String password);

    /**
     * 直接发送信息到一个用户的函数
     *
     * @param User         目标用户
     * @param InputData    信息
     * @param ProtocolType 协议类型
     */
    void sendJsonToClient(@NotNull user User, @NotNull String InputData, @NotNull String ProtocolType);

    /**
     * 根据用户Id获取用户
     *
     * @param userId 用户Id
     * @return 用户
     * @throws AccountNotFoundException 根据Id找不到用户
     */
    @NotNull
    user getUserByUserId(String userId) throws AccountNotFoundException;
}
