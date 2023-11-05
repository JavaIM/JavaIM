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
package org.yuezhikong.newServer;

import cn.hutool.crypto.SecureUtil;
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
import org.yuezhikong.newServer.UserData.Authentication.UserAuthentication;
import org.yuezhikong.newServer.UserData.SimpleUser;
import org.yuezhikong.newServer.UserData.user;
import org.yuezhikong.newServer.api.SingleAPI;
import org.yuezhikong.newServer.api.api;
import org.yuezhikong.newServer.plugin.PluginManager;
import org.yuezhikong.newServer.plugin.SimplePluginManager;
import org.yuezhikong.utils.Logger;
import org.yuezhikong.utils.Protocol.LoginProtocol;
import org.yuezhikong.utils.Protocol.NormalProtocol;
import org.yuezhikong.utils.Protocol.TransferProtocol;
import org.yuezhikong.utils.RSA;
import org.yuezhikong.utils.SaveStackTrace;

import javax.crypto.SecretKey;
import javax.security.auth.login.AccountNotFoundException;
import java.io.File;
import java.io.IOException;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 新服务端
 * @author AlexLiuDev233
 */
public class ServerMain extends GeneralMethod implements IServerMain {
    static {
        request = new ChatRequest();//初始化ChatRequest
    }
    private final List<user> Users = new ArrayList<>();
    private int clientIDAll = 0;
    private ThreadGroup IOGroup;
    private ExecutorService IOThreadPool;
    private ExecutorService RecvMessageThreadPool;

    @SuppressWarnings("unused")
    @Override
    public ExecutorService getIOThreadPool() {
        return IOThreadPool;
    }

    private ThreadGroup recvMessageThreadGroup;
    private ThreadGroup ServerGroup;
    private boolean NowCanSendRequest = false;

    protected ThreadGroup getRecvMessageThreadGroup() {
        return recvMessageThreadGroup;
    }

    protected static class UserAuthThread extends Thread
    {
        private final Logger logger;
        private final NetworkManager.NetworkData ServerNetworkData;
        @Contract("_,null -> fail")
        public UserAuthThread(ThreadGroup group, String name)
        {
            super(group,name);
            this.logger = getServer().logger;
            this.ServerNetworkData = getServer().ServerTCPNetworkData;
            this.setDaemon(true);
        }
        @Override
        public void run() {
            this.setUncaughtExceptionHandler(CrashReport.getCrashReport());
            while (true)
            {
                NetworkManager.NetworkData clientNetworkData;
                try {
                    clientNetworkData = NetworkManager.AcceptTCPConnection(ServerNetworkData);
                    clientNetworkData.setSoTimeout(CodeDynamicConfig.SocketTimeout);
                } catch (IOException e) {
                    SaveStackTrace.saveStackTrace(e);
                    break;
                }
                //检查是否超过用户上限
                if (CodeDynamicConfig.getMaxClient() != -1 && getServer().getServerAPI().GetValidClientList(false).size() >= CodeDynamicConfig.getMaxClient() -1)
                {
                    try {
                        NetworkManager.ShutdownTCPConnection(clientNetworkData);
                    } catch (IOException e) {
                        continue;
                    }
                    continue;
                }
                user CurrentUser = new SimpleUser(clientNetworkData,getServer().clientIDAll,false);//创建用户class
                CurrentUser.setUserAuthentication(new UserAuthentication(CurrentUser, getServer().getIOThreadPool()));
                getServer().Users.add(CurrentUser);
                getServer().Users.set(getServer().clientIDAll,CurrentUser);//添加到List中
                getServer().StartRecvMessageThread(getServer().clientIDAll);//启动RecvMessage线程
                getServer().clientIDAll++;//当前的最大ClientID加1
                logger.info("连入了新的socket请求");
                logger.info("基本信息如下：");
                logger.info("远程ip地址为："+clientNetworkData.getRemoteSocketAddress());
                logger.info("远程端口为："+clientNetworkData.getPort());
            }
        }
    }


    protected ThreadGroup getServerGroup() {
        return ServerGroup;
    }

    @Override
    public List<user> getUsers() {
        return new ArrayList<>(Users);
    }

    @Override
    public boolean RegisterUser(user User) {
        try {
            getServerAPI().GetUserByUserName(User.getUserName());
            return false;
        } catch (AccountNotFoundException e) {
            Users.add(User);
            return true;
        }
    }

    public static class RecvMessageThread
    {
        private final user CurrentUser;
        public RecvMessageThread(int ClientID)
        {
            CurrentUser = getServer().Users.get(ClientID);
        }
        public void LoginSystem(@NotNull user User) throws IOException
        {
            String json = NetworkManager.RecvDataFromRemote(User.getUserNetworkData());
            json = User.getUserAES().decryptStr(json);
            LoginProtocol loginProtocol = new Gson().fromJson(json,LoginProtocol.class);
            if ("token".equals(loginProtocol.getLoginPacketHead().getType()))
            {
                if (!Objects.requireNonNull(User.getUserAuthentication()).
                        DoLogin(loginProtocol.getLoginPacketBody().getReLogin().getToken()))
                    User.UserDisconnect();
            }
            else if ("passwd".equals(loginProtocol.getLoginPacketHead().getType()))
            {
                if (!Objects.requireNonNull(User.getUserAuthentication()).
                        DoLogin(loginProtocol.getLoginPacketBody().getNormalLogin().getUserName(),
                        loginProtocol.getLoginPacketBody().getNormalLogin().getPasswd()))
                    User.UserDisconnect();
            }
            else
                User.UserDisconnect();
        }
        private Thread RecvMessageThread;
        public void run()
        {
            try
            {
                run0();
                Thread.interrupted();
            } catch (Throwable throwable)
            {
                if (CodeDynamicConfig.GetDebugMode())
                {
                    SaveStackTrace.saveStackTrace(throwable);
                }
                getServer().getLogger().warning("处理用户："+CurrentUser.getUserName()+"的线程出现错误");
                getServer().getLogger().warning("正在强行踢出此用户");
                CurrentUser.UserDisconnect();
            }
        }
        private void run0() {
            RecvMessageThread = Thread.currentThread();
            CurrentUser.setRecvMessageThread(this);
            Logger logger = getServer().logger;
            api API = getServer().API;
            try (NetworkManager.NetworkData CurrentUserNetworkData = CurrentUser.getUserNetworkData()) {
                //服务器密钥加载
                final String ServerPrivateKey = FileUtils.readFileToString(new File("./ServerRSAKey/Private.txt"),StandardCharsets.UTF_8);
                //开始握手
                //测试明文通讯
                logger.info("正在连接的客户端返回："+NetworkManager.RecvDataFromRemote(CurrentUserNetworkData,10));
                NetworkManager.WriteDataToRemote(CurrentUserNetworkData,"Hello Client");
                logger.info("正在连接的客户端返回："+NetworkManager.RecvDataFromRemote(CurrentUserNetworkData,10));
                NetworkManager.WriteDataToRemote(CurrentUserNetworkData,"你好，客户端");
                //测试通讯协议
                String json = NetworkManager.RecvDataFromRemote(CurrentUserNetworkData,10);
                NormalProtocol protocol = getServer().protocolRequest(json);
                if (protocol.getMessageHead().getVersion() != CodeDynamicConfig.getProtocolVersion() || !("Test".equals(protocol.getMessageHead().getType())))
                {
                    return;
                }
                logger.info("正在连接的客户端返回："+protocol.getMessageBody().getMessage());

                Gson gson = new Gson();
                protocol = new NormalProtocol();
                NormalProtocol.MessageHead head = new NormalProtocol.MessageHead();
                head.setVersion(CodeDynamicConfig.getProtocolVersion());
                head.setType("Test");
                protocol.setMessageHead(head);
                NormalProtocol.MessageBody body = new NormalProtocol.MessageBody();
                body.setMessage("你好，客户端");
                body.setFileLong(0);
                protocol.setMessageBody(body);
                NetworkManager.WriteDataToRemote(CurrentUserNetworkData,gson.toJson(protocol));
                //RSA Key传递
                json = NetworkManager.RecvDataFromRemote(CurrentUserNetworkData,10);
                json = RSA.decrypt(json,ServerPrivateKey);
                protocol = getServer().protocolRequest(json);
                if (protocol.getMessageHead().getVersion() != CodeDynamicConfig.getProtocolVersion() || !("RSAEncryption".equals(protocol.getMessageHead().getType())))
                {
                    return;
                }
                try {
                    CurrentUser.setPublicKey(protocol.getMessageBody().getMessage());
                } catch (cn.hutool.crypto.CryptoException e)
                {
                    NetworkManager.WriteDataToRemote(CurrentUserNetworkData,"Decryption Error");
                    getServer().getLogger().warning("正在连接的客户端发送的信息无法被解密！");
                    getServer().getLogger().warning("即将断开对于此用户的连接");
                    CurrentUser.UserDisconnect();
                    return;
                }
                //测试RSA
                json = NetworkManager.RecvDataFromRemote(CurrentUserNetworkData,10);
                json = RSA.decrypt(json,ServerPrivateKey);
                protocol = getServer().protocolRequest(json);
                if (protocol.getMessageHead().getVersion() != CodeDynamicConfig.getProtocolVersion() || !("Test".equals(protocol.getMessageHead().getType())))
                {
                    return;
                }
                logger.info("正在连接的客户端返回："+protocol.getMessageBody().getMessage());

                protocol = new NormalProtocol();
                head = new NormalProtocol.MessageHead();
                head.setType("Test");
                head.setVersion(CodeDynamicConfig.getProtocolVersion());
                protocol.setMessageHead(head);
                body = new NormalProtocol.MessageBody();
                body.setMessage("你好客户端");
                protocol.setMessageBody(body);
                NetworkManager.WriteDataToRemote(CurrentUserNetworkData,RSA.encrypt(gson.toJson(protocol),CurrentUser.getPublicKey()));
                //AES制造开始
                protocol = new NormalProtocol();
                head = new NormalProtocol.MessageHead();
                head.setType("AESEncryption");
                head.setVersion(CodeDynamicConfig.getProtocolVersion());
                protocol.setMessageHead(head);
                body = new NormalProtocol.MessageBody();
                String RandomForServer = UUID.randomUUID().toString();
                body.setMessage(RandomForServer);
                protocol.setMessageBody(body);
                NetworkManager.WriteDataToRemote(CurrentUserNetworkData,RSA.encrypt(gson.toJson(protocol),CurrentUser.getPublicKey()));

                json = NetworkManager.RecvDataFromRemote(CurrentUserNetworkData,10);
                json = RSA.decrypt(json,ServerPrivateKey);
                protocol = getServer().protocolRequest(json);
                if (protocol.getMessageHead().getVersion() != CodeDynamicConfig.getProtocolVersion() || !("AESEncryption".equals(protocol.getMessageHead().getType())))
                {
                    return;
                }
                SecretKey key = SecureUtil.generateKey(SymmetricAlgorithm.AES.getValue(), Base64.decodeBase64(getServer().GenerateKey(RandomForServer+protocol.getMessageBody().getMessage())));
                CurrentUser.setUserAES(cn.hutool.crypto.SecureUtil.aes(key.getEncoded()));
                //测试AES
                protocol = new NormalProtocol();
                head = new NormalProtocol.MessageHead();
                head.setVersion(CodeDynamicConfig.getProtocolVersion());
                head.setType("Test");
                protocol.setMessageHead(head);
                body = new NormalProtocol.MessageBody();
                body.setMessage("你好客户端");
                body.setFileLong(0);
                protocol.setMessageBody(body);
                NetworkManager.WriteDataToRemote(CurrentUserNetworkData,CurrentUser.getUserAES().encryptBase64(gson.toJson(protocol)));

                json = NetworkManager.RecvDataFromRemote(CurrentUserNetworkData,10);
                json = CurrentUser.getUserAES().decryptStr(json);
                protocol = getServer().protocolRequest(json);
                if (protocol.getMessageHead().getVersion() != CodeDynamicConfig.getProtocolVersion() || !("Test".equals(protocol.getMessageHead().getType())))
                {
                    return;
                }
                logger.info("正在连接的客户端返回："+protocol.getMessageBody().getMessage());
                //询问是否允许TransferProtocol
                protocol = new NormalProtocol();
                head = new NormalProtocol.MessageHead();
                head.setVersion(CodeDynamicConfig.getProtocolVersion());
                head.setType("options");
                protocol.setMessageHead(head);
                body = new NormalProtocol.MessageBody();
                body.setMessage("AllowTransferProtocol");
                body.setFileLong(0);
                protocol.setMessageBody(body);
                NetworkManager.WriteDataToRemote(CurrentUserNetworkData,CurrentUser.getUserAES().encryptBase64(gson.toJson(protocol)));

                json = NetworkManager.RecvDataFromRemote(CurrentUserNetworkData,10);
                json = CurrentUser.getUserAES().decryptStr(json);
                protocol = getServer().protocolRequest(json);
                if (protocol.getMessageHead().getVersion() != CodeDynamicConfig.getProtocolVersion() || !("options".equals(protocol.getMessageHead().getType())))
                {
                    return;
                }
                CurrentUser.setAllowedTransferProtocol("Enable".equals(protocol.getMessageBody().getMessage()));
                //握手全部完毕，后续是登录系统
                try {
                    LoginSystem(CurrentUser);
                } catch (RuntimeException e)
                {
                    SaveStackTrace.saveStackTrace(e);
                }
                if (!(CurrentUser.isUserLogined()))
                {
                    return;
                }
                //登录完毕，开始聊天
                while (true) {
                    String ChatMsg;
                    ChatMsg = NetworkManager.RecvDataFromRemote(CurrentUserNetworkData,10);
                    ChatMsg = CurrentUser.getUserAES().decryptStr(ChatMsg);
                    protocol = getServer().protocolRequest(ChatMsg);
                    if (protocol.getMessageHead().getVersion() != CodeDynamicConfig.getProtocolVersion())
                    {
                        CurrentUser.UserDisconnect();
                        return;
                    }
                    if ("ChangePassword".equals(protocol.getMessageHead().getType()))
                    {
                        getServer().getServerAPI().ChangeUserPassword(CurrentUser,protocol.getMessageBody().getMessage());
                        continue;
                    }
                    if ("NextIsTransferProtocol".equals(protocol.getMessageHead().getType()))
                    {
                        json = NetworkManager.RecvDataFromRemote(CurrentUserNetworkData,10);
                        json = CurrentUser.getUserAES().decryptStr(json);
                        TransferProtocol transferProtocol = gson.fromJson(json, TransferProtocol.class);
                        if (transferProtocol.getTransferProtocolHead().getVersion() != CodeDynamicConfig.getProtocolVersion())
                        {
                            protocol = new NormalProtocol();
                            head = new NormalProtocol.MessageHead();
                            head.setVersion(CodeDynamicConfig.getProtocolVersion());
                            head.setType("Result");
                            protocol.setMessageHead(head);
                            body = new NormalProtocol.MessageBody();
                            body.setMessage("TransferProtocolVersionIsNotSupport");
                            protocol.setMessageBody(body);
                            NetworkManager.WriteDataToRemote(CurrentUserNetworkData,CurrentUser.getUserAES().encryptBase64(gson.toJson(protocol)));
                            continue;
                        }
                        else if (!CodeDynamicConfig.AllowedTransferProtocol)
                        {
                            protocol = new NormalProtocol();
                            head = new NormalProtocol.MessageHead();
                            head.setVersion(CodeDynamicConfig.getProtocolVersion());
                            head.setType("Result");
                            protocol.setMessageHead(head);
                            body = new NormalProtocol.MessageBody();
                            body.setMessage("ThisServerDisallowedTransferProtocol");
                            protocol.setMessageBody(body);
                            NetworkManager.WriteDataToRemote(CurrentUserNetworkData,CurrentUser.getUserAES().encryptBase64(gson.toJson(protocol)));
                            continue;
                        }
                        else {
                            try {
                                user TargetUser = getServer().getServerAPI().GetUserByUserName(transferProtocol.getTransferProtocolHead().getTargetUserName()
                                );
                                if (TargetUser.isAllowedTransferProtocol())
                                {
                                    NetworkManager.NetworkData TargetUserNetworkData = TargetUser.getUserNetworkData();

                                    protocol = new NormalProtocol();
                                    head = new NormalProtocol.MessageHead();
                                    head.setVersion(CodeDynamicConfig.getProtocolVersion());
                                    head.setType("NextIsTransferProtocol");
                                    protocol.setMessageHead(head);
                                    body = new NormalProtocol.MessageBody();
                                    body.setMessage(CurrentUser.getUserName());
                                    protocol.setMessageBody(body);
                                    NetworkManager.WriteDataToRemote(TargetUserNetworkData,TargetUser.getUserAES().encryptBase64(gson.toJson(protocol)));
                                    
                                    NetworkManager.WriteDataToRemote(TargetUserNetworkData,TargetUser.getUserAES().encryptBase64(json));

                                    protocol = new NormalProtocol();
                                    head = new NormalProtocol.MessageHead();
                                    head.setVersion(CodeDynamicConfig.getProtocolVersion());
                                    head.setType("Result");
                                    protocol.setMessageHead(head);
                                    body = new NormalProtocol.MessageBody();
                                    body.setMessage("Success");
                                }
                                else
                                {
                                    protocol = new NormalProtocol();
                                    head = new NormalProtocol.MessageHead();
                                    head.setVersion(CodeDynamicConfig.getProtocolVersion());
                                    head.setType("Result");
                                    protocol.setMessageHead(head);
                                    body = new NormalProtocol.MessageBody();
                                    body.setMessage("ThisUserDisallowedTransferProtocol");
                                }
                                protocol.setMessageBody(body);
                                NetworkManager.WriteDataToRemote(CurrentUserNetworkData,CurrentUser.getUserAES().encryptBase64(gson.toJson(protocol)));
                                continue;
                            } catch (AccountNotFoundException e) {
                                protocol = new NormalProtocol();
                                head = new NormalProtocol.MessageHead();
                                head.setVersion(CodeDynamicConfig.getProtocolVersion());
                                head.setType("Result");
                                protocol.setMessageHead(head);
                                body = new NormalProtocol.MessageBody();
                                body.setMessage("ThisUserNotFound");
                                protocol.setMessageBody(body);
                                NetworkManager.WriteDataToRemote(CurrentUserNetworkData,CurrentUser.getUserAES().encryptBase64(gson.toJson(protocol)));
                            }
                        }
                        continue;
                    }
                    if ("Leave".equals(protocol.getMessageHead().getType()))
                    {
                        API.SendMessageToUser(CurrentUser,"再见~");
                        CurrentUser.UserDisconnect();
                        return;
                    }
                    if (!("Chat".equals(protocol.getMessageHead().getType())))
                    {
                        CurrentUser.UserDisconnect();
                        return;
                    }
                    ChatRequest.ChatRequestInput input = new ChatRequest.ChatRequestInput(CurrentUser,protocol);
                    if (request.UserChatRequests(input))
                    {
                        continue;
                    }
                    final String ChatMessage = input.getChatMessage();
                    logger.ChatMsg(ChatMessage);
                    API.SendMessageToAllClient(ChatMessage);
                }
            }
            catch (IOException e) {
                if (e instanceof SocketTimeoutException)
                {
                    logger.info("用户："+CurrentUser.getUserName()+"长时间无回应，已断开连接");
                }
                CurrentUser.UserDisconnect();
            }
            finally {
                CurrentUser.UserDisconnect();
            }
        }

        public void interrupt() {
            RecvMessageThread.interrupt();
        }
    }

    private PluginManager pluginManager;
    private user ConsoleUser;
    protected static boolean started = false;
    protected NetworkManager.NetworkData ServerTCPNetworkData;
    protected static ServerMain server;
    private Logger logger;
    protected UserAuthThread authThread;
    private static final ChatRequest request;
    private api API;

    @Override
    public ChatRequest getRequest() {
        return request;
    }

    @Override
    public user getConsoleUser() {
        return ConsoleUser;
    }

    @Override
    public PluginManager getPluginManager() {
        return pluginManager;
    }

    @Override
    public api getServerAPI() {
        return API;
    }

    protected void StartRecvMessageThread(int ClientID)
    {
        RecvMessageThread recvMessageThread = new RecvMessageThread(ClientID);
        RecvMessageThreadPool.execute(recvMessageThread::run);
    }
    @Contract(pure = true)
    public static ServerMain getServer() {
        return server;
    }

    @Override
    public Logger getLogger() {
        return logger;
    }

    private static final Object lock = new Object();
    private static final List<Runnable> codelist = new ArrayList<>();

    @Override
    public void runOnMainThread(Runnable code) {
        if (!NowCanSendRequest)
        {
            synchronized (lock)
            {
                try {
                    lock.wait();
                } catch (InterruptedException e) {
                    SaveStackTrace.saveStackTrace(e);
                }
            }
        }
        codelist.add(code);
        synchronized (lock)
        {
            lock.notifyAll();
        }
    }

    //Logger init
    protected Logger initLogger()
    {
        return new Logger(null);
    }
    protected void ServerCleanup()
    {
        if (!IOThreadPool.isShutdown()) {
            IOThreadPool.shutdownNow();
        }
        if (!RecvMessageThreadPool.isShutdown()) {
            RecvMessageThreadPool.shutdownNow();
        }
    }
    //服务端main
    public void start(int bindPort)
    {
        if (!started)
        {
            try {
                //初始化ThreadGroup
                ServerGroup = Thread.currentThread().getThreadGroup();
                recvMessageThreadGroup = new ThreadGroup(ServerGroup, "recvMessageThreadGroup");
                IOGroup = new ThreadGroup(ServerGroup, "IOGroup");
                IOThreadPool = Executors.newCachedThreadPool(new ThreadFactory() {
                    private final AtomicInteger threadNumber = new AtomicInteger(1);

                    @Override
                    public Thread newThread(@NotNull Runnable r) {
                        return new Thread(new ThreadGroup(IOGroup, "UserRequestDispose ThreadGroup"),
                                r, "IO Thread #" + threadNumber.getAndIncrement());
                    }
                });
                RecvMessageThreadPool = Executors.newCachedThreadPool(new ThreadFactory() {
                    private final AtomicInteger threadNumber = new AtomicInteger(1);
                    @Override
                    public Thread newThread(@NotNull Runnable r) {
                        return new Thread(recvMessageThreadGroup,r,"RecvMessageThread #" + threadNumber.getAndIncrement());
                    }
                });
                new Thread(ServerGroup, "waitInterruptThread") {
                    @Override
                    public void run() {
                        this.setUncaughtExceptionHandler(CrashReport.getCrashReport());
                        synchronized (this) {
                            try {
                                this.wait();
                            } catch (InterruptedException ignored) {
                            }
                        }
                        ServerCleanup();
                    }
                }.start();
                started = true;
                server = this;
                logger = initLogger();
                try {
                    ServerTCPNetworkData = NetworkManager.CreateTCPServer(bindPort);
                } catch (IOException e) {
                    SaveStackTrace.saveStackTrace(e);
                    throw new RuntimeException("Socket Create Failed", e);
                }
                //初始化RSA、数据库、API、UserAuthThread、插件系统
                RSA_KeyAutogenerate("./ServerRSAKey/Public.txt", "./ServerRSAKey/Private.txt", logger);
                API = new SingleAPI(this);
                authThread = new UserAuthThread(ServerGroup, "UserAuthThread");
                authThread.start();
                pluginManager = new SimplePluginManager();
                pluginManager.LoadPluginOnDirectory(new File("./plugins"));
                //服务端虚拟账户初始化
                ConsoleUser = new SimpleUser(null, 0, true);
                ConsoleUser.setAllowedTransferProtocol(false);
                ConsoleUser.setUserAuthentication(new UserAuthentication(ConsoleUser, IOThreadPool));
                ConsoleUser.UserLogin("Server");
                ConsoleUser.SetUserPermission(1, true);
                //启动指令系统
                StartCommandSystem();
                //主线程执行代码
                while (true) {
                    NowCanSendRequest = true;
                    synchronized (lock) {
                        //提醒其他线程，已经可以开始发送请求了
                        lock.notifyAll();
                    }
                    try {
                        synchronized (lock) {
                            lock.wait();
                        }
                    } catch (InterruptedException e) {
                        SaveStackTrace.saveStackTrace(e);
                        break;
                    }
                    NowCanSendRequest = false;
                    for (Runnable runnable : codelist) {
                        runnable.run();
                    }
                    codelist.clear();
                }
            } finally
            {
                try {
                    pluginManager.UnLoadAllPlugin();
                    if (ServerTCPNetworkData != null)
                    {
                        NetworkManager.ShutdownTCPServer(ServerTCPNetworkData);
                    }
                } catch (IOException e) {
                    SaveStackTrace.saveStackTrace(e);
                }
            }
        }
        else
            throw new RuntimeException("Server is Already Started");
    }

    //命令系统
    protected void StartCommandSystem() {
        new Thread(ServerGroup,"Command Thread")
        {
            @Override
            public void run() {
                this.setUncaughtExceptionHandler(CrashReport.getCrashReport());
                //IO初始化
                Scanner scanner = new Scanner(System.in);
                //ChatRequest初始化

                //命令系统
                while (true)
                {
                    String Command = "/"+scanner.nextLine();
                    if ("/ForceClose".equals(Command))
                    {
                        logger.info("这将会强制关闭服务端");
                        logger.info("输入ForceClose来确认，其他取消");
                        if ("ForceClose".equals(scanner.nextLine()))
                        {
                            try {
                                pluginManager.UnLoadAllPlugin();
                            } catch (IOException e) {
                                SaveStackTrace.saveStackTrace(e);
                            }
                            System.exit(0);
                        }
                        System.out.print(">");
                        continue;
                    }
                    request.CommandRequest(new ChatRequest.ChatRequestInput(ConsoleUser,Command));
                }
            }
        }.start();
    }
}
