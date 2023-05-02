package org.yuezhikong.Server.LoginSystem;


import com.google.gson.Gson;
import org.yuezhikong.CodeDynamicConfig;
import org.yuezhikong.Server.Server;
import org.yuezhikong.Server.UserData.user;
import org.yuezhikong.Server.api.ServerAPI;
import org.yuezhikong.utils.CustomExceptions.UserAlreadyLoggedInException;
import org.yuezhikong.utils.CustomVar;
import org.yuezhikong.utils.ProtocolData;
import org.yuezhikong.utils.RSA;

import javax.security.auth.login.AccountNotFoundException;
import javax.security.auth.login.FailedLoginException;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

import static org.yuezhikong.CodeDynamicConfig.isAES_Mode;
import static org.yuezhikong.Server.api.ServerAPI.SendMessageToUser;
import static org.yuezhikong.CodeDynamicConfig.GetRSA_Mode;

public class UserLogin{
    /**
     * 是否允许用户登录
     * @param LoginUser 请求登录的用户
     * @return 是/否允许
     * @throws UserAlreadyLoggedInException 用户已经登录了
     * @throws NullPointerException 用户的某些信息读取出NULL
     * @throws FailedLoginException 由于用户的原因导致登录失败
     * @apiNote 虽然在执行的期间，就会写入到user.class中，但也请您根据返回值做是否踢出登录等的处理
     */
    public static boolean WhetherTheUserIsAllowedToLogin(user LoginUser) throws UserAlreadyLoggedInException, NullPointerException, FailedLoginException {
        if (LoginUser.GetUserLogined())
        {
            throw new UserAlreadyLoggedInException("This User Is Logined!");
        }
        else
        {

            try {
                SendMessageToUser(LoginUser,"在进入之前，您必须先登录/注册");
                Thread.sleep(250);
                SendMessageToUser(LoginUser,"输入1进行登录");
                Thread.sleep(250);
                SendMessageToUser(LoginUser,"输入2进行注册");
                String UserSelect;
                BufferedReader reader = new BufferedReader(new InputStreamReader(LoginUser.GetUserSocket().getInputStream()));//获取输入流
                UserSelect = reader.readLine();
                if (UserSelect == null)
                {
                    throw new NullPointerException();
                }
                if (GetRSA_Mode()) {
                    if (isAES_Mode())
                    {
                        UserSelect = LoginUser.GetUserAES().decryptStr(UserSelect);
                    }
                    else
                        UserSelect = RSA.decrypt(UserSelect,Objects.requireNonNull(RSA.loadPrivateKeyFromFile("Private.key")).PrivateKey);
                }
                UserSelect = java.net.URLDecoder.decode(UserSelect, StandardCharsets.UTF_8);
                // 将信息从Protocol Json中取出
                Gson gson = new Gson();
                ProtocolData protocolData = gson.fromJson(UserSelect,ProtocolData.class);
                if (protocolData.getMessageHead().getVersion() != CodeDynamicConfig.getProtocolVersion())
                {
                    LoginUser.UserDisconnect();
                }
                // type目前只实现了chat,FileTransfer延后
                if (protocolData.getMessageHead().getType() != 1)
                {
                    ServerAPI.SendMessageToUser(LoginUser,"此服务器暂不支持FileTransfer协议");
                }
                UserSelect = protocolData.getMessageBody().getMessage();
                int Select = Integer.parseInt(UserSelect);
                SendMessageToUser(LoginUser,"请输入您的用户名");
                String UserName;
                reader = new BufferedReader(new InputStreamReader(LoginUser.GetUserSocket().getInputStream()));//获取输入流
                UserName = reader.readLine();
                if (UserName == null)
                {
                    throw new NullPointerException();
                }
                if (GetRSA_Mode()) {
                    if (isAES_Mode())
                    {
                        UserName = LoginUser.GetUserAES().decryptStr(UserName);
                    }
                    else
                        UserName = RSA.decrypt(UserName,Objects.requireNonNull(RSA.loadPrivateKeyFromFile("Private.key")).PrivateKey);
                }
                UserName = java.net.URLDecoder.decode(UserName, StandardCharsets.UTF_8);
                // 将信息从Protocol Json中取出
                gson = new Gson();
                protocolData = gson.fromJson(UserName,ProtocolData.class);
                if (protocolData.getMessageHead().getVersion() != CodeDynamicConfig.getProtocolVersion())
                {
                    LoginUser.UserDisconnect();
                }
                UserName = protocolData.getMessageBody().getMessage();
                // type目前只实现了chat,FileTransfer延后
                if (protocolData.getMessageHead().getType() != 1)
                {
                    ServerAPI.SendMessageToUser(LoginUser,"此服务器暂不支持FileTransfer协议");
                }
                //用户名暴力格式化
                CustomVar.Command username = ServerAPI.CommandFormat(UserName);
                StringBuilder builder = new StringBuilder();
                builder.append(username.Command());
                for (String string : username.argv())
                {
                    builder.append(string);
                }
                UserName = builder.toString();

                SendMessageToUser(LoginUser,"请输入您的密码");
                String Password;
                reader = new BufferedReader(new InputStreamReader(LoginUser.GetUserSocket().getInputStream()));//获取输入流
                Password = reader.readLine();
                if (Password == null)
                {
                    throw new NullPointerException();
                }
                if (GetRSA_Mode()) {
                    if (isAES_Mode())
                    {
                        Password = LoginUser.GetUserAES().decryptStr(Password);
                    }
                    else {
                        Password = RSA.decrypt(Password, Objects.requireNonNull(RSA.loadPrivateKeyFromFile("Private.key")).PrivateKey);
                    }
                }
                Password = java.net.URLDecoder.decode(Password, StandardCharsets.UTF_8);
                // 将信息从Protocol Json中取出
                gson = new Gson();
                protocolData = gson.fromJson(Password,ProtocolData.class);
                if (protocolData.getMessageHead().getVersion() != CodeDynamicConfig.getProtocolVersion())
                {
                    LoginUser.UserDisconnect();
                }
                // type目前只实现了chat,FileTransfer延后
                if (protocolData.getMessageHead().getType() != 1)
                {
                    ServerAPI.SendMessageToUser(LoginUser,"此服务器暂不支持FileTransfer协议");
                }
                Password = protocolData.getMessageBody().getMessage();
                //上方为请求用户输入用户名、密码
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
                //上方为处理此用户是否已登录
                if (Select == 1)//登录
                {
                    //下方为SQL处理
                    UserLoginRequestThread loginRequestThread = new UserLoginRequestThread(LoginUser,UserName,Password);
                    loginRequestThread.start();
                    loginRequestThread.setName("UserLoginRequest");
                    loginRequestThread.join();
                    if (!loginRequestThread.GetReturn())
                    {
                        SendMessageToUser(LoginUser,"抱歉，您的本次登录被拒绝");
                    }
                    else
                    {
                        SendMessageToUser(LoginUser,"登录成功！");
                    }
                    return loginRequestThread.GetReturn();
                }
                else if (Select == 2)//注册
                {
                    //下方为SQL处理
                    UserRegisterRequestThread RegisterRequestThread = new UserRegisterRequestThread(LoginUser,UserName,Password);
                    RegisterRequestThread.start();
                    RegisterRequestThread.setName("UserRegisterThread");
                    RegisterRequestThread.join();
                    if (!RegisterRequestThread.GetReturn())
                    {
                        SendMessageToUser(LoginUser,"抱歉，您的本次注册被拒绝");
                    }
                    else
                    {
                        SendMessageToUser(LoginUser,"注册成功！");
                    }
                    return RegisterRequestThread.GetReturn();
                }
                else
                {
                    SendMessageToUser(LoginUser,"非法输入！这里只允许输入1/2");
                    SendMessageToUser(LoginUser,"由于您不遵守提示，系统将终止您的会话！");
                    return false;
                }
            }
            catch (IOException e)
            {
                org.yuezhikong.utils.SaveStackTrace.saveStackTrace(e);
            }
            catch (NumberFormatException e)
            {
                SendMessageToUser(LoginUser,"非法输入！这里只允许输入1/2");
                SendMessageToUser(LoginUser,"由于您不遵守提示，系统将终止您的会话！");
            } catch (InterruptedException e) {
                SendMessageToUser(LoginUser,"出现内部异常，无法完成此操作");
                return false;
            }
            return false;
        }
    }
}
