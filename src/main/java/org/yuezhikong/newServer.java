package org.yuezhikong;


import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import org.apache.logging.log4j.Level;
import org.yuezhikong.utils.Logger;

public class newServer {
    public static final Logger logger = new Logger();
    private final List<Socket> sockets = new ArrayList<>();
    private int ClientID;
    private int clientIDAll = 0;
    private ServerSocket serverSocket = null;
    private void SendMessageToAllClient(String Message,int clientID)
    {
        int i = 0;
        int tmpclientidall = clientIDAll;
        tmpclientidall = tmpclientidall - 1;
        try {
            while (true) {
                if (clientID != -1)
                {
                    if (i == clientID)
                    {
                        i = i + 1;
                        continue;
                    }
                }
                if (i > tmpclientidall) {
                    break;
                }
                Socket sendsocket = sockets.get(i);
                if (sendsocket == null) {
                    i = i + 1;
                    continue;
                }
                BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(sendsocket.getOutputStream()));
                writer.write(Message + "\n");
                writer.newLine();
                writer.flush();
                if (i == tmpclientidall) {
                    break;
                }
                i = i + 1;
            }
        } catch (IOException e) {
            logger.log(Level.ERROR, "SendMessage时出现IOException!");
            e.printStackTrace();
        }
    }


    public newServer(int port) {
        Runnable recvMessage = () -> {
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
                        SendMessageToAllClient("客户端[" + CurrentClientSocket.getInetAddress() + ":" + CurrentClientSocket.getPort() + "]: " + ChatMessage,CurrentClientID);
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
        };
        Runnable UserAuthThread = () -> {
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
                    Thread recvmessage = new Thread(recvMessage);
                    recvmessage.start();
                    recvmessage.setName("recvMessage Thread");
                }
                catch (IOException e)
                {
                    logger.log(Level.ERROR,"在accept时发生IOException");
                    e.printStackTrace();
                }
            }
        };
        try {
            serverSocket = new ServerSocket(port);
            /* serverSocket.setSoTimeout(10000); */
        } catch (IOException e) {
            e.printStackTrace();
        }
        Thread userAuthThread = new Thread(UserAuthThread);
        userAuthThread.start();
        userAuthThread.setName("UserAuthThread");
        System.out.print(">");
        Scanner sc = new Scanner(System.in);
        while (true)
        {
            String command;
            String[] argv;
            {
                String CommandLine = sc.nextLine();
                String[] CommandLineFormated = CommandLine.split("\\s+"); //分割一个或者多个空格
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

            //此时argv就是你想的那个argv，各个子指令均在这里，你可以自行更改
            //command就是命令类型
            switch (command) {
                case "help" -> {
                    logger.info("命令格式为：");
                    logger.info("kick ip 端口");
                    logger.info("say 信息");
                    logger.info("help 查看帮助");
                    logger.info("多余的空格将会被忽略");
                }
                case "say" -> {
                    StringBuilder TheServerWillSay = new StringBuilder();//服务端将要发出的信息
                    {
                        int i = 0;
                        if (0 != argv.length)
                        {
                            while (i < argv.length) {
                                if (i == argv.length - 1) {
                                    TheServerWillSay.append(argv[i]);
                                    break;
                                }
                                TheServerWillSay.append(argv[i]).append(" ");
                                i++;
                            }
                        }
                        else {
                            logger.info("您所输入的命令不正确");
                            logger.info("此命令的语法为say 信息");
                        }
                    }
                    logger.info("正在发出信息");
                    // 发送信息
                    SendMessageToAllClient("服务端广播:"+TheServerWillSay,-1);
                }
                case "kick" -> {
                    if (argv.length >= 2) {
                        String IpAddress = argv[0];
                        int Port;
                        try {
                            Port = Integer.parseInt(argv[1]);
                        }
                        catch (NumberFormatException e)
                        {
                            logger.info("输入的命令语法不正确，请检查后再输入");
                            continue;
                        }
                        {
                            int i = 0;
                            int tmpclientidall = clientIDAll;
                            tmpclientidall = tmpclientidall - 1;
                            try {
                                while (true) {
                                    if (i > tmpclientidall) {
                                        logger.info("错误，找不到此用户");
                                        break;
                                    }
                                    Socket sendsocket = sockets.get(i);
                                    if (sendsocket == null) {
                                        i = i + 1;
                                        continue;
                                    }
                                    if (sendsocket.getInetAddress().toString().equals("/"+IpAddress))
                                    {
                                        if (sendsocket.getPort() == Port)
                                        {
                                            logger.info("成功，已关闭IP为:"+sendsocket.getInetAddress()+"端口为："+sendsocket.getPort()+"的连接！");
                                            sendsocket.close();
                                            break;
                                        }
                                    }
                                    if (i == tmpclientidall) {
                                        logger.info("错误，找不到此用户");
                                        break;
                                    }
                                    i = i + 1;
                                }
                            } catch (IOException e) {
                                logger.log(Level.ERROR, "遍历用户时出现IOException!");
                                e.printStackTrace();
                            }
                        }
                    }
                }
                default -> logger.info("无效的命令！");
            }
        }
    }
}
