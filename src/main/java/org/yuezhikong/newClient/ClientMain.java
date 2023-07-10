/*
 * Simplified Chinese (简体中文)
 *
 * 版权所有 (C) 2023 QiLechan <qilechan@outlook.com> 和本程序的贡献者
 *
 * 本程序是自由软件：你可以再分发之和/或依照由自由软件基金会发布的 GNU 通用公共许可证修改之，无论是版本 3 许可证，还是 3 任何以后版都可以。
 * 发布该程序是希望它能有用，但是并无保障;甚至连可销售和符合某个特定的目的都不保证。请参看 GNU 通用公共许可证，了解详情。
 * 你应该随程序获得一份 GNU 通用公共许可证的副本。如果没有，请看 <https://www.gnu.org/licenses/>。
 * English (英语)
 *
 * Copyright (C) 2023 QiLechan <qilechan@outlook.com> and contributors to this program
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or 3 any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program. If not, see <https://www.gnu.org/licenses/>.
 */
package org.yuezhikong.newClient;

import cn.hutool.crypto.SecureUtil;
import cn.hutool.crypto.symmetric.AES;
import cn.hutool.crypto.symmetric.SymmetricAlgorithm;
import com.google.gson.Gson;
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
import java.net.SocketException;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;
import java.util.Timer;
import java.util.TimerTask;
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
        if ("Decryption Error".equals(json))
        {
            logger.error("你的服务端公钥疑似不正确");
            logger.error("服务端返回：Decryption Error");
            logger.error("服务端无法解密");
            logger.error("程序即将退出");
            System.exit(0);
        }
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
    private boolean UseUserNameAndPasswordLogin(@NotNull Socket client,@NotNull AES aes,@NotNull Logger logger) throws IOException {
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
            return false;
        }
        FileUtils.writeStringToFile(new File("./token.txt"),protocol.getMessageBody().getMessage(),StandardCharsets.UTF_8);
        return true;
    }
    protected Logger LoggerInit()
    {
        return new Logger(false,false,null,null);
    }
    public void start(String ServerAddress,int ServerPort)
    {
        Instance = this;
        Logger logger = LoggerInit();
        Timer timer = new Timer(true);
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
            TimerTask task = new TimerTask()
            {
                @Override
                public void run() {
                    try {
                        writer.write("Alive");
                        writer.newLine();
                        writer.flush();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            };
            timer.schedule(task,0,CodeDynamicConfig.HeartbeatInterval);
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
                    StartRecvMessageThread(client,aes,logger);
                }
                else if ("Fail".equals(protocol.getMessageBody().getMessage()))
                {
                    logger.info("Token无效！需重新使用用户名密码登录！");
                    if (!(UseUserNameAndPasswordLogin(client,aes,logger)))
                    {
                        logger.info("登录失败，用户名或密码错误");
                        return;
                    }
                    else
                    {
                        StartRecvMessageThread(client,aes,logger);
                    }
                }
                else
                {
                    logger.info("登录失败，非法响应标识："+aes.decryptStr(protocol.getMessageBody().getMessage()));
                }
            }
            else
            {
                if (!(UseUserNameAndPasswordLogin(client,aes,logger)))
                {
                    logger.info("登录失败，用户名或密码错误");
                    return;
                }
                else
                {
                    StartRecvMessageThread(client,aes,logger);
                }
            }
            SendMessage(logger,client,aes);
        } catch (IOException ignored) {
        }
        timer.cancel();
        System.exit(0);
    }

    private void SendMessage(Logger logger, Socket socket,AES aes) {
        Scanner scanner = new Scanner(System.in);
        try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()))) {
            while (true) {
                String UserInput = scanner.nextLine();
                if (".help".equals(UserInput)) {
                    logger.info("客户端命令系统");
                    logger.info(".help 查询帮助信息");
                    logger.info(".quit 离开服务器并退出程序");
                    if (CodeDynamicConfig.About_System) {
                        logger.info(".about 查看程序帮助");
                    }
                    continue;
                }
                if (CodeDynamicConfig.About_System) {
                    if (".about".equals(UserInput)) {
                        logger.info("JavaIM是根据GNU General Public License v3.0开源的自由程序（开源软件）");
                        logger.info("主仓库位于：https://github.com/JavaIM/JavaIM");
                        logger.info("主要开发者名单：");
                        logger.info("QiLechan（柒楽）");
                        logger.info("AlexLiuDev233 （阿白）");
                        continue;
                    }
                }
                if (".quit".equals(UserInput)) {
                    Gson gson = new Gson();
                    NormalProtocol protocol = new NormalProtocol();
                    NormalProtocol.MessageHead head = new NormalProtocol.MessageHead();
                    head.setVersion(CodeDynamicConfig.getProtocolVersion());
                    head.setType("Leave");
                    protocol.setMessageHead(head);
                    NormalProtocol.MessageBody body = new NormalProtocol.MessageBody();
                    body.setMessage(UserInput);
                    body.setFileLong(0);
                    protocol.setMessageBody(body);
                    writer.write(aes.encryptBase64(gson.toJson(protocol)));
                    writer.newLine();
                    writer.flush();
                    break;
                }
                Gson gson = new Gson();
                NormalProtocol protocol = new NormalProtocol();
                NormalProtocol.MessageHead head = new NormalProtocol.MessageHead();
                head.setVersion(CodeDynamicConfig.getProtocolVersion());
                head.setType("Chat");
                protocol.setMessageHead(head);
                NormalProtocol.MessageBody body = new NormalProtocol.MessageBody();
                body.setMessage(UserInput);
                body.setFileLong(0);
                protocol.setMessageBody(body);
                writer.write(aes.encryptBase64(gson.toJson(protocol)));
                writer.newLine();
                writer.flush();
            }
        } catch (IOException ignored) {}
        System.exit(0);
    }

    //启动RecvMessageThread
    private void StartRecvMessageThread(Socket client, AES aes,Logger logger) {
        new Thread()
        {
            @Override
            public void run() {
                this.setName("RecvMessage Thread");
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(client.getInputStream(), StandardCharsets.UTF_8))) {
                    String ChatMsg;
                    while (true) {
                        do {
                            ChatMsg = reader.readLine();
                        } while (ChatMsg == null);
                        ChatMsg = unicodeToString(ChatMsg);
                        ChatMsg = aes.decryptStr(ChatMsg);
                        NormalProtocol protocol = getClient().protocolRequest(ChatMsg);
                        if (protocol.getMessageHead().getVersion() != CodeDynamicConfig.getProtocolVersion() || !("Chat".equals(protocol.getMessageHead().getType())))
                        {
                            return;
                        }
                        logger.ChatMsg(protocol.getMessageBody().getMessage());
                    }
                } catch (IOException e) {
                    if (e instanceof SocketException)
                    {
                        System.exit(0);
                    }
                }
                System.exit(0);
            }
        }.start();
    }
}
