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
import org.yuezhikong.utils.DataBase.Database;
import org.yuezhikong.utils.Logger;
import org.yuezhikong.utils.Protocol.LoginProtocol;
import org.yuezhikong.utils.Protocol.NormalProtocol;
import org.yuezhikong.utils.RSA;
import org.yuezhikong.utils.SaveStackTrace;

import javax.crypto.SecretKey;
import javax.security.auth.login.AccountNotFoundException;
import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
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
            BufferedReader reader = new BufferedReader(new InputStreamReader(CurrentClientSocket.getInputStream()));//获取输入流
            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(CurrentClientSocket.getOutputStream()));
            {//新登录机制
                String data = reader.readLine();
                if (data == null)
                {
                    CurrentClientClass.UserDisconnect();
                    return;
                }
                if (GetRSA_Mode()) {
                    if (isAES_Mode())
                    {
                        data = CurrentClientClass.GetUserAES().decryptStr(data);
                    }
                    else {
                        data = RSA.decrypt(data, privateKey);
                    }
                }
                data = java.net.URLDecoder.decode(data,StandardCharsets.UTF_8);
                Gson gson = new Gson();
                NormalProtocol protocolData = gson.fromJson(data, NormalProtocol.class);
                if (protocolData.getMessageHead().getVersion() != CodeDynamicConfig.getProtocolVersion())
                {
                    CurrentClientClass.UserDisconnect();
                    return;
                }
                if (!protocolData.getMessageHead().getType().equals("Login"))
                {
                    CurrentClientClass.UserDisconnect();
                    return;
                }
                if ("Query".equals(protocolData.getMessageBody().getMessage()))
                {
                    try {
                        if (PluginManager.getInstance("./plugins").OnUserPreLogin(CurrentClientClass))
                        {
                            CurrentClientClass.UserDisconnect();
                            return;
                        }
                    } catch (ModeDisabledException ignored) {}
                    if (GetEnableLoginSystem())
                    {
                        gson = new Gson();
                        protocolData = new NormalProtocol();
                        NormalProtocol.MessageHead MessageHead = new NormalProtocol.MessageHead();
                        MessageHead.setType("Login");
                        MessageHead.setVersion(CodeDynamicConfig.getProtocolVersion());
                        protocolData.setMessageHead(MessageHead);
                        NormalProtocol.MessageBody MessageBody = new NormalProtocol.MessageBody();
                        MessageBody.setFileLong(0);
                        MessageBody.setMessage("Enable");
                        protocolData.setMessageBody(MessageBody);
                        data = gson.toJson(protocolData);
                        if (GetRSA_Mode()) {
                            String UserPublicKey = CurrentClientClass.GetUserPublicKey();
                            if (UserPublicKey == null) {
                                throw new NullPointerException();
                            }
                            data = java.net.URLEncoder.encode(data, StandardCharsets.UTF_8);
                            if (isAES_Mode())
                            {
                                data = CurrentClientClass.GetUserAES().encryptBase64(data);
                            }
                            else {
                                data = RSA.encrypt(data, UserPublicKey);
                            }
                        }
                        writer.write(data);
                        writer.newLine();
                        writer.flush();

                        data = reader.readLine();
                        if (GetRSA_Mode()) {
                            if (isAES_Mode())
                            {
                                data = CurrentClientClass.GetUserAES().decryptStr(data);
                            }
                            else {
                                data = RSA.decrypt(data, privateKey);
                            }
                        }
                        data = java.net.URLDecoder.decode(data, StandardCharsets.UTF_8);
                        gson = new Gson();
                        LoginProtocol LoginPacket = gson.fromJson(data, LoginProtocol.class);
                        if (LoginPacket.getLoginPacketHead().getType().equals("Token"))
                        {
                            final List<String> username = new ArrayList<>();
                            try {
                                new Thread() {
                                    @Override
                                    public void run() {
                                        this.setName("SQL Process Thread");
                                        try {
                                            Connection DatabaseConnection = Database.Init(CodeDynamicConfig.GetMySQLDataBaseHost(), CodeDynamicConfig.GetMySQLDataBasePort(), CodeDynamicConfig.GetMySQLDataBaseName(), CodeDynamicConfig.GetMySQLDataBaseUser(), CodeDynamicConfig.GetMySQLDataBasePasswd());
                                            String sql = "select * from UserData where token = ?";
                                            PreparedStatement ps = DatabaseConnection.prepareStatement(sql);
                                            ps.setString(1, LoginPacket.getLoginPacketBody().getReLogin().getToken());
                                            ResultSet rs = ps.executeQuery();
                                            if (rs.next()) {
                                                username.add(rs.getString("UserName"));
                                            }
                                            DatabaseConnection.close();
                                        } catch (Database.DatabaseException | SQLException e) {
                                            SaveStackTrace.saveStackTrace(e);
                                        } finally {
                                            Database.close();
                                        }
                                    }
                                    public Thread start2() {
                                        super.start();
                                        return this;
                                    }
                                }.start2().join();
                            } catch (InterruptedException ignored) {}
                            if (username.isEmpty())
                            {
                                gson = new Gson();
                                protocolData = new NormalProtocol();
                                MessageHead = new NormalProtocol.MessageHead();
                                MessageHead.setType("Login");
                                MessageHead.setVersion(CodeDynamicConfig.getProtocolVersion());
                                protocolData.setMessageHead(MessageHead);
                                MessageBody = new NormalProtocol.MessageBody();
                                MessageBody.setFileLong(0);
                                MessageBody.setMessage("Fail");
                                protocolData.setMessageBody(MessageBody);
                                data = gson.toJson(protocolData);
                                if (GetRSA_Mode()) {
                                    String UserPublicKey = CurrentClientClass.GetUserPublicKey();
                                    if (UserPublicKey == null) {
                                        throw new NullPointerException();
                                    }
                                    data = java.net.URLEncoder.encode(data, StandardCharsets.UTF_8);
                                    if (isAES_Mode())
                                    {
                                        data = CurrentClientClass.GetUserAES().encryptBase64(data);
                                    }
                                    else {
                                        data = RSA.encrypt(data, UserPublicKey);
                                    }
                                }
                                writer.write(data);
                                writer.newLine();
                                writer.flush();
                                try {
                                    data = reader.readLine();
                                    if (GetRSA_Mode()) {
                                        if (isAES_Mode())
                                        {
                                            data = CurrentClientClass.GetUserAES().decryptStr(data);
                                        }
                                        else {
                                            data = RSA.decrypt(data, privateKey);
                                        }
                                    }
                                    data = java.net.URLDecoder.decode(data, StandardCharsets.UTF_8);
                                    gson = new Gson();
                                    LoginProtocol LoginPacket1 = gson.fromJson(data, LoginProtocol.class);
                                    if ("passwd".equals(LoginPacket1.getLoginPacketHead().getType())) {
                                        if (!(UserLogin.WhetherTheUserIsAllowedToLogin(CurrentClientClass, logger, LoginPacket1.getLoginPacketBody().getNormalLogin().getUserName(), LoginPacket.getLoginPacketBody().getNormalLogin().getPasswd()))) {
                                            CurrentClientClass.UserDisconnect();
                                            return;
                                        }
                                    }
                                    else
                                    {
                                        CurrentClientClass.UserDisconnect();
                                        return;
                                    }
                                }
                                catch (UserAlreadyLoggedInException e)
                                {
                                    ServerAPI.SendMessageToUser(CurrentClientClass,"您的账户已经登录过了！");
                                    logger.info("["+CurrentClientClass.GetUserName()+"] [" + CurrentClientSocket.getInetAddress() + ":" + CurrentClientSocket.getPort() + "]: " + "已离线");
                                    CurrentClientClass.UserDisconnect();
                                    return;
                                }
                            }
                            else
                            {
                                gson = new Gson();
                                protocolData = new NormalProtocol();
                                MessageHead = new NormalProtocol.MessageHead();
                                MessageHead.setType("Login");
                                MessageHead.setVersion(CodeDynamicConfig.getProtocolVersion());
                                protocolData.setMessageHead(MessageHead);
                                MessageBody = new NormalProtocol.MessageBody();
                                MessageBody.setFileLong(0);
                                MessageBody.setMessage("Success");
                                protocolData.setMessageBody(MessageBody);
                                data = gson.toJson(protocolData);
                                if (GetRSA_Mode()) {
                                    String UserPublicKey = CurrentClientClass.GetUserPublicKey();
                                    if (UserPublicKey == null) {
                                        throw new NullPointerException();
                                    }
                                    data = java.net.URLEncoder.encode(data, StandardCharsets.UTF_8);
                                    if (isAES_Mode())
                                    {
                                        data = CurrentClientClass.GetUserAES().encryptBase64(data);
                                    }
                                    else {
                                        data = RSA.encrypt(data, UserPublicKey);
                                    }
                                }
                                writer.write(data);
                                writer.newLine();
                                writer.flush();
                                boolean found = true;
                                try
                                {
                                    ServerAPI.GetUserByUserName(username.get(0),Server.GetInstance(),true);
                                } catch (AccountNotFoundException e)
                                {
                                    found = false;
                                }
                                if (found)
                                {
                                    ServerAPI.SendMessageToUser(CurrentClientClass,"您的账户已经登录过了！");
                                    logger.info("["+CurrentClientClass.GetUserName()+"] [" + CurrentClientSocket.getInetAddress() + ":" + CurrentClientSocket.getPort() + "]: " + "已离线");
                                    CurrentClientClass.UserDisconnect();
                                    return;
                                }
                                CurrentClientClass.UserLogin(username.get(0));
                            }
                        }
                        else
                        {
                            try {
                                if (!(UserLogin.WhetherTheUserIsAllowedToLogin(CurrentClientClass,logger,LoginPacket.getLoginPacketBody().getNormalLogin().getUserName(),LoginPacket.getLoginPacketBody().getNormalLogin().getPasswd()))) {
                                    CurrentClientClass.UserDisconnect();
                                    return;
                                }
                            }
                            catch (UserAlreadyLoggedInException e)
                            {
                                ServerAPI.SendMessageToUser(CurrentClientClass,"您的账户已经登录过了！");
                                logger.info("["+CurrentClientClass.GetUserName()+"] [" + CurrentClientSocket.getInetAddress() + ":" + CurrentClientSocket.getPort() + "]: " + "已离线");
                                CurrentClientClass.UserDisconnect();
                                return;
                            }
                        }
                    }
                    else
                    {
                        gson = new Gson();
                        protocolData = new NormalProtocol();
                        NormalProtocol.MessageHead MessageHead = new NormalProtocol.MessageHead();
                        MessageHead.setType("Login");
                        MessageHead.setVersion(CodeDynamicConfig.getProtocolVersion());
                        protocolData.setMessageHead(MessageHead);
                        NormalProtocol.MessageBody MessageBody = new NormalProtocol.MessageBody();
                        MessageBody.setFileLong(0);
                        MessageBody.setMessage("Disable");
                        protocolData.setMessageBody(MessageBody);
                        data = gson.toJson(protocolData);
                        if (GetRSA_Mode()) {
                            String UserPublicKey = CurrentClientClass.GetUserPublicKey();
                            if (UserPublicKey == null) {
                                throw new NullPointerException();
                            }
                            data = java.net.URLEncoder.encode(data, StandardCharsets.UTF_8);
                            if (isAES_Mode())
                            {
                                data = CurrentClientClass.GetUserAES().encryptBase64(data);
                            }
                            else {
                                data = RSA.encrypt(data, UserPublicKey);
                            }
                        }
                        writer.write(data);
                        writer.newLine();
                        writer.flush();

                        data = reader.readLine();
                        if (GetRSA_Mode()) {
                            if (isAES_Mode())
                            {
                                data = CurrentClientClass.GetUserAES().decryptStr(data);
                            }
                            else {
                                data = RSA.decrypt(data, privateKey);
                            }
                        }
                        data = java.net.URLDecoder.decode(data, StandardCharsets.UTF_8);
                        gson = new Gson();
                        protocolData = gson.fromJson(data, NormalProtocol.class);
                        if (protocolData.getMessageHead().getVersion() != CodeDynamicConfig.getProtocolVersion())
                        {
                            CurrentClientClass.UserDisconnect();
                            return;
                        }
                        if (!protocolData.getMessageHead().getType().equals("Login"))
                        {
                            CurrentClientClass.UserDisconnect();
                            return;
                        }
                        else
                        {
                            CurrentClientClass.UserLogin(protocolData.getMessageBody().getMessage());
                        }
                    }
                }
                else
                {
                    CurrentClientClass.UserDisconnect();
                    return;
                }
            }
            while (true) {
                if (CurrentClientSocket.isClosed())
                {
                    logger.info("["+CurrentClientClass.GetUserName()+"] [" + CurrentClientSocket.getInetAddress() + ":" + CurrentClientSocket.getPort() + "]: " + "已离线");
                    CurrentClientClass.UserDisconnect();
                    return;
                }
                String ChatMessage;
                while ((ChatMessage = reader.readLine()) != null && !(CurrentClientSocket.isClosed())) {
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
                        ServerAPI.SendMessageToUser(CurrentClientClass,"警告，数据包非法");
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
