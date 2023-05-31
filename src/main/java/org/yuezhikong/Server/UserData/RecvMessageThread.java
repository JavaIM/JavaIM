package org.yuezhikong.Server.UserData;

import cn.hutool.crypto.SecureUtil;
import cn.hutool.crypto.symmetric.SymmetricAlgorithm;
import com.google.gson.Gson;
import org.apache.commons.codec.binary.Base64;
import org.apache.logging.log4j.Level;
import org.yuezhikong.CodeDynamicConfig;
import org.yuezhikong.Server.Commands.RequestCommand;
import org.yuezhikong.Server.LoginSystem.UserLogin;
import org.yuezhikong.Server.Server;
import org.yuezhikong.Server.api.ServerAPI;
import org.yuezhikong.Server.plugin.PluginManager;
import org.yuezhikong.utils.CustomExceptions.ModeDisabledException;
import org.yuezhikong.utils.CustomExceptions.UserAlreadyLoggedInException;
import org.yuezhikong.utils.CustomVar;
import org.yuezhikong.utils.Logger;
import org.yuezhikong.utils.Protocol.NormalProtocol;
import org.yuezhikong.utils.RSA;

import javax.crypto.SecretKey;
import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import static org.yuezhikong.CodeDynamicConfig.*;

public class RecvMessageThread extends Thread{
    private final int CurrentClientID;
    //private final List<Socket> sockets;
    private final List<user> Users;
    public static final Logger logger = Server.GetInstance().logger;
    /**
     * @apiNote 用于给recvMessage线程传参
     * @param ClientID 客户端ID
     */
    public RecvMessageThread(int ClientID, List<user> users)
    {
        CurrentClientID = ClientID;
        Users = users;
    }

    @Override
    public void run() {
        user CurrentClientClass = Users.get(CurrentClientID);
        this.setUncaughtExceptionHandler((Thread t, Throwable e)-> CurrentClientClass.UserDisconnect());
        Socket CurrentClientSocket = CurrentClientClass.GetUserSocket();
        try {
            //开始握手
            logger.info("远程主机地址：" + CurrentClientSocket.getRemoteSocketAddress());
            //获取InputStream流
            DataInputStream in = new DataInputStream(CurrentClientSocket.getInputStream());
            //获取OutputStream流
            DataOutputStream out = new DataOutputStream(CurrentClientSocket.getOutputStream());
            String privateKey = null;
            if (GetRSA_Mode()) {//根据RSA模式，决定是否进行RSA握手
                privateKey = Objects.requireNonNull(RSA.loadPrivateKeyFromFile("Private.txt")).PrivateKey;
                CurrentClientClass.SetUserPublicKey(java.net.URLDecoder.decode(in.readUTF(), StandardCharsets.UTF_8));
                out.writeUTF(RSA.encrypt("Hello,Client! This Message By Server RSA System",CurrentClientClass.GetUserPublicKey()));
                logger.info("正在连接的客户端响应："+RSA.decrypt(in.readUTF(),privateKey));
                if (isAES_Mode())
                {
                    String RandomByClient = java.net.URLDecoder.decode(RSA.decrypt(in.readUTF(),privateKey),StandardCharsets.UTF_8);
                    String RandomByServer = UUID.randomUUID().toString();
                    out.writeUTF(RSA.encrypt(java.net.URLEncoder.encode(RandomByServer, StandardCharsets.UTF_8),CurrentClientClass.GetUserPublicKey()));
                    byte[] KeyByte = new byte[32];
                    byte[] SrcByte = Base64.encodeBase64((RandomByClient+RandomByServer).getBytes(StandardCharsets.UTF_8));
                    System.arraycopy(SrcByte,0,KeyByte,0,31);
                    SecretKey key = SecureUtil.generateKey(SymmetricAlgorithm.AES.getValue(),KeyByte);
                    CurrentClientClass.SetUserAES(cn.hutool.crypto.SecureUtil.aes(key.getEncoded()));
                    out.writeUTF(CurrentClientClass.GetUserAES().encryptBase64("Hello,Client! This Message By Server AES System"));
                    logger.info("正在连接的客户端响应："+CurrentClientClass.GetUserAES().decryptStr(in.readUTF()));
                }
            }
            logger.info(in.readUTF());
            out.writeUTF("服务器连接成功：" + CurrentClientSocket.getLocalSocketAddress());
            if (GetEnableLoginSystem())
            {
                try {
                    if (!(UserLogin.WhetherTheUserIsAllowedToLogin(CurrentClientClass,logger))) {
                        CurrentClientClass.UserDisconnect();
                        return;
                    }
                    PluginManager.getInstance("./plugins").OnUserLogin(CurrentClientClass);
                }
                catch (UserAlreadyLoggedInException e)
                {
                    ServerAPI.SendMessageToUser(CurrentClientClass,"您的账户已经登录过了！");
                    logger.info("["+CurrentClientClass.GetUserName()+"] [" + CurrentClientSocket.getInetAddress() + ":" + CurrentClientSocket.getPort() + "]: " + "已离线");
                    CurrentClientClass.UserDisconnect();
                    return;
                }
                catch (NullPointerException e)
                {
                    ServerAPI.SendMessageToUser(CurrentClientClass,"来来来，你给我解释一下，你是怎么做到发的信息是null的？");
                    logger.info("["+CurrentClientClass.GetUserName()+"] [" + CurrentClientSocket.getInetAddress() + ":" + CurrentClientSocket.getPort() + "]: " + "已离线");
                    CurrentClientClass.UserDisconnect();
                    return;
                }
                catch (ModeDisabledException ignored) {}
            }
            else
            {
                ServerAPI.SendMessageToUser(CurrentClientClass,"在进入之前，您必须设置用户名");
                Thread.sleep(250);
                ServerAPI.SendMessageToUser(CurrentClientClass,"请输入您的用户名：");
                String UserName;
                BufferedReader reader = new BufferedReader(new InputStreamReader(CurrentClientSocket.getInputStream()));//获取输入流
                UserName = reader.readLine();
                if (UserName == null)
                {
                    ServerAPI.SendMessageToUser(CurrentClientClass,"来来来，你给我解释一下，你是怎么做到发的信息是null的？");
                    CurrentClientClass.UserDisconnect();
                    logger.info("["+CurrentClientClass.GetUserName()+"] [" + CurrentClientSocket.getInetAddress() + ":" + CurrentClientSocket.getPort() + "]: " + "已离线");
                    return;
                }
                if (GetRSA_Mode()) {
                    if (isAES_Mode())
                    {
                        UserName = CurrentClientClass.GetUserAES().decryptStr(UserName);
                    }
                    else {
                        UserName = RSA.decrypt(UserName, Objects.requireNonNull(RSA.loadPrivateKeyFromFile("Private.txt")).PrivateKey);
                    }
                }
                UserName = java.net.URLDecoder.decode(UserName, StandardCharsets.UTF_8);
                CurrentClientClass.UserLogin(UserName);
            }
            while (true) {
                if (CurrentClientSocket.isClosed())
                {
                    logger.info("["+CurrentClientClass.GetUserName()+"] [" + CurrentClientSocket.getInetAddress() + ":" + CurrentClientSocket.getPort() + "]: " + "已离线");
                    CurrentClientClass.UserDisconnect();
                    return;
                }
                BufferedReader reader = new BufferedReader(new InputStreamReader(CurrentClientSocket.getInputStream()));//获取输入流
                String ChatMessage;
                while ((ChatMessage = reader.readLine()) != null) {
                    // 解密信息
                    if (GetRSA_Mode()) {
                        if (isAES_Mode())
                        {
                            ChatMessage = CurrentClientClass.GetUserAES().decryptStr(ChatMessage);
                        }
                        else {
                            ChatMessage = RSA.decrypt(ChatMessage, privateKey);
                        }
                    }
                    ChatMessage = java.net.URLDecoder.decode(ChatMessage, StandardCharsets.UTF_8);
                    // 将信息从Protocol Json中取出
                    Gson gson = new Gson();
                    NormalProtocol protocolData = gson.fromJson(ChatMessage, NormalProtocol.class);
                    if (protocolData.getMessageHead().getVersion() != CodeDynamicConfig.getProtocolVersion())
                    {
                        CurrentClientClass.UserDisconnect();
                        return;
                    }
                    // type目前只实现了chat,FileTransfer延后
                    if (protocolData.getMessageHead().getType().equals("FileTransfer"))
                    {
                        ServerAPI.SendMessageToUser(CurrentClientClass,"此服务器暂不支持FileTransfer协议");
                        break;
                    }
                    else if (!protocolData.getMessageHead().getType().equals("Chat"))
                    {
                        ServerAPI.SendMessageToUser(CurrentClientClass,"警告，数据包非法，将会发回");
                        break;
                    }
                    ChatMessage = protocolData.getMessageBody().getMessage();
                    //客户端可以非法发送换行修复
                    ChatMessage = ChatMessage.replaceAll("\n","");
                    ChatMessage = ChatMessage.replaceAll("\r","");
                    if ("quit".equals(ChatMessage))// 退出登录服务端部分
                    {
                        logger.info("["+CurrentClientClass.GetUserName()+"] [" + CurrentClientSocket.getInetAddress() + ":" + CurrentClientSocket.getPort() + "]: " + "正在退出登录");
                        CurrentClientClass.UserDisconnect();
                        return;
                    }
                    if (CurrentClientClass.GetUserPermission() == -1)//ban人处理
                    {
                        logger.info("["+CurrentClientClass.GetUserName()+"] [" + CurrentClientSocket.getInetAddress() + ":" + CurrentClientSocket.getPort() + "]: " + "被封禁人员，正在被强制退出登录");
                        CurrentClientClass.UserDisconnect();
                        return;
                    }
                    if (ChatMessage.charAt(0) == '/')//判定是否是斜杠打头，如果是，判定为命令
                    {
                        CustomVar.Command CommandRequestResult = ServerAPI.CommandFormat(ChatMessage);//命令格式化
                        logger.info(CurrentClientClass.GetUserName()+" 执行了命令 "+CommandRequestResult.Command());
                        RequestCommand.CommandRequest(CommandRequestResult.Command(),CommandRequestResult.argv(),CurrentClientClass);//调用处理
                        continue;
                    }
                    if (CurrentClientClass.isMuted())
                        continue;
                    //插件处理
                    if (CodeDynamicConfig.GetPluginSystemMode()) {
                        if (Objects.requireNonNull(PluginManager.getInstance("./plugins")).OnUserChat(CurrentClientClass,ChatMessage))
                            continue;
                    }
                    // 读取客户端发送的消息
                    logger.ChatMsg("["+CurrentClientClass.GetUserName()+"] [" + CurrentClientSocket.getInetAddress() + ":" + CurrentClientSocket.getPort() + "]: " + ChatMessage);
                    // 消息转发
                    org.yuezhikong.Server.api.ServerAPI.SendMessageToAllClient("["+CurrentClientClass.GetUserName()+"] [" + CurrentClientSocket.getInetAddress() + ":" + CurrentClientSocket.getPort() + "]: " + ChatMessage , Server.GetInstance());
                }
            }
        }
        catch (IOException e)
        {
            if (!"Connection reset".equals(e.getMessage()) && !"Socket closed".equals(e.getMessage()))
            {
                logger.log(Level.ERROR,"recvMessage线程出现IOException!");
                logger.log(Level.ERROR,"正在关闭Socket并打印报错堆栈！");
                if (!CurrentClientSocket.isClosed())
                {
                    try {
                        CurrentClientSocket.close();
                    } catch (IOException ex) {
                        logger.log(Level.ERROR,"无法关闭Socket!");
                    }
                }
                org.yuezhikong.utils.SaveStackTrace.saveStackTrace(e);
            }
            else
            {
                logger.info("["+CurrentClientClass.GetUserName()+"] [" + CurrentClientSocket.getInetAddress() + ":" + CurrentClientSocket.getPort() + "]: " + "已离线");
                CurrentClientClass.UserDisconnect();
            }
        } catch (Exception e) {
            org.yuezhikong.utils.SaveStackTrace.saveStackTrace(e);
        }
    }
}
