package org.yuezhikong.Server;

import org.apache.logging.log4j.Level;
import org.yuezhikong.utils.Logger;
import org.yuezhikong.utils.RSA;

import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Objects;

import static org.yuezhikong.config.GetRSA_Mode;

class RecvMessageThread extends Thread{
    private final int CurrentClientID;
    //private final List<Socket> sockets;
    private final List<user> Users;
    public static final Logger logger = new Logger();

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
        super.run();
        user CurrentClientClass = Users.get(CurrentClientID);
        Socket CurrentClientSocket = CurrentClientClass.GetUserSocket();
        try {
            logger.info("远程主机地址：" + CurrentClientSocket.getRemoteSocketAddress());
            DataInputStream in = new DataInputStream(CurrentClientSocket.getInputStream());
            String privateKey = null;
            if (GetRSA_Mode()) {
                //logger.info("密文："+input);
                //String RSADecode = RSA.decrypt(input,privatekey);
                //logger.info("RSA解密后，decode前："+RSADecode);
                //logger.info("decode后："+java.net.URLDecoder.decode(RSADecode, StandardCharsets.UTF_8));
                privateKey = Objects.requireNonNull(RSA.loadPrivateKeyFromFile("Private.key")).PrivateKey;
                CurrentClientClass.SetUserPublicKey(java.net.URLDecoder.decode(in.readUTF(), StandardCharsets.UTF_8));
            }
            logger.info(in.readUTF());
            DataOutputStream out = new DataOutputStream(CurrentClientSocket.getOutputStream());
            out.writeUTF("服务器连接成功：" + CurrentClientSocket.getLocalSocketAddress());
            while (true) {
                if (CurrentClientSocket.isClosed())
                {
                    logger.info("客户端[" + CurrentClientSocket.getInetAddress() + ":" + CurrentClientSocket.getPort() + "]: " + "已离线");
                    CurrentClientClass.UserDisconnect();
                    return;
                }
                BufferedReader reader = new BufferedReader(new InputStreamReader(CurrentClientSocket.getInputStream()));//获取输入流
                String ChatMessage;
                while ((ChatMessage = reader.readLine()) != null) {
                    // 解密信息
                    if (GetRSA_Mode()) {
                        ChatMessage = RSA.decrypt(ChatMessage,privateKey);
                    }
                    ChatMessage = java.net.URLDecoder.decode(ChatMessage, StandardCharsets.UTF_8);
                    if ("quit".equals(ChatMessage))// 退出登录服务端部分
                    {
                        logger.info("客户端[" + CurrentClientSocket.getInetAddress() + ":" + CurrentClientSocket.getPort() + "]: " + "正在退出登录");
                        CurrentClientClass.UserDisconnect();
                        return;
                    }
                    // 读取客户端发送的消息
                    logger.info("客户端[" + CurrentClientSocket.getInetAddress() + ":" + CurrentClientSocket.getPort() + "]: " + ChatMessage);
                    // 消息转发
                    newServer.GetInstance().SendMessageToAllClient("客户端[" + CurrentClientSocket.getInetAddress() + ":" + CurrentClientSocket.getPort() + "]: " + ChatMessage);
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
                e.printStackTrace();
            }
            else
            {
                logger.info("客户端[" + CurrentClientSocket.getInetAddress() + ":" + CurrentClientSocket.getPort() + "]: " + "已离线");
                CurrentClientClass.UserDisconnect();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
