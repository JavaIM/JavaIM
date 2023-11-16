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
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.yuezhikong.CodeDynamicConfig;
import org.yuezhikong.CrashReport;
import org.yuezhikong.GeneralMethod;
import org.yuezhikong.NetworkManager;
import org.yuezhikong.utils.CustomVar;
import org.yuezhikong.utils.Logger;
import org.yuezhikong.utils.Protocol.LoginProtocol;
import org.yuezhikong.utils.Protocol.NormalProtocol;
import org.yuezhikong.utils.Protocol.TransferProtocol;
import org.yuezhikong.utils.RSA;
import org.yuezhikong.utils.SaveStackTrace;

import javax.crypto.SecretKey;
import java.io.*;
import java.net.SocketException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

@SuppressWarnings("unused")
public class ClientMain extends GeneralMethod {
    protected boolean ClientStatus = false;
    private CustomVar.KeyData keyData;
    protected static ClientMain Instance;
    private String Address;
    protected String endToEndEncryptionData = "";
    protected final Object ConsoleInputLock = new Object();
    protected String ConsoleInput = "";
    protected volatile boolean needConsoleInput = false;

    protected boolean SpecialMode = false;
    private ThreadGroup ClientThreadGroup;
    protected Thread recvMessageThread;
    private NetworkManager.NetworkData clientNetworkData;
    private AES aes;
    private Logger logger;
    protected String QuitReason = "";

    public Logger getLogger() {
        return logger;
    }

    public boolean getClientStopStatus() {
        return ClientStatus;
    }

    protected ThreadGroup getClientThreadGroup() {
        return ClientThreadGroup;
    }

    protected NetworkManager.NetworkData getClientNetworkData()
    {
        return clientNetworkData;
    }

    protected AES getAes() {
        return aes;
    }

    public static ClientMain getClient() {
        return Instance;
    }

    private void RequestRSA(@NotNull String key) throws IOException {

        RSA_KeyAutogenerate("./ClientRSAKey/ClientPublicKey.txt","./ClientRSAKey/ClientPrivateKey.txt",logger);
        keyData = new CustomVar.KeyData();
        cn.hutool.crypto.asymmetric.RSA rsa = new cn.hutool.crypto.asymmetric.RSA(FileUtils.readFileToString(
                new File("./ClientRSAKey/ClientPrivateKey.txt"),
                StandardCharsets.UTF_8),
                FileUtils.readFileToString(new File("./ClientRSAKey/ClientPublicKey.txt"),StandardCharsets.UTF_8));
        keyData.publicKey = rsa.getPublicKey();
        keyData.privateKey = rsa.getPrivateKey();
        keyData.PublicKey = FileUtils.readFileToString(new File("./ClientRSAKey/ClientPublicKey.txt"),StandardCharsets.UTF_8);
        keyData.PrivateKey = FileUtils.readFileToString(new File("./ClientRSAKey/ClientPrivateKey.txt"),StandardCharsets.UTF_8);

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
        head.setType("RSAEncryption");
        protocol.setMessageHead(head);
        NormalProtocol.MessageBody body = new NormalProtocol.MessageBody();
        body.setMessage(EncryptionKey);
        body.setFileLong(0);
        protocol.setMessageBody(body);

        NetworkManager.WriteDataToRemote(clientNetworkData,gson.toJson(protocol));
        //发送完毕，开始测试
        //测试RSA
        protocol = new NormalProtocol();
        head = new NormalProtocol.MessageHead();
        head.setType("Test");
        head.setVersion(CodeDynamicConfig.getProtocolVersion());
        protocol.setMessageHead(head);
        body = new NormalProtocol.MessageBody();
        body.setMessage("你好服务端");
        protocol.setMessageBody(body);
        NetworkManager.WriteDataToRemote(clientNetworkData,RSA.encrypt(gson.toJson(protocol),key));

        String json = NetworkManager.RecvDataFromRemote(clientNetworkData);
        if ("Decryption Error".equals(json))
        {
            logger.error("你的服务端公钥疑似不正确");
            logger.error("服务端返回：Decryption Error");
            logger.error("服务端无法解密");
            logger.error("程序即将退出");
            if (SpecialMode) {
                getClientThreadGroup().interrupt();
                return;
            }
            else
                System.exit(0);
        }
        json = RSA.decrypt(json,keyData.privateKey);
        protocol = getClient().protocolRequest(json);
        if (protocol.getMessageHead().getVersion() != CodeDynamicConfig.getProtocolVersion() || !("Test".equals(protocol.getMessageHead().getType())))
        {
            return;
        }
        logger.info("服务端响应："+protocol.getMessageBody().getMessage());
    }
    @Contract(pure = true)
    protected String[] RequestUserNameAndPassword()
    {
        Scanner scanner = new Scanner(System.in);
        logger.info("请输入用户名：");
        String UserName = scanner.nextLine();
        logger.info("请输入密码：");
        String Password = scanner.nextLine();
        return new String[] { UserName , Password };
    }

    private boolean UseUserNameAndPasswordLogin() throws IOException {
        String[] UserData = RequestUserNameAndPassword();
        if (UserData.length != 2)
            throw new RuntimeException("The RequestUserNameAndPassword Method Returned Data Is Not Support");
        String UserName = UserData[0];//UserData[0]是明文用户名
        String Password = SecureUtil.sha256(UserData[1]);//UserData[1]是明文密码
        return tryLogin(UserName,Password);//尝试登录
    }

    /***
     * LegacyLoginAndUpdateEncryption的附属函数，用于获取是否为旧版登录模式
     * @return true为是，false为否
     */
    @Contract(pure = true)
    protected boolean isLegacyLoginORNormalLogin()
    {
        logger.info("------提示------");
        logger.info("由于密码加密逻辑变更");
        logger.info("如您仍使用旧密码");
        logger.info("请尽快以兼容模式登录");
        logger.info("来进一步保护您的安全");
        logger.info("输入1来进行兼容模式登录");
        logger.info("输入其他，来进行普通登录");
        try {
            needConsoleInput = true;
            synchronized (ConsoleInputLock)
            {
                if (needConsoleInput)
                {
                    ConsoleInputLock.wait();
                }
            }
            int UserInput = Integer.parseInt(ConsoleInput);
            if (UserInput == 1)
            {
                return true;
            }
        } catch (NumberFormatException | InterruptedException ignored) {}
        return false;
    }
    private boolean LegacyLoginAndUpdateEncryption() throws IOException {
        if (isLegacyLoginORNormalLogin())
        {
            String[] UserData = RequestUserNameAndPassword();
            if (UserData.length != 2)
                throw new RuntimeException("The RequestUserNameAndPassword Method Returned Data Is Not Support");
            String UserName = UserData[0];//UserData[0]是明文用户名
            String Password = UserData[1];//UserData[1]是明文密码
            if (tryLogin(UserName,Password))
            {
                NormalProtocol protocol = new NormalProtocol();
                NormalProtocol.MessageHead head = new NormalProtocol.MessageHead();
                head.setVersion(CodeDynamicConfig.getProtocolVersion());
                head.setType("ChangePassword");
                protocol.setMessageHead(head);
                NormalProtocol.MessageBody body = new NormalProtocol.MessageBody();
                body.setMessage(SecureUtil.sha256(Password));
                protocol.setMessageBody(body);
                String json = new Gson().toJson(protocol);
                json = aes.encryptBase64(json);
                NetworkManager.WriteDataToRemote(clientNetworkData,json);
                StartRecvMessageThread();
                SendMessage();
            }
            else
            {
                logger.info("登录失败，用户名或密码错误");
            }
            return true;
        }
        return false;
    }
    private boolean tryLogin(@NotNull String UserName, @NotNull String Password) throws IOException {
        String json = getPasswordSendJson(UserName, Password);
        json = aes.encryptBase64(json);
        NetworkManager.WriteDataToRemote(clientNetworkData,json);

        json = NetworkManager.RecvDataFromRemote(clientNetworkData);
        json = aes.decryptStr(json);
        NormalProtocol protocol = getClient().protocolRequest(json);
        if (protocol.getMessageHead().getVersion() != CodeDynamicConfig.getProtocolVersion()
                || ("Login".equals(protocol.getMessageHead().getType()) && "Fail".equals(protocol.getMessageBody().getMessage())))
            return false;
        if ("Chat".equals(protocol.getMessageHead().getType()))
        {
            logger.info("登录失败，理由："+protocol.getMessageBody().getMessage());
            logger.info("正在重新开启登录过程");
            return UseUserNameAndPasswordLogin();
        }
        if (!("Login".equals(protocol.getMessageHead().getType())))
            return false;
        FileUtils.writeStringToFile(new File("./token.txt"),protocol.getMessageBody().getMessage(),StandardCharsets.UTF_8);
        return true;
    }

    private static String getPasswordSendJson(@NotNull String UserName, @NotNull String Password) {
        Gson gson = new Gson();
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
        return gson.toJson(loginProtocol);
    }

    protected Logger LoggerInit()
    {
        return new Logger(null);
    }
    protected File getServerPublicKeyFile()
    {
        return new File("./ClientRSAKey/ServerPublicKeys/CurrentServerPublicKey.txt");
    }
    public void start(String ServerAddress,int ServerPort)
    {
        logger = LoggerInit();
        Timer timer = new Timer(true);
        logger.info("正在连接主机：" + ServerAddress + " ，端口号：" + ServerPort);
        try (NetworkManager.NetworkData clientNetworkData =
                     NetworkManager.ConnectToTCPServer(ServerAddress, ServerPort)) {
            ClientThreadGroup = Thread.currentThread().getThreadGroup();
            this.clientNetworkData = clientNetworkData;
            Instance = this;
            Address = ServerAddress;
            logger.info("远程主机地址：" + clientNetworkData.getRemoteSocketAddress());
            //测试明文通讯
            NetworkManager.WriteDataToRemote(clientNetworkData,"Hello Server");

            logger.info("服务端响应："+NetworkManager.RecvDataFromRemote(clientNetworkData));

            NetworkManager.WriteDataToRemote(clientNetworkData,"你好，服务端");

            logger.info("服务端响应："+NetworkManager.RecvDataFromRemote(clientNetworkData));
            //初始化心跳包
            getTimerThreadPool().scheduleWithFixedDelay(() -> {
                try {
                    NetworkManager.WriteDataToRemote(clientNetworkData,"Alive");
                } catch (IOException e) {
                    SaveStackTrace.saveStackTrace(e);
                }
            },0,CodeDynamicConfig.HeartbeatInterval,TimeUnit.SECONDS);
            //测试通讯协议
            Gson gson = new Gson();
            NormalProtocol protocol = new NormalProtocol();
            NormalProtocol.MessageHead head = new NormalProtocol.MessageHead();
            head.setVersion(CodeDynamicConfig.getProtocolVersion());
            head.setType("Test");
            protocol.setMessageHead(head);
            NormalProtocol.MessageBody body = new NormalProtocol.MessageBody();
            body.setMessage("你好服务端");
            protocol.setMessageBody(body);
            NetworkManager.WriteDataToRemote(clientNetworkData,gson.toJson(protocol));

            protocol = protocolRequest(NetworkManager.RecvDataFromRemote(clientNetworkData));
            if (protocol.getMessageHead().getVersion() != CodeDynamicConfig.getProtocolVersion() || !("Test".equals(protocol.getMessageHead().getType())))
            {
                return;
            }
            logger.info("服务端响应："+protocol.getMessageBody().getMessage());

            //加密处理
            if (!getServerPublicKeyFile().exists())
            {
                QuitReason = "服务端公钥未被配置";
                return;
            }

            final String ServerPublicKey = FileUtils.readFileToString(getServerPublicKeyFile(), StandardCharsets.UTF_8);
            if (ServerPublicKey.isEmpty())
            {
                QuitReason = "服务端公钥未被配置";
                return;
            }
            logger.info("正在配置RSA加密...");
            RequestRSA(ServerPublicKey);
            //AES制造开始
            logger.info("正在配置AES加密...");
            String RandomForClient = UUID.randomUUID().toString();
            protocol = new NormalProtocol();
            head = new NormalProtocol.MessageHead();
            head.setType("AESEncryption");
            head.setVersion(CodeDynamicConfig.getProtocolVersion());
            protocol.setMessageHead(head);
            body = new NormalProtocol.MessageBody();
            body.setMessage(RandomForClient);
            protocol.setMessageBody(body);
            NetworkManager.WriteDataToRemote(clientNetworkData,RSA.encrypt(gson.toJson(protocol),ServerPublicKey));

            String json = NetworkManager.RecvDataFromRemote(clientNetworkData);
            json = RSA.decrypt(json,keyData.privateKey);
            protocol = getClient().protocolRequest(json);
            if (protocol.getMessageHead().getVersion() != CodeDynamicConfig.getProtocolVersion() || !("AESEncryption".equals(protocol.getMessageHead().getType())))
            {
                return;
            }
            String RandomForServer = protocol.getMessageBody().getMessage();
            SecretKey key = SecureUtil.generateKey(SymmetricAlgorithm.AES.getValue(), Base64.decodeBase64(getClient().GenerateKey(RandomForServer+RandomForClient)));
            final AES aes = cn.hutool.crypto.SecureUtil.aes(key.getEncoded());
            this.aes = aes;
            //开始AES测试
            protocol = new NormalProtocol();
            head = new NormalProtocol.MessageHead();
            head.setVersion(CodeDynamicConfig.getProtocolVersion());
            head.setType("Test");
            protocol.setMessageHead(head);
            body = new NormalProtocol.MessageBody();
            body.setMessage("你好服务端");
            protocol.setMessageBody(body);
            NetworkManager.WriteDataToRemote(clientNetworkData,aes.encryptBase64(gson.toJson(protocol)));

            json = NetworkManager.RecvDataFromRemote(clientNetworkData);
            json = aes.decryptStr(json);
            protocol = getClient().protocolRequest(json);
            if (protocol.getMessageHead().getVersion() != CodeDynamicConfig.getProtocolVersion() || !("Test".equals(protocol.getMessageHead().getType())))
            {
                return;
            }
            logger.info("服务器响应："+protocol.getMessageBody().getMessage());
            //额外的配置项目
            logger.info("正在配置TransferProtocol...");
            protocol = new NormalProtocol();
            head = new NormalProtocol.MessageHead();
            head.setVersion(CodeDynamicConfig.getProtocolVersion());
            head.setType("options");
            protocol.setMessageHead(head);
            body = new NormalProtocol.MessageBody();
            if (CodeDynamicConfig.AllowedTransferProtocol) {
                body.setMessage("AllowedTransferProtocol:Enable");
            }
            else
            {
                body.setMessage("AllowedTransferProtocol:Disabled");
            }
            protocol.setMessageBody(body);
            NetworkManager.WriteDataToRemote(clientNetworkData,aes.encryptBase64(gson.toJson(protocol)));

            json = NetworkManager.RecvDataFromRemote(clientNetworkData);
            json = aes.decryptStr(json);
            protocol = getClient().protocolRequest(json);
            if (protocol.getMessageHead().getVersion() != CodeDynamicConfig.getProtocolVersion()
                    || !("options".equals(protocol.getMessageHead().getType()))
                    || !("Accept".equals(protocol.getMessageBody().getMessage())))
            {
                return;
            }
            logger.info("服务器响应："+protocol.getMessageBody().getMessage());
            //握手完成，接下来是登录逻辑
            if (new File("./token.txt").exists() && new File("./token.txt").isFile() && new File("./token.txt").canRead())
            {
                LoginProtocol loginProtocol = new LoginProtocol();
                LoginProtocol.LoginPacketHeadBean loginPacketHead = new LoginProtocol.LoginPacketHeadBean();
                loginPacketHead.setType("token");
                loginProtocol.setLoginPacketHead(loginPacketHead);
                LoginProtocol.LoginPacketBodyBean loginPacketBody = new LoginProtocol.LoginPacketBodyBean();
                LoginProtocol.LoginPacketBodyBean.ReLoginBean reLogin = new LoginProtocol.LoginPacketBodyBean.ReLoginBean();
                reLogin.setToken(FileUtils.readFileToString(new File("./token.txt"),StandardCharsets.UTF_8));
                loginPacketBody.setReLogin(reLogin);
                loginProtocol.setLoginPacketBody(loginPacketBody);
                json = gson.toJson(loginProtocol);
                json = aes.encryptBase64(json);
                NetworkManager.WriteDataToRemote(clientNetworkData,json);
                try {
                    if (TokenLoginSystem())
                    {
                        SendMessage();
                    }
                } catch (QuitException e) {
                    if (QuitReason == null || QuitReason.isEmpty())
                    {
                        QuitReason = e.getMessage();
                    }
                }
                return;
            }
            else
            {
                if (LegacyLoginAndUpdateEncryption())
                {
                    return;
                }
                if (!(UseUserNameAndPasswordLogin()))
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
                    StartRecvMessageThread();
                }
            }
            SendMessage();
        } catch (IOException e) {
            SaveStackTrace.saveStackTrace(e);
        }
        finally {
            try {
                if (getClientNetworkData() != null)
                    NetworkManager.ShutdownTCPConnection(getClientNetworkData());
            } catch (IOException e) {
                SaveStackTrace.saveStackTrace(e);
            }
            ClientStatus = true;
            ClientThreadGroup.interrupt();
            Instance = null;
            if (QuitReason.isEmpty())
            {
                QuitReason = "客户端没有设置退出原因";
            }
            if (AllowShutdownScheduledExecutorService())
                TimerThreadPool.shutdownNow();
            getLogger().info("程序即将退出");
            getLogger().info("理由：");
            getLogger().info(QuitReason);
            getLogger().OutDate();
        }
    }

    protected ScheduledExecutorService TimerThreadPool;
    protected boolean AllowShutdownScheduledExecutorService()
    {
        return !TimerThreadPool.isShutdown();
    }
    @Contract(pure = true)
    protected synchronized ScheduledExecutorService getTimerThreadPool() {
        if (TimerThreadPool == null)
        {
            TimerThreadPool = Executors.newScheduledThreadPool(1, new ThreadFactory() {
                private final AtomicInteger threadNumber = new AtomicInteger(1);

                @Override
                public Thread newThread(@NotNull Runnable r) {
                    return new Thread(ClientThreadGroup,r, "Timer Thread #" + threadNumber.getAndIncrement());
                }
            });
        }
        return TimerThreadPool;
    }

    protected static class QuitException extends Exception
    {
        public QuitException(String Message)
        {
            super(Message);
        }
    }
    private boolean TokenLoginSystem() throws IOException, QuitException {
        NormalProtocol protocol;
        String json = NetworkManager.RecvDataFromRemote(clientNetworkData);
        json = aes.decryptStr(json);
        protocol = getClient().protocolRequest(json);
        if (protocol.getMessageHead().getVersion() != CodeDynamicConfig.getProtocolVersion())
        {
            return false;
        }
        if ("Chat".equals(protocol.getMessageHead().getType()))//此时的chat消息就是服务端的登录失败原因
        {
            logger.info("自动登录失败，原因："+protocol.getMessageBody().getMessage());
            logger.info("正在重新请求登录...");
            TokenLoginSystem();
            return false;
        }
        if ("Success".equals(protocol.getMessageBody().getMessage()))
        {
            StartRecvMessageThread();
            return true;
        }
        else if ("Fail".equals(protocol.getMessageBody().getMessage()))
        {
            logger.info("Token无效！需重新使用用户名密码登录！");
            if (!(UseUserNameAndPasswordLogin()))
            {
                logger.info("登录失败，用户名或密码错误");
                logger.info("------提示------");
                logger.info("由于密码加密逻辑变更");
                logger.info("如您仍使用旧密码");
                logger.info("却使用标准模式登录");
                logger.info("同样会发生此问题");
                logger.info("如您是此原因");
                logger.info("您可使用兼容模式登录一次来解决此问题");
                throw new QuitException("UserNameOrPasswordFailed");
            }
            else
            {
                StartRecvMessageThread();
                return true;
            }
        }
        else
        {
            QuitReason = "服务器非法响应";
            logger.info("登录失败，非法响应标识："+protocol.getMessageHead().getType()+"，响应信息："+protocol.getMessageBody().getMessage());
            throw new QuitException("Illegal return");
        }
    }

    public String getAddress() {
        return Address;
    }

    /**
     * 客户端指令处理程序
     * @param UserInput 用户输入
     * @return {@code true} 是一条命令 {@code false} 不是一条命令
     * @throws IOException Socket IO出错
     * @throws QuitException 用户的指令是.quit
     */
    @Contract(pure = true)
    protected boolean CommandRequest(String UserInput) throws IOException, QuitException {
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
        switch (command)
        {
            case ".help" -> {
                logger.info("客户端命令系统");
                logger.info(".help 查询帮助信息");
                logger.info(".secure-tell 安全私聊");
                logger.info(".quit 离开服务器并退出程序");
                logger.info(".change-password 修改密码");
                logger.info(".about 查看程序帮助");
                return true;
            }
            case ".secure-tell" -> {
                if (argv.length == 2)
                {
                    endToEndEncryptionData = argv[1];
                    Gson gson = new Gson();
                    NormalProtocol protocol = new NormalProtocol();
                    NormalProtocol.MessageHead head = new NormalProtocol.MessageHead();
                    head.setVersion(CodeDynamicConfig.getProtocolVersion());
                    head.setType("NextIsTransferProtocol");
                    protocol.setMessageHead(head);
                    NetworkManager.WriteDataToRemote(clientNetworkData,aes.encryptBase64(gson.toJson(protocol)));

                    TransferProtocol transferProtocol = new TransferProtocol();
                    TransferProtocol.TransferProtocolHeadBean transferProtocolHead = new TransferProtocol.TransferProtocolHeadBean();
                    transferProtocolHead.setTargetUserName(argv[0]);
                    transferProtocolHead.setVersion(CodeDynamicConfig.getProtocolVersion());
                    transferProtocolHead.setType("first");
                    transferProtocol.setTransferProtocolHead(transferProtocolHead);
                    TransferProtocol.TransferProtocolBodyBean transferProtocolBody = new TransferProtocol.TransferProtocolBodyBean();
                    transferProtocolBody.setData(FileUtils.readFileToString(new File("./ClientRSAKey/ClientPublicKey.txt"),StandardCharsets.UTF_8));
                    transferProtocol.setTransferProtocolBody(transferProtocolBody);

                    NetworkManager.WriteDataToRemote(clientNetworkData,aes.encryptBase64(gson.toJson(transferProtocol)));
                }
                else
                {
                    logger.info("不符合命令语法！");
                    logger.info("此命令的语法为：.secure-tell <用户名> <消息>");
                }
                return true;
            }
            case ".about" -> {
                logger.info("JavaIM是根据GNU General Public License v3.0开源的自由程序（开源软件)");
                logger.info("主仓库位于：https://github.com/JavaIM/JavaIM");
                logger.info("主要开发者名单：");
                logger.info("QiLechan（柒楽)");
                logger.info("AlexLiuDev233 （阿白)");
                return true;
            }
            case ".quit" -> {
                Gson gson = new Gson();
                NormalProtocol protocol = new NormalProtocol();
                NormalProtocol.MessageHead head = new NormalProtocol.MessageHead();
                head.setVersion(CodeDynamicConfig.getProtocolVersion());
                head.setType("Leave");
                protocol.setMessageHead(head);
                NormalProtocol.MessageBody body = new NormalProtocol.MessageBody();
                body.setMessage(UserInput);
                protocol.setMessageBody(body);
                NetworkManager.WriteDataToRemote(clientNetworkData,aes.encryptBase64(gson.toJson(protocol)));
                throw new QuitException("UserRequestQuit");
            }
            case ".crash" -> {
                if (CodeDynamicConfig.GetDebugMode()) {
                    throw new RuntimeException("Debug Crash");
                }
                else
                {
                    return false;
                }
            }
            case ".change-password" -> {
                if (argv.length == 1)
                {
                    NormalProtocol protocol = new NormalProtocol();//开始构造协议
                    NormalProtocol.MessageHead head = new NormalProtocol.MessageHead();
                    head.setVersion(CodeDynamicConfig.getProtocolVersion());
                    head.setType("ChangePassword");//类型是更改密码数据包
                    protocol.setMessageHead(head);
                    NormalProtocol.MessageBody body = new NormalProtocol.MessageBody();
                    body.setMessage(SecureUtil.sha256(argv[0]));//对用户输入的明文密码进行sha256哈希计算
                    protocol.setMessageBody(body);
                    String json = new Gson().toJson(protocol);//生成json
                    json = aes.encryptBase64(json);//aes加密
                    NetworkManager.WriteDataToRemote(clientNetworkData,json);
                }
                else
                {
                    logger.info("不符合命令语法！");
                    logger.info("此命令的语法为：.change-password <新密码>");
                }
                return true;
            }
            default -> {
                return false;
            }
        }

    }
    protected void SendMessage() {
        Scanner scanner = new Scanner(System.in);
        try {
            while (true) {
                String UserInput = scanner.nextLine();
                if (needConsoleInput)
                {
                    synchronized (ConsoleInputLock)
                    {
                        if (needConsoleInput) {
                            needConsoleInput = false;
                            ConsoleInput = UserInput;
                            ConsoleInputLock.notifyAll();
                        }
                    }
                    continue;
                }

                try {
                    if (CommandRequest(UserInput))
                    {
                        continue;
                    }
                } catch (QuitException e) {
                    QuitReason = "用户界面要求关闭";
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
                protocol.setMessageBody(body);
                NetworkManager.WriteDataToRemote(clientNetworkData,aes.encryptBase64(gson.toJson(protocol)));
            }
        } catch (IOException ignored) {}
        if (Thread.currentThread().isInterrupted())
        {
            return;
        }
        System.exit(0);
    }

    //启动RecvMessageThread
    private void StartRecvMessageThread() {
        logger.info("登录成功！");
        new Thread(ClientThreadGroup,"RecvMessageThread")
        {
            @Override
            public void run() {
                List<String> ThisSessionForbiddenUserNameList = new ArrayList<>();
                this.setUncaughtExceptionHandler(CrashReport.getCrashReport());
                recvMessageThread = Thread.currentThread();
                try {
                    String ChatMsg;
                    while (true) {
                        ChatMsg = NetworkManager.RecvDataFromRemote(clientNetworkData);
                        ChatMsg = aes.decryptStr(ChatMsg);
                        NormalProtocol protocol = getClient().protocolRequest(ChatMsg);
                        if (protocol.getMessageHead().getVersion() != CodeDynamicConfig.getProtocolVersion())
                        {
                            return;
                        }
                        else if ("NextIsTransferProtocol".equals(protocol.getMessageHead().getType()))
                        {
                            //端到端加密
                            String UserNameOfSender = protocol.getMessageBody().getMessage();
                            String json;
                            //读取TransferProtocol
                            json = NetworkManager.RecvDataFromRemote(clientNetworkData);
                            json = aes.decryptStr(json);
                            TransferProtocol transferProtocol = new Gson().fromJson(json, TransferProtocol.class);
                            //检测这个用户是否在黑名单，如果在，禁止他的聊天
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
                                NetworkManager.WriteDataToRemote(clientNetworkData,aes.encryptBase64(new Gson().toJson(protocol)));
                                json = NetworkManager.RecvDataFromRemote(clientNetworkData);
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
                                NetworkManager.WriteDataToRemote(clientNetworkData,aes.encryptBase64(new Gson().toJson(transferProtocol)));
                            }
                            Path dir = Paths.get("./end-to-end_encryption_saved");
                            if ("first".equals(transferProtocol.getTransferProtocolHead().getType())) {
                                //说明是被接收方
                                //检测文件夹与文件是否存在
                                String CounterpartClientPublicKey = transferProtocol.getTransferProtocolBody().getData();
                                if (!(new File("./end-to-end_encryption_saved").exists())) {
                                    Files.createDirectory(dir);
                                }
                                if (new File("./end-to-end_encryption_saved").isFile()) {
                                    Files.delete(dir);
                                    Files.createDirectory(dir);
                                }
                                boolean Trust = false;
                                if (new File("./end-to-end_encryption_saved/client-" + Address + "-" + protocol.getMessageBody().getMessage()
                                ).exists() && new File("./end-to-end_encryption_saved/client-" + Address + "-" + protocol.getMessageBody().getMessage()
                                ).isFile()) {
                                    //文件如果存在，直接检测RSA key
                                    //与保存的不一致，断开连接，一致则直接放行
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
                                        NetworkManager.WriteDataToRemote(clientNetworkData,aes.encryptBase64(new Gson().toJson(protocol)));
                                        json = NetworkManager.RecvDataFromRemote(clientNetworkData);
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
                                        NetworkManager.WriteDataToRemote(clientNetworkData,aes.encryptBase64(new Gson().toJson(transferProtocol)));
                                        continue;
                                    } else
                                        Trust = true;
                                }
                                if (!Trust) {
                                    //处理未保存的key的流程
                                    logger.info("用户：" + protocol.getMessageBody().getMessage() + " 试图为您发送端到端安全通讯");
                                    logger.info("但是他是第一次和您聊天");
                                    logger.info("是否要信任他的公钥");
                                    logger.info("对等机客户端公钥：" + CounterpartClientPublicKey);
                                    logger.info("输入1信任，输入其他为不信任");
                                    //等待ConsoleInputLock锁
                                    //等到下一次发生控制台输入时
                                    //会将needConsoleInput改为false,然后notify ConsoleInputLock锁
                                    //然后将ConsoleInput设置为输入
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
                                        //如果用户信任了key，就发送信任包并保存到文件
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
                                            NetworkManager.WriteDataToRemote(clientNetworkData,aes.encryptBase64(new Gson().toJson(protocol)));
                                            NetworkManager.WriteDataToRemote(clientNetworkData,aes.encryptBase64(new Gson().toJson(transferProtocol)));
                                        } catch (IOException e) {
                                            SaveStackTrace.saveStackTrace(e);
                                        }
                                    }
                                    else {
                                        //不信任就断开，然后拉黑客户端
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
                                        NetworkManager.WriteDataToRemote(clientNetworkData,aes.encryptBase64(new Gson().toJson(protocol)));
                                        NetworkManager.WriteDataToRemote(clientNetworkData,aes.encryptBase64(new Gson().toJson(transferProtocol)));
                                        json = NetworkManager.RecvDataFromRemote(clientNetworkData);
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
                                transferProtocolBody.setData(RSA.encrypt(FileUtils.readFileToString(new File("./ClientRSAKey/ClientPublicKey.txt"), StandardCharsets.UTF_8), CounterpartClientPublicKey));
                                transferProtocol.setTransferProtocolBody(transferProtocolBody);
                                NetworkManager.WriteDataToRemote(clientNetworkData,aes.encryptBase64(new Gson().toJson(protocol)));
                                NetworkManager.WriteDataToRemote(clientNetworkData,aes.encryptBase64(new Gson().toJson(transferProtocol)));
                                //等待服务端回传
                                boolean tmp;
                                do {
                                    tmp = false;
                                    json = NetworkManager.RecvDataFromRemote(clientNetworkData);
                                    json = aes.decryptStr(json);
                                    protocol = getClient().protocolRequest(json);
                                    if (protocol.getMessageHead().getVersion() != CodeDynamicConfig.getProtocolVersion()) {
                                        return;
                                    }
                                    else if ("NextIsTransferProtocol".equals(protocol.getMessageHead().getType())) {
                                        json = NetworkManager.RecvDataFromRemote(clientNetworkData);
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
                                        if (!("Encryption".equals(transferProtocol.getTransferProtocolHead().getType())))
                                        {
                                            //如果是其他客户端发信，阻止他的聊天
                                            ThisSessionForbiddenUserNameList.add(UserNameOfSender);
                                            protocol = new NormalProtocol();
                                            head = new NormalProtocol.MessageHead();
                                            head.setVersion(CodeDynamicConfig.getProtocolVersion());
                                            head.setType("NextIsTransferProtocol");
                                            protocol.setMessageHead(head);

                                            transferProtocol = new TransferProtocol();
                                            transferProtocolHead = new TransferProtocol.TransferProtocolHeadBean();
                                            transferProtocolHead.setTargetUserName(UserNameOfSender);
                                            transferProtocolHead.setVersion(CodeDynamicConfig.getProtocolVersion());
                                            transferProtocolHead.setType("reply");
                                            transferProtocol.setTransferProtocolHead(transferProtocolHead);
                                            transferProtocolBody = new TransferProtocol.TransferProtocolBodyBean();
                                            transferProtocolBody.setData("Untrusted");
                                            transferProtocol.setTransferProtocolBody(transferProtocolBody);
                                            NetworkManager.WriteDataToRemote(clientNetworkData,aes.encryptBase64(new Gson().toJson(protocol)));
                                            NetworkManager.WriteDataToRemote(clientNetworkData,aes.encryptBase64(new Gson().toJson(transferProtocol)));
                                            //后续这里要新增服务端私聊，向他发送提示
                                            logger.info(protocol.getMessageBody().getMessage()+"想要进行端到端通讯，但是系统已经在处理一个端到端了");
                                            tmp = true;
                                            continue;
                                        }
                                        new Thread(ClientThreadGroup,"end-to-end encryption Thread") {
                                            @Override
                                            public void run() {
                                                try {
                                                    //logger输出聊天消息
                                                    logger.info("[端到端安全通讯] [" + finalProtocol.getMessageBody().getMessage() + "] "
                                                            + RSA.decrypt(finalTransferProtocol.getTransferProtocolBody().getData(),
                                                            FileUtils.readFileToString(new File("./ClientRSAKey/ClientPrivateKey.txt"),
                                                                    StandardCharsets.UTF_8)));
                                                } catch (IOException e) {
                                                    SaveStackTrace.saveStackTrace(e);
                                                }
                                            }
                                        }.start();
                                    }
                                    else if ("Result".equals(protocol.getMessageHead().getType())) {
                                        //extIsTrasferProtocol的处理
                                        if ("TransferProtocolVersionIsNotSupport".equals(protocol.getMessageBody().getMessage())) {
                                            logger.info("协议版本不受到服务端的支持");
                                        } else if ("ThisServerDisallowedTransferProtocol".equals(protocol.getMessageBody().getMessage())) {
                                            logger.info("这个服务器上禁止 Transfer Protocol");
                                        } else if ("ThisUserDisallowedTransferProtocol".equals(protocol.getMessageBody().getMessage())) {
                                            logger.info("目标用户禁止 Transfer Protocol");
                                        } else if ("ThisUserNotFound".equals(protocol.getMessageBody().getMessage())) {
                                            logger.info("找不到目标用户");
                                        }
                                        break;
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
                                //发送方前半段处理
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
                                //发送方的后半段处理
                                TransferProtocol finalTransferProtocol1 = transferProtocol;
                                NormalProtocol finalProtocol1 = protocol;
                                new Thread(ClientThreadGroup,"end-to-end encryption Thread")
                                {
                                    @Override
                                    public void run() {
                                        try {
                                            //获取接收方回传的公钥
                                            String key = RSA.decrypt(finalTransferProtocol1.getTransferProtocolBody().getData(),FileUtils.readFileToString(new File("./ClientRSAKey/ClientPrivateKey.txt"),StandardCharsets.UTF_8));
                                            String UserNameOfSender = finalProtocol1.getMessageBody().getMessage();
                                            //检查这个key是否被信任
                                            //此段代码复制自接收端
                                            if (!(new File("./end-to-end_encryption_saved").exists())) {
                                                Files.createDirectory(dir);
                                            }
                                            if (!(new File("./end-to-end_encryption_saved").isDirectory())) {
                                                Files.delete(dir);
                                                Files.createDirectory(dir);
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

                                                    TransferProtocol transferProtocol = getTransferProtocol(UserNameOfSender, "reply", "Untrusted");
                                                    NetworkManager.WriteDataToRemote(clientNetworkData,aes.encryptBase64(new Gson().toJson(protocol)));
                                                    NetworkManager.WriteDataToRemote(clientNetworkData,aes.encryptBase64(new Gson().toJson(transferProtocol)));
                                                    String json = NetworkManager.RecvDataFromRemote(clientNetworkData);
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

                                                    TransferProtocol transferProtocol = getTransferProtocol(UserNameOfSender, "reply", "trust");
                                                    try {
                                                        FileUtils.writeStringToFile(new File("./end-to-end_encryption_saved/client-" + Address + "-" + UserNameOfSender), key, StandardCharsets.UTF_8);
                                                        NetworkManager.WriteDataToRemote(clientNetworkData,aes.encryptBase64(new Gson().toJson(protocol)));
                                                        NetworkManager.WriteDataToRemote(clientNetworkData,aes.encryptBase64(new Gson().toJson(transferProtocol)));
                                                    } catch (IOException e) {
                                                        SaveStackTrace.saveStackTrace(e);
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

                                                    TransferProtocol transferProtocol = getTransferProtocol(UserNameOfSender, "reply", "Untrusted");
                                                    NetworkManager.WriteDataToRemote(clientNetworkData,aes.encryptBase64(new Gson().toJson(protocol)));
                                                    NetworkManager.WriteDataToRemote(clientNetworkData,aes.encryptBase64(new Gson().toJson(transferProtocol)));
                                                    String json;
                                                    json = NetworkManager.RecvDataFromRemote(clientNetworkData);
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
                                                    return;
                                                }
                                            }
                                            //正常的加密与发送系统
                                            NormalProtocol protocol = new NormalProtocol();
                                            NormalProtocol.MessageHead head = new NormalProtocol.MessageHead();
                                            head.setVersion(CodeDynamicConfig.getProtocolVersion());
                                            head.setType("NextIsTransferProtocol");
                                            protocol.setMessageHead(head);
                                            protocol.setMessageBody(new NormalProtocol.MessageBody());

                                            TransferProtocol transferProtocol = getTransferProtocol(UserNameOfSender, "Encryption", RSA.encrypt(endToEndEncryptionData, key));
                                            NetworkManager.WriteDataToRemote(clientNetworkData,aes.encryptBase64(new Gson().toJson(protocol)));
                                            NetworkManager.WriteDataToRemote(clientNetworkData,aes.encryptBase64(new Gson().toJson(transferProtocol)));
                                            logger.ChatMsg("信息已成功发送");
                                        } catch (IOException e) {
                                            SaveStackTrace.saveStackTrace(e);
                                        }
                                    }

                                    @NotNull
                                    private static TransferProtocol getTransferProtocol(String UserNameOfSender, String HeadType, String BodyData) {
                                        TransferProtocol transferProtocol = new TransferProtocol();
                                        TransferProtocol.TransferProtocolHeadBean transferProtocolHead = new TransferProtocol.TransferProtocolHeadBean();
                                        transferProtocolHead.setTargetUserName(UserNameOfSender);
                                        transferProtocolHead.setVersion(CodeDynamicConfig.getProtocolVersion());
                                        transferProtocolHead.setType(HeadType);
                                        transferProtocol.setTransferProtocolHead(transferProtocolHead);
                                        TransferProtocol.TransferProtocolBodyBean transferProtocolBody = new TransferProtocol.TransferProtocolBodyBean();
                                        transferProtocolBody.setData(BodyData);
                                        transferProtocol.setTransferProtocolBody(transferProtocolBody);
                                        return transferProtocol;
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
                        if (!SpecialMode) {
                            System.exit(0);
                        }
                        else
                        {
                            getClientThreadGroup().interrupt();
                        }
                    }
                }
                if (this.isInterrupted())
                {
                    return;
                }
                if (!SpecialMode) {
                    System.exit(0);
                }
                else
                {
                    getClientThreadGroup().interrupt();
                }
            }
        }.start();
    }
}
