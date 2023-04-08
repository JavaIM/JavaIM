package org.yuezhikong.Server.api;

import com.google.gson.Gson;
import org.apache.logging.log4j.Level;
import org.yuezhikong.CodeDynamicConfig;
import org.yuezhikong.Server.Server;
import org.yuezhikong.Server.UserData.user;
import org.yuezhikong.utils.ProtocolData;
import org.yuezhikong.utils.RSA;
import org.yuezhikong.utils.SaveStackTrace;

import javax.security.auth.login.AccountNotFoundException;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

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
     * 将消息转换为Protocol
     * @param Message 原始信息
     * @param Version 协议版本
     * @return 转换为Protocol的Json格式
     */
    private static String ProtocolRequest(String Message,int Version)
    {
        // 将消息根据Protocol封装
        Gson gson = new Gson();
        ProtocolData protocolData = new ProtocolData();
        ProtocolData.MessageHeadBean MessageHead = new ProtocolData.MessageHeadBean();
        MessageHead.setVersion(Version);
        MessageHead.setType(1);
        protocolData.setMessageHead(MessageHead);
        ProtocolData.MessageBodyBean MessageBody = new ProtocolData.MessageBodyBean();
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
        String Message = inputMessage;
        Message = ProtocolRequest(Message, CodeDynamicConfig.getProtocolVersion());
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
        } catch (Exception e)
        {
            SaveStackTrace.saveStackTrace(e);
        }
    }
    /**
     * 向所有客户端发信
     * @param inputMessage 要发信的信息
     * @param ServerInstance 服务器实例
     */
    static void SendMessageToAllClient(String inputMessage, Server ServerInstance)
    {
        String Message = inputMessage;
        String MessageJson = ProtocolRequest(Message,CodeDynamicConfig.getProtocolVersion());
        int i = 0;
        int tmpclientidall = ServerInstance.getClientIDAll();
        tmpclientidall = tmpclientidall - 1;
        try {
            while (true) {
                Message = MessageJson;
                if (i > tmpclientidall) {
                    break;
                }
                Socket sendsocket = ServerInstance.getUsers().get(i).GetUserSocket();
                if (sendsocket == null) {
                    i = i + 1;
                    continue;
                }
                if (!ServerInstance.getUsers().get(i).GetUserLogined())
                {
                    i = i + 1;
                    continue;
                }
                if (GetRSA_Mode()) {
                    String UserPublicKey = ServerInstance.getUsers().get(i).GetUserPublicKey();
                    if (UserPublicKey == null) {
                        i = i + 1;
                        continue;
                    }
                    Message = java.net.URLEncoder.encode(Message, StandardCharsets.UTF_8);
                    if (isAES_Mode())
                    {
                        Message = ServerInstance.getUsers().get(i).GetUserAES().encryptBase64(Message);
                    }
                    else {
                        Message = RSA.encrypt(Message, UserPublicKey);
                    }
                }
                BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(sendsocket.getOutputStream()));
                try {
                    writer.write(Message);
                    writer.newLine();
                    writer.flush();
                }
                catch (IOException e)
                {
                    if ("Broken pipe".equals(e.getMessage()))
                    {
                        ServerInstance.getUsers().get(i).UserDisconnect();
                        i = i + 1;
                        continue;
                    }
                }
                if (i == tmpclientidall) {
                    break;
                }
                i = i + 1;
            }
        } catch (IOException e) {
            Server.logger.log(Level.ERROR, "SendMessage时出现IOException!");
            org.yuezhikong.utils.SaveStackTrace.saveStackTrace(e);
        } catch (Exception e) {
            org.yuezhikong.utils.SaveStackTrace.saveStackTrace(e);
        }
    }

    /**
     * 获取用户User Data Class
     * @param UserName 用户名
     * @param ServerInstance 服务器实例
     * @return 用户User Data Class
     * @exception AccountNotFoundException 无法根据指定的用户名找到用户时抛出此异常
     */
    static user GetUserByUserName(String UserName, Server ServerInstance) throws AccountNotFoundException {
        int i = 0;
        int tmpclientidall = ServerInstance.getClientIDAll();
        tmpclientidall = tmpclientidall - 1;
        while (true) {
            if (i > tmpclientidall) {
                throw new AccountNotFoundException("This UserName Is Not Found,if this UserName No Login?");//找不到用户时抛出异常
            }
            user RequestUser = Server.GetInstance().getUsers().get(i);
            if (RequestUser.GetUserSocket() == null) {
                i = i + 1;
                continue;
            }
            if (RequestUser.GetUserName().equals(UserName)) {
                return RequestUser;
            }
            if (i == tmpclientidall) {
                throw new AccountNotFoundException("This UserName Is Not Found,if this UserName No Login?");//找不到用户时抛出异常
            }
            i = i + 1;
        }
    }
}
