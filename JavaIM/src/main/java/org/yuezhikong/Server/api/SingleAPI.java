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

import com.google.gson.Gson;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.yuezhikong.Server.IServer;
import org.yuezhikong.Server.UserData.user;
import org.yuezhikong.Server.UserData.userInformation;
import org.yuezhikong.utils.Protocol.SystemProtocol;
import org.yuezhikong.utils.SHA256;

import javax.security.auth.login.AccountNotFoundException;
import java.util.ArrayList;
import java.util.List;

@Slf4j
public abstract class SingleAPI implements api {
    private final IServer ServerInstance;

    /**
     * 初始化服务端API
     *
     * @param serverInstance 服务端实例
     */
    public SingleAPI(IServer serverInstance) {
        ServerInstance = serverInstance;
    }

    /**
     * 为指定用户发送消息
     *
     * @param user         发信的目标用户
     * @param inputMessage 发信的信息
     */
    @Override
    public void SendMessageToUser(@NotNull user user, @NotNull @Nls String inputMessage) {
        if (user.isServer()) {
            log.info(inputMessage);
            return;
        }
        String[] inputs = inputMessage.replaceAll("\r", "").split("\n");
        for (String input : inputs) {
            Gson gson = new Gson();
            SystemProtocol protocolData = new SystemProtocol();
            protocolData.setType("DisplayMessage");
            protocolData.setMessage(input);
            String Message = gson.toJson(protocolData);
            SendJsonToClient(user, Message, "SystemProtocol");
        }
    }

    /**
     * 新的向所有客户端发信api
     *
     * @param inputMessage 要发信的信息
     */
    @Override
    public void SendMessageToAllClient(@NotNull @Nls String inputMessage) {
        List<user> ValidClientList = GetValidClientList(true);
        String[] inputs = inputMessage.replaceAll("\r", "").split("\n");
        for (String input : inputs) {
            for (user User : ValidClientList) {
                SendMessageToUser(User, input);
            }
        }
    }

    /**
     * 获取有效的客户端列表
     *
     * @return 有效的客户端列表
     * @apiNote 用户列表更新后，您获取到的list不会被更新！请勿长时间保存此数据，长时间保存将变成过期数据
     */
    @Override
    public @NotNull List<user> GetValidClientList(boolean CheckLoginStatus) {
        List<user> AllClientList = ServerInstance.getUsers();
        List<user> ValidClientList = new ArrayList<>();
        for (user User : AllClientList) {
            if (User == null)
                continue;
            if (CheckLoginStatus && !User.isUserLogged())
                continue;
            if (User.isServer())
                continue;
            ValidClientList.add(User);
        }
        return ValidClientList;
    }

    /**
     * 新的获取用户User Data Class api
     *
     * @param UserName 用户名
     * @return 用户User Data Class
     * @throws AccountNotFoundException 无法根据指定的用户名找到用户时抛出此异常
     */
    @Override
    public @NotNull user GetUserByUserName(@NotNull @Nls String UserName) throws AccountNotFoundException {
        List<user> ValidClientList = GetValidClientList(true);
        for (user User : ValidClientList) {
            if (User.getUserName().equals(UserName)) {
                return User;
            }
        }
        throw new AccountNotFoundException("This UserName Is Not Found,if this UserName No Login?");//找不到用户时抛出异常
    }

    @Override
    public void ChangeUserPassword(user User, String password) {
        userInformation information = User.getUserInformation();
        information.setPasswd(SHA256.sha256(password + information.getSalt()));
        User.setUserInformation(information);
    }

    @Override
    public @NotNull user GetUserByUserId(String userId) throws AccountNotFoundException {
        for (user User : GetValidClientList(true)) {
            if (User.getUserInformation().getUserId().equals(userId)) {
                return User;
            }
        }
        throw new AccountNotFoundException("This userId not Found");
    }
}
