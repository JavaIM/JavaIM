package org.yuezhikong;


import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
//import org.apache.logging.log4j.LogManager;
//import org.apache.logging.log4j.Logger;

public class newServer {
    //public static final Logger logger = LogManager.getLogger(newServer.class);
    public static final Logger logger = Logger.getGlobal();
    private final List<Socket> sockets = new ArrayList<>();
    private int ClientID;
    private int clientIDAll = 0;

    public newServer(int port) {
        ServerSocket serverSocket = null;
        try {
            serverSocket = new ServerSocket(port);
            /* serverSocket.setSoTimeout(10000); */
        } catch (IOException e) {
            e.printStackTrace();
        }
        while (true)
        {
            try {
                assert serverSocket != null;
                Socket clientSocket = serverSocket.accept();//接受客户端Socket请求
                if (config.getMaxClient() >= 0)//检查是否已到最大
                {
                    //说下这里的逻辑
                    //客户端ID 客户端数量
                    //0 1
                    //1 2
                    //2 3
                    //假如限制为3
                    //那么就需要检测接下来要写入的ID是不是2或者大于2，如果是，那就是超过了
                    if (clientIDAll >= config.getMaxClient() -1)
                    {
                        clientSocket.close();
                        continue;
                    }
                }
                sockets.add(clientSocket);//添加到数组中
                ClientID = clientIDAll;//为临时变量ClientID赋值
                clientIDAll++;//当前的最大ClientID加1
                Runnable runnable = () -> {
                    int CurrentClientID = ClientID;//复制当前的SocketID
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
                                int i = 0;
                                int tmpclientidall = clientIDAll;
                                tmpclientidall = tmpclientidall -1;
                                while (true)
                                {
                                    if (i == CurrentClientID)
                                    {
                                        i = i + 1;
                                        continue;
                                    }
                                    if (i > tmpclientidall)
                                    {
                                        break;
                                    }
                                    Socket sendsocket = sockets.get(i);
                                    if (sendsocket == null)
                                    {
                                        i = i+1;
                                        continue;
                                    }
                                    BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(sendsocket.getOutputStream()));
                                    writer.write("客户端[" + CurrentClientSocket.getInetAddress() + ":" + CurrentClientSocket.getPort() + "]: " + ChatMessage+"\n");
                                    writer.newLine();
                                    writer.flush();
                                    if (i == tmpclientidall)
                                    {
                                        break;
                                    }
                                    i = i+1;
                                }
                            }
                        }
                    }
                    catch (IOException e)
                    {
                        if (!"Connection reset".equals(e.getMessage()))
                        {
                            logger.log(Level.SEVERE,"recvMessage线程出现IOException!");
                            logger.log(Level.SEVERE,"正在关闭Socket并打印报错堆栈！");
                            if (!CurrentClientSocket.isClosed())
                            {
                                try {
                                    CurrentClientSocket.close();
                                } catch (IOException ex) {
                                    logger.log(Level.SEVERE,"无法关闭Socket!");
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
                };
                Thread thread = new Thread(runnable);
                thread.start();
            }
            catch (IOException e)
            {
                logger.log(Level.SEVERE,"在accept时发生IOException");
                e.printStackTrace();
            }
        }
    }
}
