package org.yuezhikong.newServer;

import com.google.gson.Gson;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.yuezhikong.CodeDynamicConfig;
import org.yuezhikong.newServer.UserData.user;
import org.yuezhikong.utils.CustomVar;
import org.yuezhikong.utils.Protocol.NormalProtocol;
import org.yuezhikong.utils.SaveStackTrace;

import javax.security.auth.login.AccountNotFoundException;
import java.io.BufferedWriter;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class api {
    /**
     * 将命令进行格式化
     * @param Command 原始命令信息
     * @return 命令和参数
     */
    @Contract("_ -> new")
    @NotNull
    public static CustomVar.Command CommandFormat(@NotNull @Nls String Command)
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
    public static @NotNull String ChatProtocolRequest(@NotNull @Nls String Message, int Version)
    {
        // 将消息根据聊天协议封装
        Gson gson = new Gson();
        NormalProtocol protocolData = new NormalProtocol();
        NormalProtocol.MessageHead MessageHead = new NormalProtocol.MessageHead();
        MessageHead.setVersion(Version);
        MessageHead.setType("Chat");
        protocolData.setMessageHead(MessageHead);
        NormalProtocol.MessageBody MessageBody = new NormalProtocol.MessageBody();
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
    public static void SendMessageToUser(@NotNull user user, @NotNull @Nls String inputMessage)
    {
        if (user.isServer())
        {
            ServerMain.getServer().getLogger().ChatMsg(inputMessage);
            return;
        }
        try {
            Thread.sleep(250);
        } catch (InterruptedException e) {
            SaveStackTrace.saveStackTrace(e);
        }
        String Message = inputMessage;
        Message = ChatProtocolRequest(Message, CodeDynamicConfig.getProtocolVersion());
        Message = user.getUserAES().encryptBase64(Message);
        try {
            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(user.getUserSocket().getOutputStream(), StandardCharsets.UTF_8));
            writer.write(Message);
            writer.newLine();
            writer.flush();
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
    public static void SendMessageToAllClient(@NotNull @Nls String inputMessage,@NotNull ServerMain ServerInstance)
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
    public static @NotNull List<user> GetValidClientList(@NotNull ServerMain ServerInstance, boolean DetectLoginStatus)
    {
        List<user> AllClientList = ServerInstance.getUsers();
        List<user> ValidClientList = new ArrayList<>();
        for (user User : AllClientList)
        {
            if (User == null)
                continue;
            if (DetectLoginStatus) {
                if (!User.isUserLogined())
                    continue;
            }
            if (User.getUserSocket() == null)
                continue;
            if (User.getPublicKey() == null)
                continue;
            if (User.getUserAES() == null)
                continue;
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
    public static @NotNull user GetUserByUserName(@NotNull @Nls String UserName, @NotNull ServerMain ServerInstance, boolean DetectLoginStatus) throws AccountNotFoundException {
        List<user> ValidClientList = GetValidClientList(ServerInstance,DetectLoginStatus);
        for (user User : ValidClientList)
        {
            if (User.getUserName().equals(UserName)) {
                return User;
            }
        }
        throw new AccountNotFoundException("This UserName Is Not Found,if this UserName No Login?");//找不到用户时抛出异常
    }
}
