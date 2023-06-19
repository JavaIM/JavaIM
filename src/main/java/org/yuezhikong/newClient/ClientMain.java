package org.yuezhikong.newClient;

import cn.hutool.crypto.SecureUtil;
import cn.hutool.crypto.symmetric.AES;
import cn.hutool.crypto.symmetric.SymmetricAlgorithm;
import com.google.gson.Gson;
import com.mysql.cj.log.Log;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.FileUtils;
import org.jetbrains.annotations.NotNull;
import org.yuezhikong.CodeDynamicConfig;
import org.yuezhikong.GeneralMethod;
import org.yuezhikong.utils.CustomVar;
import org.yuezhikong.utils.Logger;
import org.yuezhikong.utils.Protocol.LoginProtocol;
import org.yuezhikong.utils.Protocol.NormalProtocol;
import org.yuezhikong.utils.RSA;

import javax.crypto.SecretKey;
import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;
import java.util.UUID;

public class ClientMain extends GeneralMethod {
    private CustomVar.KeyData keyData;
    private static ClientMain Instance;

    public static ClientMain getClient() {
        return Instance;
    }
    private void RequestRSA(@NotNull String key, @NotNull Socket client, @NotNull Logger logger) throws IOException {
        keyData = RSA.generateKeyToReturn();
        String pubkey = Base64.encodeBase64String(keyData.publicKey.getEncoded());
        logger.info("客户端密钥制作完成！");
        logger.info("公钥是："+pubkey);
        logger.info("私钥是："+Base64.encodeBase64String(keyData.privateKey.getEncoded()));
        String EncryptionKey = RSA.encrypt(pubkey, key);
        logger.info("加密后的Key是："+EncryptionKey);
        logger.info("正在发送公钥");
        Gson gson = new Gson();
        NormalProtocol protocol = new NormalProtocol();
        NormalProtocol.MessageHead head = new NormalProtocol.MessageHead();
        head.setVersion(CodeDynamicConfig.getProtocolVersion());
        head.setType("Encryption");
        protocol.setMessageHead(head);
        NormalProtocol.MessageBody body = new NormalProtocol.MessageBody();
        body.setMessage(EncryptionKey);
        body.setFileLong(0);
        protocol.setMessageBody(body);
        BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(client.getOutputStream(),StandardCharsets.UTF_8));
        writer.write(gson.toJson(protocol));
        writer.newLine();
        writer.flush();
        //发送完毕，开始测试
        //测试RSA
        String json;
        BufferedReader reader = new BufferedReader(new InputStreamReader(client.getInputStream(),StandardCharsets.UTF_8));
        do {
            json = reader.readLine();
        } while (json == null);
        json = unicodeToString(json);
        protocol = getClient().protocolRequest(json);
        if (protocol.getMessageHead().getVersion() != CodeDynamicConfig.getProtocolVersion() || !("Test".equals(protocol.getMessageHead().getType())))
        {
            return;
        }
        logger.info("服务端响应："+RSA.decrypt(protocol.getMessageBody().getMessage(),keyData.privateKey));

        protocol = new NormalProtocol();
        head = new NormalProtocol.MessageHead();
        head.setType("Test");
        head.setVersion(CodeDynamicConfig.getProtocolVersion());
        protocol.setMessageHead(head);
        body = new NormalProtocol.MessageBody();
        body.setMessage(RSA.encrypt("你好服务端",key));
        protocol.setMessageBody(body);
        writer.write(gson.toJson(protocol));
        writer.newLine();
        writer.flush();
    }
    private void UseUserNameAndPasswordLogin(@NotNull Socket client,@NotNull AES aes,@NotNull Logger logger) throws IOException {
        Scanner scanner = new Scanner(System.in);
        logger.info("请输入用户名：");
        String UserName = scanner.nextLine();
        logger.info("请输入密码：");
        String Password = scanner.nextLine();

        Gson gson = new Gson();
        BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(client.getOutputStream(),StandardCharsets.UTF_8));
        BufferedReader reader = new BufferedReader(new InputStreamReader(client.getInputStream(),StandardCharsets.UTF_8));
        LoginProtocol loginProtocol = new LoginProtocol();
        LoginProtocol.LoginPacketHeadBean loginPacketHead = new LoginProtocol.LoginPacketHeadBean();
        loginPacketHead.setType("passwd");
        loginProtocol.setLoginPacketHead(loginPacketHead);
        LoginProtocol.LoginPacketBodyBean loginPacketBody = new LoginProtocol.LoginPacketBodyBean();
        LoginProtocol.LoginPacketBodyBean.NormalLoginBean normalLoginBean = new LoginProtocol.LoginPacketBodyBean.NormalLoginBean();
        normalLoginBean.setUserName(UserName);
        normalLoginBean.setPasswd(Password);
        loginPacketBody.setNormalLogin(normalLoginBean);
        loginProtocol.setLoginPacketBody(loginPacketBody);
        String json = gson.toJson(loginProtocol);
        json = aes.encryptBase64(json);
        writer.write(json);
        writer.newLine();
        writer.flush();

        do {
            json = reader.readLine();
        } while (json == null);
        json = unicodeToString(json);
        json = aes.decryptStr(json);
        NormalProtocol protocol = getClient().protocolRequest(json);
        if (protocol.getMessageHead().getVersion() != CodeDynamicConfig.getProtocolVersion() || !("Login".equals(protocol.getMessageHead().getType())))
        {
            return;
        }
        FileUtils.writeStringToFile(new File("./token.txt"),protocol.getMessageBody().getMessage(),StandardCharsets.UTF_8);
    }
    protected Logger LoggerInit()
    {
        return new Logger(false,false,null,null);
    }
    public void start(String ServerAddress,int ServerPort)
    {
        Instance = this;
        Logger logger = LoggerInit();
        logger.info("正在连接主机：" + ServerAddress + " ，端口号：" + ServerPort);
        try (Socket client = new Socket(ServerAddress, ServerPort)) {
            logger.info("远程主机地址：" + client.getRemoteSocketAddress());
            //开始握手
            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(client.getOutputStream(),StandardCharsets.UTF_8));
            BufferedReader reader = new BufferedReader(new InputStreamReader(client.getInputStream(),StandardCharsets.UTF_8));
            //测试明文通讯
            logger.info("服务端响应："+unicodeToString(reader.readLine()));
            writer.write("Hello Server");
            writer.newLine();
            writer.flush();
            logger.info("服务端响应："+unicodeToString(reader.readLine()));
            writer.write("你好，服务端");
            writer.newLine();
            writer.flush();
            //测试通讯协议
            NormalProtocol protocol = protocolRequest(unicodeToString(reader.readLine()));
            if (protocol.getMessageHead().getVersion() != CodeDynamicConfig.getProtocolVersion() || !("Test".equals(protocol.getMessageHead().getType())))
            {
                return;
            }
            logger.info("服务端响应："+protocol.getMessageBody().getMessage());
            Gson gson = new Gson();
            protocol = new NormalProtocol();
            NormalProtocol.MessageHead head = new NormalProtocol.MessageHead();
            head.setVersion(CodeDynamicConfig.getProtocolVersion());
            head.setType("Test");
            protocol.setMessageHead(head);
            NormalProtocol.MessageBody body = new NormalProtocol.MessageBody();
            body.setMessage("你好服务端");
            body.setFileLong(0);
            protocol.setMessageBody(body);
            writer.write(gson.toJson(protocol));
            writer.newLine();
            writer.flush();
            //加密处理
            final String ServerPublicKey = FileUtils.readFileToString(new File("ServerPublicKey.txt"), StandardCharsets.UTF_8);
            RequestRSA(ServerPublicKey,client,logger);
            //AES制造开始
            String json;
            do {
                json = reader.readLine();
            } while (json == null);
            json = unicodeToString(json);
            protocol = getClient().protocolRequest(json);
            if (protocol.getMessageHead().getVersion() != CodeDynamicConfig.getProtocolVersion() || !("Encryption".equals(protocol.getMessageHead().getType())))
            {
                return;
            }
            String RandomForServer = RSA.decrypt(protocol.getMessageBody().getMessage(),keyData.privateKey);
            String RandomForClient = UUID.randomUUID().toString();
            protocol = new NormalProtocol();
            head = new NormalProtocol.MessageHead();
            head.setType("Encryption");
            head.setVersion(CodeDynamicConfig.getProtocolVersion());
            protocol.setMessageHead(head);
            body = new NormalProtocol.MessageBody();
            body.setMessage(RSA.encrypt(RandomForClient,ServerPublicKey));
            protocol.setMessageBody(body);
            writer.write(gson.toJson(protocol));
            writer.newLine();
            writer.flush();
            SecretKey key = SecureUtil.generateKey(SymmetricAlgorithm.AES.getValue(), Base64.decodeBase64(getClient().GenerateKey(RandomForServer+RandomForClient)));
            final AES aes = cn.hutool.crypto.SecureUtil.aes(key.getEncoded());
            do {
                json = reader.readLine();
            } while (json == null);
            json = unicodeToString(json);
            protocol = getClient().protocolRequest(json);
            if (protocol.getMessageHead().getVersion() != CodeDynamicConfig.getProtocolVersion() || !("Test".equals(protocol.getMessageHead().getType())))
            {
                return;
            }
            logger.info("服务器响应："+aes.decryptStr(protocol.getMessageBody().getMessage()));
            protocol = new NormalProtocol();
            head = new NormalProtocol.MessageHead();
            head.setVersion(CodeDynamicConfig.getProtocolVersion());
            head.setType("Test");
            protocol.setMessageHead(head);
            body = new NormalProtocol.MessageBody();
            body.setMessage(aes.encryptBase64("你好服务端"));
            body.setFileLong(0);
            protocol.setMessageBody(body);
            writer.write(gson.toJson(protocol));
            writer.newLine();
            writer.flush();
            do {
                json = reader.readLine();
            } while (json == null);
            json = unicodeToString(json);
            protocol = getClient().protocolRequest(json);
            if (protocol.getMessageHead().getVersion() != CodeDynamicConfig.getProtocolVersion() || !("UpdateProtocol".equals(protocol.getMessageHead().getType())))
            {
                return;
            }
            if (!("Update To All Encryption".equals(aes.decryptStr(protocol.getMessageBody().getMessage()))))
            {
                return;
            }
            protocol = new NormalProtocol();
            head = new NormalProtocol.MessageHead();
            head.setVersion(CodeDynamicConfig.getProtocolVersion());
            head.setType("UpdateProtocol");
            protocol.setMessageHead(head);
            body = new NormalProtocol.MessageBody();
            body.setMessage(aes.encryptBase64("ok"));
            body.setFileLong(0);
            protocol.setMessageBody(body);
            writer.write(gson.toJson(protocol));
            writer.newLine();
            writer.flush();
            //握手完成，接下来是登录逻辑
            if (new File("./token.txt").exists() && new File("./token.txt").isFile() && new File("./token.txt").canRead())
            {
                LoginProtocol loginProtocol = new LoginProtocol();
                LoginProtocol.LoginPacketHeadBean loginPacketHead = new LoginProtocol.LoginPacketHeadBean();
                loginPacketHead.setType("Token");
                loginProtocol.setLoginPacketHead(loginPacketHead);
                LoginProtocol.LoginPacketBodyBean loginPacketBody = new LoginProtocol.LoginPacketBodyBean();
                LoginProtocol.LoginPacketBodyBean.ReLoginBean reLogin = new LoginProtocol.LoginPacketBodyBean.ReLoginBean();
                reLogin.setToken(FileUtils.readFileToString(new File("./token.txt"),StandardCharsets.UTF_8));
                loginPacketBody.setReLogin(reLogin);
                loginProtocol.setLoginPacketBody(loginPacketBody);
                json = gson.toJson(loginProtocol);
                json = aes.encryptBase64(json);
                writer.write(json);
                writer.newLine();
                writer.flush();
                do {
                    json = reader.readLine();
                } while (json == null);
                json = unicodeToString(json);
                json = aes.decryptStr(json);
                protocol = getClient().protocolRequest(json);
                if (protocol.getMessageHead().getVersion() != CodeDynamicConfig.getProtocolVersion() || !("Login".equals(protocol.getMessageHead().getType())))
                {
                    return;
                }
                if ("Success".equals(protocol.getMessageBody().getMessage()))
                {
                    StartRecvMessageThread(client,aes);
                }
                else if ("Fail".equals(protocol.getMessageBody().getMessage()))
                {
                    logger.info("Token无效！需重新使用用户名密码登录！");
                    UseUserNameAndPasswordLogin(client,aes,logger);
                }
                else
                {
                    logger.info("登录失败，非法响应标识："+aes.decryptStr(protocol.getMessageBody().getMessage()));
                }
            }
            else
            {
                UseUserNameAndPasswordLogin(client,aes,logger);
            }
        } catch (IOException ignored) {
        }
    }
    //启动RecvMessageThread
    private void StartRecvMessageThread(Socket client, AES aes) {

    }
}
