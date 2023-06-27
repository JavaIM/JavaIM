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
package org.yuezhikong.Server.LoginSystem;


import org.jetbrains.annotations.NotNull;
import org.yuezhikong.Server.Server;
import org.yuezhikong.Server.UserData.user;
import org.yuezhikong.Server.api.ServerAPI;
import org.yuezhikong.utils.CustomExceptions.UserAlreadyLoggedInException;
import org.yuezhikong.utils.CustomVar;
import org.yuezhikong.utils.Logger;

import javax.security.auth.login.AccountNotFoundException;

import static org.yuezhikong.Server.api.ServerAPI.SendMessageToUser;

public class UserLogin{
    /**
     * 是否允许用户登录
     * @param LoginUser 请求登录的用户
     * @param logger Logger
     * @return 是/否允许
     * @throws UserAlreadyLoggedInException 用户已经登录了
     * @throws NullPointerException 用户的某些信息读取出NULL
     * @apiNote 虽然在执行的期间，就会写入到user.class中，但也请您根据返回值做是否踢出登录等的处理
     */
    public static boolean WhetherTheUserIsAllowedToLogin(@NotNull user LoginUser, Logger logger,String username,String Passwd) throws UserAlreadyLoggedInException, NullPointerException {
        if (LoginUser.GetUserLogined())
        {
            throw new UserAlreadyLoggedInException("This User Is Logined!");
        }
        else
        {
            try {
                //用户名暴力格式化，防止用奇奇怪怪的名字绕过命令选择
                CustomVar.Command userName = ServerAPI.CommandFormat(username);
                StringBuilder builder = new StringBuilder();
                builder.append(userName.Command());
                for (String string : userName.argv())
                {
                    builder.append(string);
                }
                String UserName = builder.toString();
                boolean ThisUserNameIsNotLogin = false;
                try {
                    ServerAPI.GetUserByUserName(UserName, Server.GetInstance(),false);
                } catch (AccountNotFoundException e)
                {
                    ThisUserNameIsNotLogin = true;
                }
                if (!ThisUserNameIsNotLogin)
                {
                    throw new UserAlreadyLoggedInException("This User Is Logined!");
                }
                //处理登录请求
                LoginORRegisterRequestThread requestThread = new LoginORRegisterRequestThread(UserName,Passwd,LoginUser);
                requestThread.start();
                requestThread.join();
                return requestThread.isSuccess();
            }
            catch (InterruptedException e) {
                SendMessageToUser(LoginUser,"出现内部异常，无法完成此操作");
                return false;
            }
        }
    }
}
