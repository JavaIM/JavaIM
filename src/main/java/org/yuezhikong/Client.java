package org.yuezhikong;

import cn.hutool.crypto.SecureUtil;
import cn.hutool.crypto.symmetric.SymmetricAlgorithm;
import org.apache.commons.codec.binary.Base64;
import org.apache.logging.log4j.LogManager;
import org.yuezhikong.utils.KeyData;
import org.yuezhikong.utils.Logger;
import org.yuezhikong.utils.RSA;
import org.yuezhikong.utils.SaveStackTrace;

import javax.crypto.SecretKey;
import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.security.PrivateKey;
import java.util.Objects;
import java.util.UUID;

import static org.yuezhikong.CodeDynamicConfig.GetRSA_Mode;
import static org.yuezhikong.CodeDynamicConfig.isAES_Mode;

public class Client {
    //public static final Logger logger = LogManager.getLogger(Client.class);
    public static final Logger logger = new Logger();
    private Socket client;
    private final KeyData RSAKey;
    private cn.hutool.crypto.symmetric.AES AES;
    private static byte[] Sha256ByteSubByte(byte[] src){
        byte[]bs=new byte[32];
        System.arraycopy(src, 0, bs, 0, 32);
        return bs;
    }
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
                        SaveStackTrace.saveStackTrace(e);
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
                    if (GetRSA_Mode()) {
                        if (isAES_Mode())
                        {
                            msg = AES.decryptStr(msg);
                        }
                        else {
                            if (privateKey != null) {
                                msg = RSA.decrypt(msg, privateKey);
                            } else {
                                logger.error("错误，您的私钥为null，但现在处于RSA模式，无法解密此消息！");
                                continue;
                            }
                        }
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
            InputStream inFromServer = client.getInputStream();
            DataInputStream in = new DataInputStream(inFromServer);
            String ServerPublicKey = null;
            if (GetRSA_Mode()) {
                ServerPublicKey = Objects.requireNonNull(RSA.loadPublicKeyFromFile("ServerPublicKey.key")).PublicKey;
                String ClientRSAKey = java.net.URLEncoder.encode(Base64.encodeBase64String(RSAKey.publicKey.getEncoded()), StandardCharsets.UTF_8);
                out.writeUTF(ClientRSAKey);
                logger.info("服务器响应："+RSA.decrypt(in.readUTF(),RSAKey.privateKey));
                out.writeUTF(RSA.encrypt("Hello,Server! This Message By Client RSA System",ServerPublicKey));
                if (isAES_Mode())
                {
                    //客户端随机uuid
                    String RandomByClient = UUID.randomUUID().toString();
                    out.writeUTF(RSA.encrypt(java.net.URLEncoder.encode(RandomByClient, StandardCharsets.UTF_8),ServerPublicKey));
                    String RandomByServer = java.net.URLDecoder.decode(RSA.decrypt(in.readUTF(),RSAKey.privateKey),StandardCharsets.UTF_8);
                    SecretKey key = SecureUtil.generateKey(SymmetricAlgorithm.AES.getValue(), Sha256ByteSubByte(SecureUtil.sha256(RandomByClient+RandomByServer).getBytes(StandardCharsets.UTF_8)));
                    AES = cn.hutool.crypto.SecureUtil.aes(key.getEncoded());
                    logger.info("服务器响应："+AES.decryptStr(in.readUTF()));
                    out.writeUTF(AES.encryptBase64("Hello,Server! This Message By Client AES System!"));
                }
                //out.writeUTF(RSA.encrypt(java.net.URLEncoder.encode("你", StandardCharsets.UTF_8),ServerPublicKey));
            }
            //后续握手过程还需测试RSA！
            out.writeUTF("Hello from " + client.getLocalSocketAddress());//通讯握手开始
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
                if (".about".equals(input))
                {
                    logger.info("JavaIM是根据GNU General Public License v3.0开源的自由程序（开源软件）");
                    logger.info("主仓库位于：https://github.com/QiLechan/JavaIM");
                    logger.info("主要开发者名单：");
                    logger.info("QiLechan（柒楽）");
                    logger.info("AlexLiuDev233 （阿白）");
                    logger.info("仓库启用了不允许协作者直接推送到主分支，需审核后再提交");
                    logger.info("因此，如果想要体验最新功能，请查看fork仓库，但不保证稳定性");
                }
                if (".help".equals(input))
                {
                    logger.info("客户端命令列表");
                    logger.info(".about 查看本程序相关信息");
                    logger.info(".quit 断开与服务器的连接并终止本程序");
                }
                // 检查用户输入是否是.quit
                if (".quit".equals(input))
                {
                    logger.info("正在断开连接");
                    writer.write("quit\n");
                    client.close();
                    break;
                }
                // 为控制台补上一个>
                System.out.print(">");
                // 加密信息
                input = java.net.URLEncoder.encode(input, StandardCharsets.UTF_8);
                if (GetRSA_Mode()) {
                    if (isAES_Mode())
                    {
                        input = AES.encryptBase64(input,StandardCharsets.UTF_8);
                    }
                    else
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