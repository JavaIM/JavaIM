package org.yuezhikong;

import org.yuezhikong.utils.Logger;
import org.yuezhikong.utils.RSA;

import java.io.*;
import java.net.Socket;
import java.util.Scanner;
//import java.util.logging.Logger;
//import org.apache.logging.log4j.LogManager;
//import org.apache.logging.log4j.Logger;

public class Client {
    //public static final Logger logger = LogManager.getLogger(Client.class);
    public static final Logger logger = new Logger();
    private Socket client;
    public Client(String serverName, int port) {
        try {
            logger.info("连接到主机：" + serverName + " ，端口号：" + port);
            client = new Socket(serverName, port);
            logger.info("远程主机地址：" + client.getRemoteSocketAddress());
            OutputStream outToServer = client.getOutputStream();
            DataOutputStream out = new DataOutputStream(outToServer);
            out.writeUTF("Hello from " + client.getLocalSocketAddress());
            InputStream inFromServer = client.getInputStream();
            DataInputStream in = new DataInputStream(inFromServer);
            logger.info("服务器响应： " + in.readUTF());
            logger.info("请输入加密方法，使用AES算法生成RSA密钥请输入1：");
            Scanner sc = new Scanner(System.in);
            int mode = 0;
            String algorithm;
            mode = Integer.parseInt(sc.nextLine());
            if (mode == 1) {
                algorithm = "AES-128";
                String pubPath = "pub.key";
                String priPath = "pri.key";
                RSA.generateKeyToFile(algorithm,pubPath,priPath);
            }

            Runnable runnable = () ->
            {
                while (true)
                {
                    BufferedReader reader;//获取输入流
                    try {
                        reader = new BufferedReader(new InputStreamReader(client.getInputStream()));
                        String msg = reader.readLine();
                        if (msg == null)
                        {
                            logger.info("您可能已被服务端强制踢下线");
                            break;
                        }
                        logger.info(msg);
                    }
                    catch (IOException e)
                    {
                        if (!"Connection reset by peer".equals(e.getMessage()) && !"Connection reset".equals(e.getMessage())) {
                            logger.warning("发生I/O错误");
                            e.printStackTrace();
                        }
                        else
                        {
                            logger.info("连接早已被关闭...");
                            break;
                        }
                    }
                }
            };
            Thread thread = new Thread(runnable);
            thread.start();

            BufferedWriter writer;
            BufferedReader consoleReader;

            while (true) {
                writer = new BufferedWriter(new OutputStreamWriter(client.getOutputStream()));
                // 等待用户输入信息
                consoleReader = new BufferedReader(new InputStreamReader(System.in));
                String input = consoleReader.readLine();
                // 检查用户输入是否是quit
                if ("quit".equals(input))
                {
                    logger.info("正在断开连接");
                    writer.write(input + "\n");
                    client.close();
                    break;
                }
                // 为控制台补上一个>
                System.out.print(">");
                // 发送消息给服务器
                writer.write(input + "\n");
                writer.flush();
            }
            logger.info("再见~");
        }
        catch (IOException e)
        {
            if (!"Connection reset by peer".equals(e.getMessage()) && !"Connection reset".equals(e.getMessage())) {
                logger.warning("发生I/O错误");
                e.printStackTrace();
            }
            else
            {
                logger.info("连接早已被关闭...");
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
