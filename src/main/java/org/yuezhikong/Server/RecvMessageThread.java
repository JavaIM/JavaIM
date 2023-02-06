package org.yuezhikong.Server;

import org.apache.logging.log4j.Level;
import org.yuezhikong.utils.Logger;

import java.io.*;
import java.net.Socket;
import java.util.List;

class RecvMessageThread extends Thread{
    private final int clientid;
    private final List<Socket> sockets;
    public static final Logger logger = new Logger();

    /**
     * @apiNote 用于给recvMessage线程传参
     * @param ClientID 客户端ID
     */
    public RecvMessageThread(int ClientID, List<Socket> socketList)
    {
        clientid = ClientID;
        sockets = socketList;
    }

    @Override
    public void run() {
        super.run();
        int CurrentClientID = clientid;//复制当前的SocketID
        Socket CurrentClientSocket = sockets.get(CurrentClientID);
        try {
            logger.info("远程主机地址：" + CurrentClientSocket.getRemoteSocketAddress());
            DataInputStream in = new DataInputStream(CurrentClientSocket.getInputStream());
            logger.info(in.readUTF());
            DataOutputStream out = new DataOutputStream(CurrentClientSocket.getOutputStream());
            out.writeUTF("服务器连接成功：" + CurrentClientSocket.getLocalSocketAddress());
            while (true) {
                if (CurrentClientSocket.isClosed())
                {
                    logger.info("客户端[" + CurrentClientSocket.getInetAddress() + ":" + CurrentClientSocket.getPort() + "]: " + "已离线");
                    sockets.set(CurrentClientID,null);
                    return;
                }
                BufferedReader reader = new BufferedReader(new InputStreamReader(CurrentClientSocket.getInputStream()));//获取输入流
                String ChatMessage;
                while ((ChatMessage = reader.readLine()) != null) {
                    if ("quit".equals(ChatMessage))// 退出登录服务端部分
                    {
                        logger.info("客户端[" + CurrentClientSocket.getInetAddress() + ":" + CurrentClientSocket.getPort() + "]: " + "正在退出登录");
                        sockets.set(CurrentClientID,null);
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
                sockets.set(CurrentClientID,null);
            }
        }
    }
}
