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
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.yuezhikong.CodeDynamicConfig;
import org.yuezhikong.CrashReport;
import org.yuezhikong.GeneralMethod;
import org.yuezhikong.utils.CustomVar;
import org.yuezhikong.utils.Logger;
import org.yuezhikong.utils.Protocol.LoginProtocol;
import org.yuezhikong.utils.Protocol.NormalProtocol;
import org.yuezhikong.utils.Protocol.TransferProtocol;
import org.yuezhikong.utils.RSA;
import org.yuezhikong.utils.SaveStackTrace;

import javax.crypto.SecretKey;
import java.io.*;
import java.net.Socket;
import java.net.SocketException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

public class ClientMain extends GeneralMethod {
    private CustomVar.KeyData keyData;
    private static ClientMain Instance;
    private String Address;
    private String endToEndEncryptionData = "";
    private final Object ConsoleInputLock = new Object();
    private String ConsoleInput = "";
    private boolean needConsoleInput = false;

    public static ClientMain getClient() {
        return Instance;
    }
    private void RequestRSA(@NotNull String key, @NotNull Socket client, @NotNull Logger logger) throws IOException {

        RSA_KeyAutogenerate("ClientPublicKey.txt","ClientPrivateKey.txt",logger);
        keyData = new CustomVar.KeyData();
        cn.hutool.crypto.asymmetric.RSA rsa = new cn.hutool.crypto.asymmetric.RSA(FileUtils.readFileToString(
                new File("ClientPrivateKey.txt"),
                StandardCharsets.UTF_8),
                FileUtils.readFileToString(new File("ClientPublicKey.txt"),StandardCharsets.UTF_8));
        keyData.publicKey = rsa.getPublicKey();
        keyData.privateKey = rsa.getPrivateKey();
        keyData.PublicKey = FileUtils.readFileToString(new File("ClientPublicKey.txt"),StandardCharsets.UTF_8);
        keyData.PrivateKey = FileUtils.readFileToString(new File("ClientPrivateKey.txt"),StandardCharsets.UTF_8);

        logger.info("客户端密钥制作完成！");
        logger.info("公钥是："+keyData.PublicKey);
        logger.info("私钥是："+keyData.PrivateKey);
        String EncryptionKey = RSA.encrypt(keyData.PublicKey, key);
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
        String Password = SecureUtil.sha256(scanner.nextLine());
        return tryLogin(client,aes, UserName,Password);
    }
    private boolean tryLogin(@NotNull Socket client, @NotNull AES aes, @NotNull @Nls String UserName, @NotNull @Nls String Password) throws IOException {
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
        return new Logger();
    }
    public void start(String ServerAddress,int ServerPort)
    {
        Instance = this;
        Address = ServerAddress;
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

            do {
                json = reader.readLine();
            } while (json == null);
            json = getClient().unicodeToString(json);
            json = aes.decryptStr(json);
            protocol = getClient().protocolRequest(json);
            if (protocol.getMessageHead().getVersion() != CodeDynamicConfig.getProtocolVersion() || !("options".equals(protocol.getMessageHead().getType())))
            {
                return;
            }
            if (!("AllowTransferProtocol".equals(protocol.getMessageBody().getMessage())))
            {
                return;
            }
            protocol = new NormalProtocol();
            head = new NormalProtocol.MessageHead();
            head.setVersion(CodeDynamicConfig.getProtocolVersion());
            head.setType("options");
            protocol.setMessageHead(head);
            body = new NormalProtocol.MessageBody();
            if (CodeDynamicConfig.AllowedTransferProtocol) {
                body.setMessage("Enable");
            }
            else
            {
                body.setMessage("Disabled");
            }
            body.setFileLong(0);
            protocol.setMessageBody(body);
            writer.write(aes.encryptBase64(gson.toJson(protocol)));
            writer.newLine();
            writer.flush();

            logger.info("------提示------");
            logger.info("由于密码加密逻辑变更");
            logger.info("如您仍使用旧密码");
            logger.info("请尽快以兼容模式登录");
            logger.info("来进一步保护您的安全");
            logger.info("输入1来进行兼容模式登录");
            logger.info("输入其他，来进行普通登录");
            Scanner scanner = new Scanner(System.in);
            try {
                int UserInput = Integer.parseInt(scanner.nextLine());
                if (UserInput == 1)
                {
                    logger.info("请输入用户名：");
                    String UserName = scanner.nextLine();
                    logger.info("请输入密码：");
                    String Password = scanner.nextLine();
                    if (tryLogin(client,aes, UserName,Password))
                    {
                        protocol = new NormalProtocol();
                        head = new NormalProtocol.MessageHead();
                        head.setVersion(CodeDynamicConfig.getProtocolVersion());
                        head.setType("ChangePassword");
                        protocol.setMessageHead(head);
                        body = new NormalProtocol.MessageBody();
                        body.setMessage(SecureUtil.sha256(Password));
                        protocol.setMessageBody(body);
                        json = gson.toJson(protocol);
                        json = aes.encryptBase64(json);
                        writer.write(json);
                        writer.newLine();
                        writer.flush();
                        StartRecvMessageThread(client,aes,logger);
                        SendMessage(logger,client,aes);
                    }
                    else
                    {
                        logger.info("登录失败，用户名或密码错误");
                    }
                    return;
                }
            } catch (NumberFormatException ignored) {}
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
                if (protocol.getMessageHead().getVersion() != CodeDynamicConfig.getProtocolVersion())
                {
                    return;
                }
                if ("Chat".equals(protocol.getMessageHead().getType()))
                {
                    logger.ChatMsg(protocol.getMessageBody().getMessage());
                    do {
                        json = reader.readLine();
                    } while (json == null);
                    json = unicodeToString(json);
                    json = aes.decryptStr(json);
                    protocol = getClient().protocolRequest(json);
                }
                if (!("Login".equals(protocol.getMessageHead().getType())))
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
                        logger.info("------提示------");
                        logger.info("由于密码加密逻辑变更");
                        logger.info("如您仍使用旧密码");
                        logger.info("却使用标准模式登录");
                        logger.info("同样会发生此问题");
                        logger.info("如您是此原因");
                        logger.info("您可使用兼容模式登录一次来解决此问题");
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
                    logger.info("------提示------");
                    logger.info("由于密码加密逻辑变更");
                    logger.info("如您仍使用旧密码");
                    logger.info("却使用标准模式登录");
                    logger.info("同样会发生此问题");
                    logger.info("如您是此原因");
                    logger.info("您可使用兼容模式登录一次来解决此问题");
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
    }

    private void SendMessage(Logger logger, Socket socket,AES aes) {
        Scanner scanner = new Scanner(System.in);
        try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()))) {
            while (true) {
                String UserInput = scanner.nextLine();
                if (needConsoleInput)
                {
                    needConsoleInput = false;
                    ConsoleInput = UserInput;
                    synchronized (ConsoleInputLock)
                    {
                        ConsoleInputLock.notifyAll();
                    }
                    continue;
                }
                if (".help".equals(UserInput)) {
                    logger.info("客户端命令系统");
                    logger.info(".help 查询帮助信息");
                    logger.info(".secure-tell 安全私聊");
                    logger.info(".quit 离开服务器并退出程序");
                    if (CodeDynamicConfig.About_System) {
                        logger.info(".about 查看程序帮助");
                    }
                    continue;
                }
                String command;
                String[] argv;
                {
                    String[] CommandLineFormated = UserInput.split("\\s+"); //分割一个或者多个空格
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
                if (".secure-tell".equals(command))
                {
                    if (argv.length == 2)
                    {
                        endToEndEncryptionData = argv[1];
                        Gson gson = new Gson();
                        NormalProtocol protocol = new NormalProtocol();
                        NormalProtocol.MessageHead head = new NormalProtocol.MessageHead();
                        head.setVersion(CodeDynamicConfig.getProtocolVersion());
                        head.setType("NextIsTransferProtocol");
                        protocol.setMessageHead(head);
                        writer.write(aes.encryptBase64(gson.toJson(protocol)));
                        writer.newLine();
                        writer.flush();

                        TransferProtocol transferProtocol = new TransferProtocol();
                        TransferProtocol.TransferProtocolHeadBean transferProtocolHead = new TransferProtocol.TransferProtocolHeadBean();
                        transferProtocolHead.setTargetUserName(argv[0]);
                        transferProtocolHead.setVersion(CodeDynamicConfig.getProtocolVersion());
                        transferProtocolHead.setType("first");
                        transferProtocol.setTransferProtocolHead(transferProtocolHead);
                        TransferProtocol.TransferProtocolBodyBean transferProtocolBody = new TransferProtocol.TransferProtocolBodyBean();
                        transferProtocolBody.setData(FileUtils.readFileToString(new File("ClientPublicKey.txt"),StandardCharsets.UTF_8));
                        transferProtocol.setTransferProtocolBody(transferProtocolBody);

                        writer.write(aes.encryptBase64(gson.toJson(transferProtocol)));
                        writer.newLine();
                        writer.flush();
                    }
                    else
                    {
                        logger.info("不符合命令语法！");
                        logger.info("此命令的语法为：.secure-tell <用户名> <消息>");
                    }
                    continue;
                }
                if (CodeDynamicConfig.About_System) {
                    if (".about".equals(UserInput)) {
                        logger.info("JavaIM是根据GNU General Public License v3.0开源的自由程序（开源软件)");
                        logger.info("主仓库位于：https://github.com/JavaIM/JavaIM");
                        logger.info("主要开发者名单：");
                        logger.info("QiLechan（柒楽)");
                        logger.info("AlexLiuDev233 （阿白)");
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
                if (".crash".equals(UserInput))
                {
                    if (CodeDynamicConfig.GetDebugMode()) {
                        throw new RuntimeException("Debug Crash");
                    }
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
        logger.info("登录成功！");
        new Thread()
        {
            @Override
            public void run() {
                List<String> ThisSessionForbiddenUserNameList = new ArrayList<>();
                this.setName("RecvMessage Thread");
                this.setUncaughtExceptionHandler(new CrashReport());
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(client.getInputStream(), StandardCharsets.UTF_8))) {
                    String ChatMsg;
                    while (true) {
                        do {
                            ChatMsg = reader.readLine();
                        } while (ChatMsg == null);
                        ChatMsg = unicodeToString(ChatMsg);
                        ChatMsg = aes.decryptStr(ChatMsg);
                        NormalProtocol protocol = getClient().protocolRequest(ChatMsg);
                        if (protocol.getMessageHead().getVersion() != CodeDynamicConfig.getProtocolVersion())
                        {
                            return;
                        }
                        else if ("NextIsTransferProtocol".equals(protocol.getMessageHead().getType()))
                        {
                            String UserNameOfSender = protocol.getMessageBody().getMessage();
                            String json;
                            do {
                                json = reader.readLine();
                            } while (json == null);
                            json = unicodeToString(json);
                            json = aes.decryptStr(json);
                            TransferProtocol transferProtocol = new Gson().fromJson(json, TransferProtocol.class);
                            if (ThisSessionForbiddenUserNameList.contains(UserNameOfSender))
                            {
                                logger.info("用户："+UserNameOfSender+" 试图为您发送端到端安全通信");
                                logger.info("但是由于您之前已不信任他的公钥");
                                logger.info("在本次程序运行过程中，他的端到端安全通讯均会被忽略");
                                protocol = new NormalProtocol();
                                NormalProtocol.MessageHead head = new NormalProtocol.MessageHead();
                                head.setVersion(CodeDynamicConfig.getProtocolVersion());
                                head.setType("NextIsTransferProtocol");
                                protocol.setMessageHead(head);

                                transferProtocol = new TransferProtocol();
                                TransferProtocol.TransferProtocolHeadBean transferProtocolHead = new TransferProtocol.TransferProtocolHeadBean();
                                transferProtocolHead.setTargetUserName(UserNameOfSender);
                                transferProtocolHead.setVersion(CodeDynamicConfig.getProtocolVersion());
                                transferProtocolHead.setType("reply");
                                transferProtocol.setTransferProtocolHead(transferProtocolHead);
                                TransferProtocol.TransferProtocolBodyBean transferProtocolBody = new TransferProtocol.TransferProtocolBodyBean();
                                transferProtocolBody.setData("Untrusted");
                                transferProtocol.setTransferProtocolBody(transferProtocolBody);
                                BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(client.getOutputStream()));
                                writer.write(aes.encryptBase64(new Gson().toJson(protocol)));
                                writer.newLine();
                                writer.flush();
                                do {
                                    json = reader.readLine();
                                } while (json == null);
                                json = unicodeToString(json);
                                json = aes.decryptStr(json);
                                protocol = getClient().protocolRequest(json);
                                if (protocol.getMessageHead().getVersion() != CodeDynamicConfig.getProtocolVersion() || !("Result".equals(protocol.getMessageBody().getMessage())))
                                    return;
                                if ("TransferProtocolVersionIsNotSupport".equals(protocol.getMessageBody().getMessage()))
                                {
                                    logger.info("协议版本不受到服务端的支持");
                                    continue;
                                }
                                else if ("ThisServerDisallowedTransferProtocol".equals(protocol.getMessageBody().getMessage()))
                                {
                                    logger.info("这个服务器上禁止 Transfer Protocol");
                                    continue;
                                }
                                else if ("ThisUserDisallowedTransferProtocol".equals(protocol.getMessageBody().getMessage()))
                                {
                                    logger.info("目标用户禁止 Transfer Protocol");
                                    continue;
                                }
                                else if ("ThisUserNotFound".equals(protocol.getMessageBody().getMessage()))
                                {
                                    logger.info("找不到目标用户");
                                    continue;
                                }
                                writer.write(aes.encryptBase64(new Gson().toJson(transferProtocol)));
                                writer.newLine();
                                writer.flush();
                            }
                            if ("first".equals(transferProtocol.getTransferProtocolHead().getType())) {
                                //说明是被接收方
                                String CounterpartClientPublicKey = transferProtocol.getTransferProtocolBody().getData();
                                if (!(new File("./end-to-end_encryption_saved").exists())) {
                                    Files.createDirectory(Paths.get("./end-to-end_encryption_saved"));
                                }
                                if (!(new File("./end-to-end_encryption_saved").isDirectory())) {
                                    Files.delete(Paths.get("./end-to-end_encryption_saved"));
                                    Files.createDirectory(Paths.get("./end-to-end_encryption_saved"));
                                }
                                boolean Trust = false;
                                if (new File("./end-to-end_encryption_saved/client-" + Address + "-" + protocol.getMessageBody().getMessage()
                                ).exists() && new File("./end-to-end_encryption_saved/client-" + Address + "-" + protocol.getMessageBody().getMessage()
                                ).isFile()) {
                                    if (!(FileUtils.readFileToString(new File("./end-to-end_encryption_saved/client-"
                                            + Address + "-" + UserNameOfSender), StandardCharsets.UTF_8).equals(CounterpartClientPublicKey))) {
                                        logger.warning("用户：" + UserNameOfSender + "试图发送端到端安全通讯");
                                        logger.warning("但是他的公钥已发生变更");
                                        logger.warning("为了您的通讯安全，程序已阻止与他进行聊天");
                                        protocol = new NormalProtocol();
                                        NormalProtocol.MessageHead head = new NormalProtocol.MessageHead();
                                        head.setVersion(CodeDynamicConfig.getProtocolVersion());
                                        head.setType("NextIsTransferProtocol");
                                        protocol.setMessageHead(head);

                                        transferProtocol = new TransferProtocol();
                                        TransferProtocol.TransferProtocolHeadBean transferProtocolHead = new TransferProtocol.TransferProtocolHeadBean();
                                        transferProtocolHead.setTargetUserName(UserNameOfSender);
                                        transferProtocolHead.setVersion(CodeDynamicConfig.getProtocolVersion());
                                        transferProtocolHead.setType("reply");
                                        transferProtocol.setTransferProtocolHead(transferProtocolHead);
                                        TransferProtocol.TransferProtocolBodyBean transferProtocolBody = new TransferProtocol.TransferProtocolBodyBean();
                                        transferProtocolBody.setData("Untrusted");
                                        transferProtocol.setTransferProtocolBody(transferProtocolBody);
                                        BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(client.getOutputStream()));
                                        writer.write(aes.encryptBase64(new Gson().toJson(protocol)));
                                        writer.newLine();
                                        writer.flush();
                                        do {
                                            json = reader.readLine();
                                        } while (json == null);
                                        json = unicodeToString(json);
                                        json = aes.decryptStr(json);
                                        protocol = getClient().protocolRequest(json);
                                        if (protocol.getMessageHead().getVersion() != CodeDynamicConfig.getProtocolVersion() || !("Result".equals(protocol.getMessageBody().getMessage())))
                                            return;
                                        if ("TransferProtocolVersionIsNotSupport".equals(protocol.getMessageBody().getMessage())) {
                                            logger.info("协议版本不受到服务端的支持");
                                            continue;
                                        } else if ("ThisServerDisallowedTransferProtocol".equals(protocol.getMessageBody().getMessage())) {
                                            logger.info("这个服务器上禁止 Transfer Protocol");
                                            continue;
                                        } else if ("ThisUserDisallowedTransferProtocol".equals(protocol.getMessageBody().getMessage())) {
                                            logger.info("目标用户禁止 Transfer Protocol");
                                            continue;
                                        } else if ("ThisUserNotFound".equals(protocol.getMessageBody().getMessage())) {
                                            logger.info("找不到目标用户");
                                            continue;
                                        }
                                        writer.write(aes.encryptBase64(new Gson().toJson(transferProtocol)));
                                        writer.newLine();
                                        writer.flush();
                                        continue;
                                    } else
                                        Trust = true;
                                }
                                if (!Trust) {
                                    logger.info("用户：" + protocol.getMessageBody().getMessage() + " 试图为您发送端到端安全通讯");
                                    logger.info("但是他是第一次和您聊天");
                                    logger.info("是否要信任他的公钥");
                                    logger.info("对等机客户端公钥：" + CounterpartClientPublicKey);
                                    logger.info("输入1信任，输入其他为不信任");
                                    needConsoleInput = true;
                                    synchronized (ConsoleInputLock) {
                                        try {
                                            ConsoleInputLock.wait();//这里其实会导致本线程长时间堵塞
                                        } catch (InterruptedException e) {
                                            logger.info("接收信息线程被中断");
                                            return;
                                        }
                                    }
                                    if ("1".equals(ConsoleInput)) {
                                        protocol = new NormalProtocol();
                                        NormalProtocol.MessageHead head = new NormalProtocol.MessageHead();
                                        head.setVersion(CodeDynamicConfig.getProtocolVersion());
                                        head.setType("NextIsTransferProtocol");
                                        protocol.setMessageHead(head);

                                        transferProtocol = new TransferProtocol();
                                        TransferProtocol.TransferProtocolHeadBean transferProtocolHead = new TransferProtocol.TransferProtocolHeadBean();
                                        transferProtocolHead.setTargetUserName(UserNameOfSender);
                                        transferProtocolHead.setVersion(CodeDynamicConfig.getProtocolVersion());
                                        transferProtocolHead.setType("reply");
                                        transferProtocol.setTransferProtocolHead(transferProtocolHead);
                                        TransferProtocol.TransferProtocolBodyBean transferProtocolBody = new TransferProtocol.TransferProtocolBodyBean();
                                        transferProtocolBody.setData("trust");
                                        transferProtocol.setTransferProtocolBody(transferProtocolBody);
                                        try {
                                            FileUtils.writeStringToFile(new File("./end-to-end_encryption_saved/client-" + Address + "-" + UserNameOfSender), CounterpartClientPublicKey, StandardCharsets.UTF_8);
                                            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(client.getOutputStream()));
                                            writer.write(aes.encryptBase64(new Gson().toJson(protocol)));
                                            writer.newLine();
                                            writer.flush();
                                            writer.write(aes.encryptBase64(new Gson().toJson(transferProtocol)));
                                            writer.newLine();
                                            writer.flush();
                                        } catch (IOException e) {
                                            e.printStackTrace();
                                        }
                                    }
                                    else {
                                        logger.info("已断开与此客户端的通信");
                                        logger.info("并且已自动拉黑");
                                        ThisSessionForbiddenUserNameList.add(UserNameOfSender);
                                        protocol = new NormalProtocol();
                                        NormalProtocol.MessageHead head = new NormalProtocol.MessageHead();
                                        head.setVersion(CodeDynamicConfig.getProtocolVersion());
                                        head.setType("NextIsTransferProtocol");
                                        protocol.setMessageHead(head);

                                        transferProtocol = new TransferProtocol();
                                        TransferProtocol.TransferProtocolHeadBean transferProtocolHead = new TransferProtocol.TransferProtocolHeadBean();
                                        transferProtocolHead.setTargetUserName(UserNameOfSender);
                                        transferProtocolHead.setVersion(CodeDynamicConfig.getProtocolVersion());
                                        transferProtocolHead.setType("reply");
                                        transferProtocol.setTransferProtocolHead(transferProtocolHead);
                                        TransferProtocol.TransferProtocolBodyBean transferProtocolBody = new TransferProtocol.TransferProtocolBodyBean();
                                        transferProtocolBody.setData("Untrusted");
                                        transferProtocol.setTransferProtocolBody(transferProtocolBody);
                                        BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(client.getOutputStream()));
                                        writer.write(aes.encryptBase64(new Gson().toJson(protocol)));
                                        writer.newLine();
                                        writer.flush();
                                        do {
                                            json = reader.readLine();
                                        } while (json == null);
                                        json = unicodeToString(json);
                                        json = aes.decryptStr(json);
                                        protocol = getClient().protocolRequest(json);
                                        if (protocol.getMessageHead().getVersion() != CodeDynamicConfig.getProtocolVersion() || !("Result".equals(protocol.getMessageBody().getMessage())))
                                            return;
                                        if ("TransferProtocolVersionIsNotSupport".equals(protocol.getMessageBody().getMessage())) {
                                            logger.info("协议版本不受到服务端的支持");
                                            continue;
                                        } else if ("ThisServerDisallowedTransferProtocol".equals(protocol.getMessageBody().getMessage())) {
                                            logger.info("这个服务器上禁止 Transfer Protocol");
                                            continue;
                                        } else if ("ThisUserDisallowedTransferProtocol".equals(protocol.getMessageBody().getMessage())) {
                                            logger.info("目标用户禁止 Transfer Protocol");
                                            continue;
                                        } else if ("ThisUserNotFound".equals(protocol.getMessageBody().getMessage())) {
                                            logger.info("找不到目标用户");
                                            continue;
                                        }
                                        writer.write(aes.encryptBase64(new Gson().toJson(transferProtocol)));
                                        writer.newLine();
                                        writer.flush();
                                        continue;
                                    }
                                }
                                //此时都是受信任的
                                protocol = new NormalProtocol();
                                NormalProtocol.MessageHead head = new NormalProtocol.MessageHead();
                                head.setVersion(CodeDynamicConfig.getProtocolVersion());
                                head.setType("NextIsTransferProtocol");
                                protocol.setMessageHead(head);
                                //发送公钥
                                transferProtocol = new TransferProtocol();
                                TransferProtocol.TransferProtocolHeadBean transferProtocolHead = new TransferProtocol.TransferProtocolHeadBean();
                                transferProtocolHead.setTargetUserName(UserNameOfSender);
                                transferProtocolHead.setVersion(CodeDynamicConfig.getProtocolVersion());
                                transferProtocolHead.setType("Encryption");
                                transferProtocol.setTransferProtocolHead(transferProtocolHead);
                                TransferProtocol.TransferProtocolBodyBean transferProtocolBody = new TransferProtocol.TransferProtocolBodyBean();
                                transferProtocolBody.setData(RSA.encrypt(FileUtils.readFileToString(new File("ClientPublicKey.txt"), StandardCharsets.UTF_8), CounterpartClientPublicKey));
                                transferProtocol.setTransferProtocolBody(transferProtocolBody);
                                BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(client.getOutputStream()));
                                writer.write(aes.encryptBase64(new Gson().toJson(protocol)));
                                writer.newLine();
                                writer.flush();
                                writer.write(aes.encryptBase64(new Gson().toJson(transferProtocol)));
                                writer.newLine();
                                writer.flush();
                                boolean tmp;
                                do {
                                    tmp = false;
                                    do {
                                        json = reader.readLine();
                                    } while (json == null);
                                    json = unicodeToString(json);
                                    json = aes.decryptStr(json);
                                    protocol = getClient().protocolRequest(json);
                                    if (protocol.getMessageHead().getVersion() != CodeDynamicConfig.getProtocolVersion()) {
                                        return;
                                    }
                                    else if ("NextIsTransferProtocol".equals(protocol.getMessageHead().getType())) {
                                        do {
                                            json = reader.readLine();
                                        } while (json == null);
                                        json = unicodeToString(json);
                                        json = aes.decryptStr(json);
                                        transferProtocol = new Gson().fromJson(json, TransferProtocol.class);
                                        TransferProtocol finalTransferProtocol = transferProtocol;
                                        NormalProtocol finalProtocol = protocol;
                                        if ("reply".equals(transferProtocol.getTransferProtocolHead().getType()))
                                        {
                                            if ("Untrusted".equals(transferProtocol.getTransferProtocolBody().getData()))
                                            {
                                                logger.ChatMsg("试图向您发送公钥的用户： "+protocol.getMessageBody().getMessage()+" 不信任你的RSA公钥");
                                                logger.ChatMsg("信息接收失败");
                                                break;
                                            }
                                            tmp = true;
                                            continue;
                                        }
                                        new Thread() {
                                            @Override
                                            public void run() {
                                                this.setName("end-to-end encryption Thread");
                                                try {
                                                    logger.info("[端到端安全通讯] [" + finalProtocol.getMessageBody().getMessage() + "] "
                                                            + RSA.decrypt(finalTransferProtocol.getTransferProtocolBody().getData(),
                                                            FileUtils.readFileToString(new File("ClientPrivateKey.txt"),
                                                                    StandardCharsets.UTF_8)));
                                                } catch (IOException e) {
                                                    e.printStackTrace();
                                                }
                                            }
                                        }.start();
                                    }
                                    else if ("Result".equals(protocol.getMessageHead().getType())) {
                                        if ("TransferProtocolVersionIsNotSupport".equals(protocol.getMessageBody().getMessage())) {
                                            logger.info("协议版本不受到服务端的支持");
                                        } else if ("ThisServerDisallowedTransferProtocol".equals(protocol.getMessageBody().getMessage())) {
                                            logger.info("这个服务器上禁止 Transfer Protocol");
                                        } else if ("ThisUserDisallowedTransferProtocol".equals(protocol.getMessageBody().getMessage())) {
                                            logger.info("目标用户禁止 Transfer Protocol");
                                        } else if ("ThisUserNotFound".equals(protocol.getMessageBody().getMessage())) {
                                            logger.info("找不到目标用户");
                                        }
                                        tmp = true;
                                    }
                                    else if (!("Chat".equals(protocol.getMessageHead().getType()))) {
                                        return;
                                    }
                                    else {
                                        logger.ChatMsg(protocol.getMessageBody().getMessage());
                                        tmp = true;
                                    }
                                } while (tmp);
                                continue;
                            }
                            else if ("reply".equals(transferProtocol.getTransferProtocolHead().getType()))
                            {
                                //说明是发送方
                                if ("Untrusted".equals(transferProtocol.getTransferProtocolBody().getData()))
                                {
                                    logger.ChatMsg("您试图与 "+protocol.getMessageBody().getMessage()+" 发送端到端安全通讯");
                                    logger.ChatMsg("但是对方不信任您的RSA公钥");
                                    logger.ChatMsg("无法发送信息");
                                }
                                continue;
                            }
                            else if ("Encryption".equals(transferProtocol.getTransferProtocolHead().getType()))
                            {
                                TransferProtocol finalTransferProtocol1 = transferProtocol;
                                NormalProtocol finalProtocol1 = protocol;
                                new Thread()
                                {
                                    @Override
                                    public void run() {
                                        this.setName("end-to-end encryption Thread");
                                        try {
                                            String key = RSA.decrypt(finalTransferProtocol1.getTransferProtocolBody().getData(),FileUtils.readFileToString(new File("ClientPrivateKey.txt"),StandardCharsets.UTF_8));
                                            String UserNameOfSender = finalProtocol1.getMessageBody().getMessage();
                                            //检查这个key是否被信任
                                            if (!(new File("./end-to-end_encryption_saved").exists())) {
                                                Files.createDirectory(Paths.get("./end-to-end_encryption_saved"));
                                            }
                                            if (!(new File("./end-to-end_encryption_saved").isDirectory())) {
                                                Files.delete(Paths.get("./end-to-end_encryption_saved"));
                                                Files.createDirectory(Paths.get("./end-to-end_encryption_saved"));
                                            }
                                            boolean Trust = false;
                                            if (new File("./end-to-end_encryption_saved/client-" + Address + "-" + UserNameOfSender
                                            ).exists() && new File("./end-to-end_encryption_saved/client-" + Address + "-" + UserNameOfSender
                                            ).isFile()) {
                                                if (!(FileUtils.readFileToString(new File("./end-to-end_encryption_saved/client-"
                                                        + Address + "-" + UserNameOfSender), StandardCharsets.UTF_8).equals(key))) {
                                                    logger.warning("您正在与用户：" + UserNameOfSender + "发送端到端安全通讯");
                                                    logger.warning("但是他的公钥已发生变更");
                                                    logger.warning("为了您的通讯安全，程序已阻止与他进行聊天");
                                                    NormalProtocol protocol = new NormalProtocol();
                                                    NormalProtocol.MessageHead head = new NormalProtocol.MessageHead();
                                                    head.setVersion(CodeDynamicConfig.getProtocolVersion());
                                                    head.setType("NextIsTransferProtocol");
                                                    protocol.setMessageHead(head);

                                                    TransferProtocol transferProtocol = new TransferProtocol();
                                                    TransferProtocol.TransferProtocolHeadBean transferProtocolHead = new TransferProtocol.TransferProtocolHeadBean();
                                                    transferProtocolHead.setTargetUserName(UserNameOfSender);
                                                    transferProtocolHead.setVersion(CodeDynamicConfig.getProtocolVersion());
                                                    transferProtocolHead.setType("reply");
                                                    transferProtocol.setTransferProtocolHead(transferProtocolHead);
                                                    TransferProtocol.TransferProtocolBodyBean transferProtocolBody = new TransferProtocol.TransferProtocolBodyBean();
                                                    transferProtocolBody.setData("Untrusted");
                                                    transferProtocol.setTransferProtocolBody(transferProtocolBody);
                                                    BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(client.getOutputStream()));
                                                    writer.write(aes.encryptBase64(new Gson().toJson(protocol)));
                                                    writer.newLine();
                                                    writer.flush();
                                                    String json;
                                                    do {
                                                        json = reader.readLine();
                                                    } while (json == null);
                                                    json = unicodeToString(json);
                                                    json = aes.decryptStr(json);
                                                    protocol = getClient().protocolRequest(json);
                                                    if (protocol.getMessageHead().getVersion() != CodeDynamicConfig.getProtocolVersion() || !("Result".equals(protocol.getMessageBody().getMessage())))
                                                        return;
                                                    if ("TransferProtocolVersionIsNotSupport".equals(protocol.getMessageBody().getMessage())) {
                                                        logger.info("协议版本不受到服务端的支持");
                                                        return;
                                                    } else if ("ThisServerDisallowedTransferProtocol".equals(protocol.getMessageBody().getMessage())) {
                                                        logger.info("这个服务器上禁止 Transfer Protocol");
                                                        return;
                                                    } else if ("ThisUserDisallowedTransferProtocol".equals(protocol.getMessageBody().getMessage())) {
                                                        logger.info("目标用户禁止 Transfer Protocol");
                                                        return;
                                                    } else if ("ThisUserNotFound".equals(protocol.getMessageBody().getMessage())) {
                                                        logger.info("找不到目标用户");
                                                        return;
                                                    }
                                                    writer.write(aes.encryptBase64(new Gson().toJson(transferProtocol)));
                                                    writer.newLine();
                                                    writer.flush();
                                                    return;
                                                } else
                                                    Trust = true;
                                            }
                                            if (!Trust) {
                                                logger.info("您正在与用户：" + UserNameOfSender + " 发送端到端安全通讯");
                                                logger.info("但是您是第一次和他聊天");
                                                logger.info("是否要信任他的公钥");
                                                logger.info("对等机客户端公钥：" + key);
                                                logger.info("输入1信任，输入其他为不信任");
                                                needConsoleInput = true;
                                                synchronized (ConsoleInputLock) {
                                                    try {
                                                        ConsoleInputLock.wait();//这里其实会导致本线程长时间堵塞
                                                    } catch (InterruptedException e) {
                                                        logger.info("接收信息线程被中断");
                                                        return;
                                                    }
                                                }
                                                if ("1".equals(ConsoleInput)) {
                                                    NormalProtocol protocol = new NormalProtocol();
                                                    NormalProtocol.MessageHead head = new NormalProtocol.MessageHead();
                                                    head.setVersion(CodeDynamicConfig.getProtocolVersion());
                                                    head.setType("NextIsTransferProtocol");
                                                    protocol.setMessageHead(head);

                                                    TransferProtocol transferProtocol = new TransferProtocol();
                                                    TransferProtocol.TransferProtocolHeadBean transferProtocolHead = new TransferProtocol.TransferProtocolHeadBean();
                                                    transferProtocolHead.setTargetUserName(UserNameOfSender);
                                                    transferProtocolHead.setVersion(CodeDynamicConfig.getProtocolVersion());
                                                    transferProtocolHead.setType("reply");
                                                    transferProtocol.setTransferProtocolHead(transferProtocolHead);
                                                    TransferProtocol.TransferProtocolBodyBean transferProtocolBody = new TransferProtocol.TransferProtocolBodyBean();
                                                    transferProtocolBody.setData("trust");
                                                    transferProtocol.setTransferProtocolBody(transferProtocolBody);
                                                    try {
                                                        FileUtils.writeStringToFile(new File("./end-to-end_encryption_saved/client-" + Address + "-" + UserNameOfSender), key, StandardCharsets.UTF_8);
                                                        BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(client.getOutputStream()));
                                                        writer.write(aes.encryptBase64(new Gson().toJson(protocol)));
                                                        writer.newLine();
                                                        writer.flush();
                                                        writer.write(aes.encryptBase64(new Gson().toJson(transferProtocol)));
                                                        writer.newLine();
                                                        writer.flush();
                                                    } catch (IOException e) {
                                                        e.printStackTrace();
                                                    }
                                                }
                                                else {
                                                    logger.info("已断开与此客户端的通信");
                                                    logger.info("并且已自动拉黑");
                                                    ThisSessionForbiddenUserNameList.add(UserNameOfSender);
                                                    NormalProtocol protocol = new NormalProtocol();
                                                    NormalProtocol.MessageHead head = new NormalProtocol.MessageHead();
                                                    head.setVersion(CodeDynamicConfig.getProtocolVersion());
                                                    head.setType("NextIsTransferProtocol");
                                                    protocol.setMessageHead(head);

                                                    TransferProtocol transferProtocol = new TransferProtocol();
                                                    TransferProtocol.TransferProtocolHeadBean transferProtocolHead = new TransferProtocol.TransferProtocolHeadBean();
                                                    transferProtocolHead.setTargetUserName(UserNameOfSender);
                                                    transferProtocolHead.setVersion(CodeDynamicConfig.getProtocolVersion());
                                                    transferProtocolHead.setType("reply");
                                                    transferProtocol.setTransferProtocolHead(transferProtocolHead);
                                                    TransferProtocol.TransferProtocolBodyBean transferProtocolBody = new TransferProtocol.TransferProtocolBodyBean();
                                                    transferProtocolBody.setData("Untrusted");
                                                    transferProtocol.setTransferProtocolBody(transferProtocolBody);
                                                    BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(client.getOutputStream()));
                                                    writer.write(aes.encryptBase64(new Gson().toJson(protocol)));
                                                    writer.newLine();
                                                    writer.flush();
                                                    String json;
                                                    do {
                                                        json = reader.readLine();
                                                    } while (json == null);
                                                    json = unicodeToString(json);
                                                    json = aes.decryptStr(json);
                                                    protocol = getClient().protocolRequest(json);
                                                    if (protocol.getMessageHead().getVersion() != CodeDynamicConfig.getProtocolVersion() || !("Result".equals(protocol.getMessageBody().getMessage())))
                                                        return;
                                                    if ("TransferProtocolVersionIsNotSupport".equals(protocol.getMessageBody().getMessage())) {
                                                        logger.info("协议版本不受到服务端的支持");
                                                        return;
                                                    } else if ("ThisServerDisallowedTransferProtocol".equals(protocol.getMessageBody().getMessage())) {
                                                        logger.info("这个服务器上禁止 Transfer Protocol");
                                                        return;
                                                    } else if ("ThisUserDisallowedTransferProtocol".equals(protocol.getMessageBody().getMessage())) {
                                                        logger.info("目标用户禁止 Transfer Protocol");
                                                        return;
                                                    } else if ("ThisUserNotFound".equals(protocol.getMessageBody().getMessage())) {
                                                        logger.info("找不到目标用户");
                                                        return;
                                                    }
                                                    writer.write(aes.encryptBase64(new Gson().toJson(transferProtocol)));
                                                    writer.newLine();
                                                    writer.flush();
                                                    return;
                                                }
                                            }
                                            //正常的加密系统
                                            NormalProtocol protocol = new NormalProtocol();
                                            NormalProtocol.MessageHead head = new NormalProtocol.MessageHead();
                                            head.setVersion(CodeDynamicConfig.getProtocolVersion());
                                            head.setType("NextIsTransferProtocol");
                                            protocol.setMessageHead(head);
                                            protocol.setMessageBody(new NormalProtocol.MessageBody());

                                            TransferProtocol transferProtocol = new TransferProtocol();
                                            TransferProtocol.TransferProtocolHeadBean transferProtocolHead = new TransferProtocol.TransferProtocolHeadBean();
                                            transferProtocolHead.setTargetUserName(UserNameOfSender);
                                            transferProtocolHead.setVersion(CodeDynamicConfig.getProtocolVersion());
                                            transferProtocolHead.setType("Encryption");
                                            transferProtocol.setTransferProtocolHead(transferProtocolHead);
                                            TransferProtocol.TransferProtocolBodyBean transferProtocolBody = new TransferProtocol.TransferProtocolBodyBean();
                                            transferProtocolBody.setData(RSA.encrypt(endToEndEncryptionData,key));
                                            transferProtocol.setTransferProtocolBody(transferProtocolBody);
                                            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(client.getOutputStream()));
                                            writer.write(aes.encryptBase64(new Gson().toJson(protocol)));
                                            writer.newLine();
                                            writer.flush();
                                            writer.write(aes.encryptBase64(new Gson().toJson(transferProtocol)));
                                            writer.newLine();
                                            writer.flush();
                                            logger.ChatMsg("信息已成功发送");
                                        } catch (IOException e) {
                                            SaveStackTrace.saveStackTrace(e);
                                        }
                                    }
                                }.start();

                            }
                            continue;
                        }
                        else if ("Result".equals(protocol.getMessageHead().getType()))
                        {
                            if ("TransferProtocolVersionIsNotSupport".equals(protocol.getMessageBody().getMessage()))
                            {
                                logger.info("协议版本不受到服务端的支持");
                            }
                            else if ("ThisServerDisallowedTransferProtocol".equals(protocol.getMessageBody().getMessage()))
                            {
                                logger.info("这个服务器上禁止 Transfer Protocol");
                            }
                            else if ("ThisUserDisallowedTransferProtocol".equals(protocol.getMessageBody().getMessage()))
                            {
                                logger.info("目标用户禁止 Transfer Protocol");
                            }
                            else if ("ThisUserNotFound".equals(protocol.getMessageBody().getMessage()))
                            {
                                logger.info("找不到目标用户");
                            }
                            continue;
                        }
                        else if (!("Chat".equals(protocol.getMessageHead().getType())))
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
