package org.yuezhikong;

import org.apache.commons.io.FileUtils;
import org.yuezhikong.utils.Logger;
import org.yuezhikong.utils.RSA;

import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.security.PrivateKey;

public class Client {
    //public static final Logger logger = LogManager.getLogger(Client.class);
    public static final Logger logger = new Logger();
    private Socket client;

    public Client(String serverName, int port) {
        Runnable recvmessage = () ->
        {
            PrivateKey privateKey = null;
            try {
                privateKey = RSA.loadPrivateKeyFromFile("pri.key");
            } catch (Exception e)
            {
                e.printStackTrace();
                System.exit(-1);
            }
            while (true)
            {
                BufferedReader reader;//获取输入流
                try {
                    reader = new BufferedReader(new InputStreamReader(client.getInputStream()));
                    String msg = reader.readLine();
                    if (msg == null)
                    {
                        logger.info("连接早已被关闭...");
                        System.exit(0);
                        break;
                    }
                    if (privateKey != null)
                    {
                        msg = java.net.URLDecoder.decode(RSA.decrypt(msg,privateKey),StandardCharsets.UTF_8);
                    }
                    logger.info(msg);
                }
                catch (IOException e)
                {
                    if (!"Connection reset by peer".equals(e.getMessage()) && !"Connection reset".equals(e.getMessage()) &&!"Socket is closed".equals(e.getMessage())) {
                        logger.warning("发生I/O错误");
                        e.printStackTrace();
                    }
                    else
                    {
                        logger.info("连接早已被关闭...");
                        System.exit(0);
                        break;
                    }
                }
            }
        };
        try {
            logger.info("连接到主机：" + serverName + " ，端口号：" + port);
            client = new Socket(serverName, port);
            logger.info("远程主机地址：" + client.getRemoteSocketAddress());
            OutputStream outToServer = client.getOutputStream();
            DataOutputStream out = new DataOutputStream(outToServer);

            java.security.PublicKey ServerPublicKey = RSA.loadPublicKeyFromFile("ServerPublicKey.key");
            out.writeUTF(java.net.URLEncoder.encode(FileUtils.readFileToString(new File("pub.key"), StandardCharsets.UTF_8),StandardCharsets.UTF_8));
            //out.writeUTF(RSA.encrypt(java.net.URLEncoder.encode("你", StandardCharsets.UTF_8),ServerPublicKey));

            out.writeUTF("Hello from " + client.getLocalSocketAddress());
            InputStream inFromServer = client.getInputStream();
            DataInputStream in = new DataInputStream(inFromServer);
            logger.info("服务器响应： " + in.readUTF());
            RSA.generateKeyToFile("pub.key","pri.key");
            Thread thread = new Thread(recvmessage);
            thread.start();
            thread.setName("RecvMessage Thread");

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
                // 加密信息
                input = java.net.URLEncoder.encode(input, StandardCharsets.UTF_8);
                input = RSA.encrypt(input, ServerPublicKey);
                // 发送消息给服务器
                writer.write(input);
                writer.newLine();
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
            logger.error("由于某些错误，我们无法发送您的信息");
            logger.error("报错信息："+e.getMessage());
        }
    }
}
