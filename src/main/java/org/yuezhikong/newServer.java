package org.yuezhikong;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

public class newServer {
    private final List<Socket> sockets = new ArrayList<>();
    private int ClientID;

    public newServer(int port) {
        int clientIDAll = 0;
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
                sockets.add(clientSocket);//添加到数组中
                ClientID = clientIDAll;//为临时变量ClientID赋值
                clientIDAll++;//当前的最大ClientID加1
                Runnable runnable = () -> {
                    int CurrentClientID = ClientID;//复制当前的SocketID
                    Socket CurrentClientSocket = sockets.get(CurrentClientID);
                    try {
                        System.out.println("远程主机地址：" + CurrentClientSocket.getRemoteSocketAddress());
                        DataInputStream in = new DataInputStream(CurrentClientSocket.getInputStream());
                        System.out.println(in.readUTF());
                        DataOutputStream out = new DataOutputStream(CurrentClientSocket.getOutputStream());
                        out.writeUTF("服务器连接成功：" + CurrentClientSocket.getLocalSocketAddress());
                        while (true) {
                            if (CurrentClientSocket.isClosed())
                            {
                                Logger.getGlobal().info("客户端[" + CurrentClientSocket.getInetAddress() + ":" + CurrentClientSocket.getPort() + "]: " + "已离线");
                                sockets.set(CurrentClientID,null);
                                return;
                            }
                            BufferedReader reader = new BufferedReader(new InputStreamReader(CurrentClientSocket.getInputStream()));//获取输入流
                            String ChatMessage;
                            while ((ChatMessage = reader.readLine()) != null) {
                                if ("quit".equals(ChatMessage))// 退出登录服务端部分
                                {
                                    Logger.getGlobal().info("客户端[" + CurrentClientSocket.getInetAddress() + ":" + CurrentClientSocket.getPort() + "]: " + "正在退出登录");
                                    return;
                                }
                                // 读取客户端发送的消息
                                Logger.getGlobal().info("客户端[" + CurrentClientSocket.getInetAddress() + ":" + CurrentClientSocket.getPort() + "]: " + ChatMessage);
                            }
                        }
                    }
                    catch (IOException e)
                    {
                        Logger.getGlobal().warning("recvMessage线程出现IOException!");
                        Logger.getGlobal().warning("正在关闭Socket并打印报错堆栈！");
                        if (!CurrentClientSocket.isClosed())
                        {
                            try {
                                CurrentClientSocket.close();
                            } catch (IOException ex) {
                                Logger.getGlobal().warning("无法关闭Socket!");
                            }
                        }
                        e.printStackTrace();

                    }
                };
                Thread thread = new Thread(runnable);
                thread.start();
            }
            catch (IOException e)
            {
                Logger.getGlobal().warning("在accept时发生IOException");
                e.printStackTrace();
            }
        }
    }
}
