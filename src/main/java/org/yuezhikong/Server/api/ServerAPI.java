package org.yuezhikong.Server.api;

import com.google.gson.Gson;
import org.yuezhikong.CodeDynamicConfig;
import org.yuezhikong.Server.Server;
import org.yuezhikong.Server.UserData.user;
import org.yuezhikong.utils.CustomExceptions.ModeDisabledException;
import org.yuezhikong.utils.CustomVar;
import org.yuezhikong.utils.ProtocolData;
import org.yuezhikong.utils.RSA;
import org.yuezhikong.utils.SaveStackTrace;

import javax.security.auth.login.AccountNotFoundException;
import java.io.BufferedWriter;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import static org.yuezhikong.CodeDynamicConfig.GetRSA_Mode;
import static org.yuezhikong.CodeDynamicConfig.isAES_Mode;

/**
 * 服务端API集合
 * 建议插件仅调用本API或UserData包下的user class
 * 其他class不要直接调用！更不要用反射骗自己！
 * @author AlexLiuDev233
 * @version v0.1
 * @Description 2023年2月25日从org.yuezhikong.Server.utils.utils迁来
 * @Date 2023年2月25日
 */
public interface ServerAPI {
    /**
     * 将命令进行格式化
     * @param Command 原始命令信息
     * @return 命令和参数
     */
    static CustomVar.Command CommandFormat(String Command)
    {
        String command;
        String[] argv;
        {
            String[] CommandLineFormated = Command.split("\\s+"); //分割一个或者多个空格
            command = CommandLineFormated[0];
            argv = new String[CommandLineFormated.length - 1];
            int j = 0;//要删除的字符索引
            int i = 0;
            int k = 0;
            while (i < CommandLineFormated.length) {
                if (i != j) {
                    argv[k] = CommandLineFormated[i];
                    k++;
                }
                i++;
            }
        }
        return new CustomVar.Command(command,argv);
    }
    /**
     * 将聊天消息转换为聊天协议格式
     * @param Message 原始信息
     * @param Version 协议版本
     * @return 转换为的聊天协议格式
     */
    static String ChatProtocolRequest(String Message, int Version)
    {
        // 将消息根据聊天协议封装
        Gson gson = new Gson();
        ProtocolData protocolData = new ProtocolData();
        ProtocolData.MessageHead MessageHead = new ProtocolData.MessageHead();
        MessageHead.setVersion(Version);
        MessageHead.setType("Chat");
        protocolData.setMessageHead(MessageHead);
        ProtocolData.MessageBody MessageBody = new ProtocolData.MessageBody();
        MessageBody.setFileLong(0);
        MessageBody.setMessage(Message);
        protocolData.setMessageBody(MessageBody);
        return gson.toJson(protocolData);
    }
    /**
     * 为指定用户发送消息
     * @param user 发信的目标用户
     * @param inputMessage 发信的信息
     */
    static void SendMessageToUser(user user, String inputMessage)
    {
        try {
            Thread.sleep(250);
        } catch (InterruptedException e) {
            SaveStackTrace.saveStackTrace(e);
        }
        if (user.isServer())
        {
            Server.GetInstance().logger.info(inputMessage);
            return;
        }
        String Message = inputMessage;
        Message = ChatProtocolRequest(Message, CodeDynamicConfig.getProtocolVersion());
        try {
            if (GetRSA_Mode()) {
                String UserPublicKey = user.GetUserPublicKey();
                if (UserPublicKey == null) {
                    throw new NullPointerException();
                }
                Message = java.net.URLEncoder.encode(Message, StandardCharsets.UTF_8);
                if (isAES_Mode())
                {
                    Message = user.GetUserAES().encryptBase64(Message);
                }
                else {
                    Message = RSA.encrypt(Message, UserPublicKey);
                }
            }
            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(user.GetUserSocket().getOutputStream()));
            writer.write(Message);
            writer.newLine();
            writer.flush();
            writer.close();
        } catch (Exception e)
        {
            SaveStackTrace.saveStackTrace(e);
        }
    }

    /**
     * 新的向所有客户端发信api
     * @param inputMessage 要发信的信息
     * @param ServerInstance 服务器实例
     */
    static void SendMessageToAllClient(String inputMessage,Server ServerInstance)
    {
        List<user> ValidClientList = GetValidClientList(ServerInstance,true);
        for (user User : ValidClientList)
        {
            SendMessageToUser(User,inputMessage);
        }
    }
    /**
     * 获取有效的客户端列表
     * @param ServerInstance 服务端实例
     * @param DetectLoginStatus 是否检测已登录
     * @apiNote 用户列表更新后，您获取到的list不会被更新！请勿长时间保存此数据，长时间保存将变成过期数据
     * @return 有效的客户端列表
     */
    static List<user> GetValidClientList(Server ServerInstance,boolean DetectLoginStatus)
    {
        List<user> AllClientList = ServerInstance.getUsers();
        List<user> ValidClientList = new ArrayList<>();
        for (user User : AllClientList)
        {
            if (User == null)
                continue;
            if (DetectLoginStatus) {
                if (!User.GetUserLogined())
                    continue;
            }
            if (User.GetUserSocket() == null)
                continue;
            if (CodeDynamicConfig.GetRSA_Mode())
            {
                try {
                    if (User.GetUserPublicKey() == null)
                        continue;
                    if (CodeDynamicConfig.isAES_Mode()) {
                        if (User.GetUserAES() == null)
                            continue;
                    }
                } catch (ModeDisabledException e) {
                    SaveStackTrace.saveStackTrace(e);
                }
            }
            ValidClientList.add(User);
        }
        return ValidClientList;
    }

    /**
     * 新的获取用户User Data Class api
     * @param UserName 用户名
     * @param ServerInstance 服务器实例
     * @param DetectLoginStatus 是否检测已登录
     * @return 用户User Data Class
     * @exception AccountNotFoundException 无法根据指定的用户名找到用户时抛出此异常
     */
    static user GetUserByUserName(String UserName, Server ServerInstance,boolean DetectLoginStatus) throws AccountNotFoundException {
        List<user> ValidClientList = GetValidClientList(ServerInstance,DetectLoginStatus);
        for (user User : ValidClientList)
        {
            if (User.GetUserName().equals(UserName)) {
                return User;
            }
        }
        throw new AccountNotFoundException("This UserName Is Not Found,if this UserName No Login?");//找不到用户时抛出异常
    }
}
