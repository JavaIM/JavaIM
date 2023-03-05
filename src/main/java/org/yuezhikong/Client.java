package org.yuezhikong;

import org.apache.commons.codec.binary.Base64;
import org.apache.logging.log4j.LogManager;
import org.yuezhikong.utils.KeyData;
import org.yuezhikong.utils.Logger;
import org.yuezhikong.utils.RSA;

import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.security.PrivateKey;
import java.util.Objects;

import static org.yuezhikong.CodeDynamicConfig.GetRSA_Mode;

public class Client {
    //public static final Logger logger = LogManager.getLogger(Client.class);
    public static final Logger logger = new Logger();
    private Socket client;
    private final KeyData RSAKey;

    public Client(String serverName, int port) {
        {
            if (!(new File("ServerPublicKey.key").exists()))
            {
                Logger.logger_root.fatal("在运行目录下未找到ServerPublicKey.key");
                Logger.logger_root.fatal("此文件为服务端公钥文件，用于保证通信安全");
                Logger.logger_root.fatal("由于此文件缺失，客户端即将停止运行");
                System.exit(-1);
            }
        }
        RSAKey = RSA.generateKeyToReturn();
        Runnable recvmessage = () ->
        {
            PrivateKey privateKey = null;
            if (GetRSA_Mode()) {
                try {
                    privateKey = RSAKey.privateKey;
                } catch (Exception e) {
                    if (!"Socket closed".equals(e.getMessage()))
                    {
                        StringWriter sw = new StringWriter();
                        PrintWriter pw = new PrintWriter(sw);
                        e.printStackTrace(pw);
                        pw.flush();
                        sw.flush();
                        org.apache.logging.log4j.Logger logger_log4j = LogManager.getLogger("Debug");
                        logger_log4j.debug(sw.toString());
                        pw.close();
                        try {
                            sw.close();
                        }
                        catch (IOException ex)
                        {
                            ex.printStackTrace();
                        }
                    }
                    System.exit(-1);
                }
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
                        msg = RSA.decrypt(msg,privateKey);
                    }
                    msg = java.net.URLDecoder.decode(msg,StandardCharsets.UTF_8);
                    logger.info(msg);
                }
                catch (IOException e)
                {
                    if (!"Connection reset by peer".equals(e.getMessage()) && !"Connection reset".equals(e.getMessage()) && !"Socket is closed".equals(e.getMessage()))  {
                        logger.warning("发生I/O错误");
                        StringWriter sw = new StringWriter();
                        PrintWriter pw = new PrintWriter(sw);
                        e.printStackTrace(pw);
                        pw.flush();
                        sw.flush();
                        org.apache.logging.log4j.Logger logger_log4j = LogManager.getLogger("Debug");
                        logger_log4j.debug(sw.toString());
                        pw.close();
                        try {
                            sw.close();
                        }
                        catch (IOException ex)
                        {
                            ex.printStackTrace();
                        }
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
            String ServerPublicKey = null;
            if (GetRSA_Mode()) {
                ServerPublicKey = Objects.requireNonNull(RSA.loadPublicKeyFromFile("ServerPublicKey.key")).PublicKey;
                String ClientRSAKey = java.net.URLEncoder.encode(Base64.encodeBase64String(RSAKey.publicKey.getEncoded()), StandardCharsets.UTF_8);
                out.writeUTF(ClientRSAKey);
                //out.writeUTF(RSA.encrypt(java.net.URLEncoder.encode("你", StandardCharsets.UTF_8),ServerPublicKey));
            }
            //后续握手过程还需测试RSA！
            out.writeUTF("Hello from " + client.getLocalSocketAddress());//通讯握手开始
            InputStream inFromServer = client.getInputStream();
            DataInputStream in = new DataInputStream(inFromServer);
            logger.info("服务器响应： " + in.readUTF());//握手结束
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
                if (GetRSA_Mode()) {
                    input = RSA.encrypt(input, ServerPublicKey);
                }
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
                StringWriter sw = new StringWriter();
                PrintWriter pw = new PrintWriter(sw);
                e.printStackTrace(pw);
                pw.flush();
                sw.flush();
                org.apache.logging.log4j.Logger logger_log4j = LogManager.getLogger("Debug");
                logger_log4j.debug(sw.toString());
                pw.close();
                try {
                    sw.close();
                }
                catch (IOException ex)
                {
                    ex.printStackTrace();
                }
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
