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
