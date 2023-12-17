package org.yuezhikong.newClient;

import cn.hutool.core.lang.UUID;
import cn.hutool.crypto.CryptoException;
import cn.hutool.crypto.SecureUtil;
import cn.hutool.crypto.symmetric.AES;
import com.google.gson.Gson;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.LineBasedFrameDecoder;
import io.netty.handler.codec.MessageToMessageDecoder;
import io.netty.handler.codec.MessageToMessageEncoder;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.handler.codec.string.StringEncoder;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.util.ReferenceCountUtil;
import org.apache.commons.codec.binary.Base64;
import org.apache.logging.log4j.LogManager;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.TestOnly;
import org.yuezhikong.CodeDynamicConfig;
import org.yuezhikong.GeneralMethod;
import org.yuezhikong.utils.*;
import org.yuezhikong.utils.Protocol.LoginProtocol;
import org.yuezhikong.utils.Protocol.NormalProtocol;

import java.io.File;
import java.io.IOException;
import java.nio.CharBuffer;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class NettyClient extends GeneralMethod {
    static {
        instance = new NettyClient();
    }
    private NettyClient() {}
    private static NettyClient instance;
    private final ThreadGroup ClientThreadGroup = new ThreadGroup(Thread.currentThread().getThreadGroup(), "Client Thread Group");
    private final ThreadGroup IOThreadGroup = new ThreadGroup(ClientThreadGroup, "IO ThreadGroup");

    public Channel getChannel() {
        return channel;
    }

    public static NettyClient getInstance() {
        return instance;
    }

    private boolean started = false;
    private boolean stopped = false;

    public boolean isStarted() {
        return started;
    }

    public boolean isStopped() {
        return stopped;
    }

    private String serverPublicKey;
    private ExecutorService UserRequestDisposeThreadPool;

    private ScheduledExecutorService TimerThreadPool;

    protected Logger initLogger()
    {
        return new Logger(null);
    }

    private Logger logger;
    /**
     * 启动一个Netty聊天客户端
     * @apiNote 请注意，此方法在Netty关闭前不会退出，是一个同步方法!
     * @param host 服务器地址
     * @param port 服务器端口
     * @param ServerPublicKey 服务器公钥
     * @throws IllegalArgumentException host为null或空，port小于1或大于65535，ServerPublicKey为null或空
     */
    public void start(String host, int port, String ServerPublicKey) {
        checks.checkArgument(host == null || host.isEmpty(), "Host is null or empty");
        checks.checkArgument(port < 1 || port > 65535, "The Port is not in the range of [0,65535]!");
        checks.checkArgument(ServerPublicKey == null || ServerPublicKey.isEmpty(),"Server Public Key is null or empty");
        if (started)
            return;
        synchronized (this) {
            if (started)
                return;
            started = true;
        }
        logger = initLogger();
        RSA_KeyAutogenerate(getPublicKeyFile().getPath(),getPrivateKeyFile().getPath(),logger);
        serverPublicKey = ServerPublicKey;
        TimerThreadPool = Executors.newScheduledThreadPool(1,new ThreadFactory() {
            private final AtomicInteger threadNumber = new AtomicInteger(1);

            @Override
            public Thread newThread(@NotNull Runnable r) {
                return new Thread(IOThreadGroup, r,"Timer Thread #"+threadNumber.getAndIncrement());
            }
        });
        UserRequestDisposeThreadPool = Executors.newSingleThreadExecutor(new ThreadFactory() {
            private final AtomicInteger threadNumber = new AtomicInteger(1);
            @Override
            public Thread newThread(@NotNull Runnable r) {
                return new Thread(new ThreadGroup(Thread.currentThread().getThreadGroup(), "UserRequestDispose ThreadGroup"),
                        r,"UserRequestDispose Thread #"+threadNumber.getAndIncrement());
            }
        });
        EventLoopGroup workerGroup = new NioEventLoopGroup(new ThreadFactory() {
            private final AtomicInteger threadNumber = new AtomicInteger(1);
            @Override
            public Thread newThread(@NotNull Runnable r) {
                return new Thread(IOThreadGroup,
                        r,"Netty Worker Thread #"+threadNumber.getAndIncrement());
            }
        });

        DefaultEventLoopGroup RecvMessageThreadPool = new DefaultEventLoopGroup((new ThreadFactory() {
            private final AtomicInteger threadNumber = new AtomicInteger(1);
            @Override
            public Thread newThread(@NotNull Runnable r) {
                return new Thread(new ThreadGroup(Thread.currentThread().getThreadGroup(), "Recv Message Thread Group"),
                        r,"Recv Message Thread #"+threadNumber.getAndIncrement());
            }
        }));
        ChannelFuture future = null;
        try {
            Bootstrap bootstrap = new Bootstrap();
            bootstrap.group(workerGroup)
                    .channel(NioSocketChannel.class)
                    .handler(new ChannelInitializer<>() {
                        @Override
                        protected void initChannel(Channel ch) throws Exception {
                            ch.pipeline().addLast(new LoggingHandler(LogLevel.DEBUG));
                            ch.pipeline().addLast(new StringDecoder(StandardCharsets.UTF_8));
                            ch.pipeline().addLast(new StringEncoder(StandardCharsets.UTF_8));
                            ch.pipeline().addLast(new LineBasedFrameDecoder(100000000));
                            ch.pipeline().addLast(new ClientInDecoder());
                            ch.pipeline().addLast(new ClientOutEncoder());

                            ch.pipeline().addLast(RecvMessageThreadPool,new ClientHandler());//JavaIM逻辑
                        }
                    });
            future = bootstrap.connect(host, port).sync();
            future.channel().closeFuture().sync();
        }
        catch (InterruptedException e) {
            stop();
            SaveStackTrace.saveStackTrace(e);
        }
        finally {
            if (future != null)
                future.channel().close();
            RecvMessageThreadPool.shutdownGracefully();
            workerGroup.shutdownGracefully();
            TimerThreadPool.shutdownNow();
            UserRequestDisposeThreadPool.shutdownNow();
            ClientThreadGroup.interrupt();
        }
    }

    /**
     * 发送消息给服务器
     * @param message 消息
     * @throws IllegalArgumentException message 为null或为空
     * @throws IllegalStateException 客户端未启动或已关闭
     */
    @SuppressWarnings("unused")
    public void sendMessage(String message) {
        checks.checkArgument(message == null || message.isEmpty(), "message is null or empty");
        checks.checkState(channel == null || !channel.isWritable(), "Client channel is not init or is not writeable");
        checks.checkState(!started, "Client is not started");
        checks.checkState(stopped,"Client is stopped");
        SendData(message,status,channel);
    }
    private Channel channel;

    public void stop() {
        if (!started)
            return;

        if (stopped)
            return;

        synchronized (this) {
            if (stopped)
                return;
            stopped = true;
        }
        channel.close();
        instance = new NettyClient();
    }

    /**
     * 获取公钥文件
     * @return 公钥文件
     */
    protected File getPublicKeyFile()
    {
        return new File("./ClientRSAKey/ClientPublicKey.txt");
    }

    /**
     * 获取私钥文件
     * @return 私钥文件
     */
    protected File getPrivateKeyFile()
    {
        return new File("./ClientRSAKey/ClientPrivateKey.txt");
    }

    /**
     * 获取端到端加密保存数据文件夹
     * @return 文件夹
     */
    protected File getEndToEndEncryptionSavedDirectory()
    {
        return new File("./end-to-end_encryption_saved");
    }

    /**
     * 请求用户token
     * @return 用户token
     */
    protected String RequestUserToken()
    {
        if (!(new File("./token.txt").exists() && new File("./token.txt").isFile() && new File("./token.txt").canRead()))
            return "";
        try {
            return FileIO.readFileToString(new File("./token.txt"));
        } catch (IOException e) {
            return "";
        }
    }

    /**
     * 写入用户token
     * @param UserToken 新的用户token
     * @throws IOException 出现IO错误
     */
    protected void writeUserToken(String UserToken) throws IOException {
        FileIO.writeStringToFile(new File("./token.txt"),UserToken);
    }

    /**
     * 向用户请求用户名密码
     * @return 用户名密码
     */
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
    /**
     * 客户端加密模式
     */
    protected enum EncryptionMode
    {
        /**
         * 明文未加密
         */
        NON_ENCRYPTION,
        /**
         * RSA加密中
         */
        RSA_ENCRYPTION,
        /**
         * AES加密中
         */
        AES_ENCRYPTION
    }
    /**
     * 客户端状态
     * @param encryptionMode 客户端加密状态
     * @param encryptionKey 客户端密钥
     */
    public record ClientStatus(EncryptionMode encryptionMode, String encryptionKey) {}
    public void SendData(String msg, ClientStatus status, Channel channel) {
        if (status.encryptionMode.equals(EncryptionMode.NON_ENCRYPTION))
        {
            channel.writeAndFlush(msg);
        }
        else if (status.encryptionMode.equals(EncryptionMode.RSA_ENCRYPTION))
        {
            channel.writeAndFlush(RSA.encrypt(msg,serverPublicKey));
        }
        else if (status.encryptionMode.equals(EncryptionMode.AES_ENCRYPTION))
        {
            channel.writeAndFlush(new AES(
                    "ECB",
                    "PKCS5Padding",
                    Base64.decodeBase64(status.encryptionKey)
            ).encryptBase64(msg));
        }
        else {
            throw new RuntimeException("The Encryption Mode is not Support!");
        }
    }

    private ClientStatus status;
    private class ClientHandler extends ChannelInboundHandlerAdapter
    {
        private final Gson gson = new Gson();


        private void UserRequestDispose(ChannelHandlerContext ctx, String msg)
        {
            LogManager.getLogger("Debug").debug("接收到消息:{}",msg);
            NormalProtocol protocol = gson.fromJson(msg,NormalProtocol.class);
            if (protocol.getMessageHead().getVersion() != CodeDynamicConfig.getProtocolVersion())
            {
                logger.info("无效的协议版本");
                logger.info("可能为服务器版本过期或客户端版本过期导致的");
                logger.info("由于此问题，连接将被关闭");
                stop();
                return;
            }
            switch (protocol.getMessageHead().getType())
            {
                case "Test" : {
                    logger.info("服务器响应：" + protocol.getMessageBody().getMessage());
                    if (status.encryptionMode.equals(EncryptionMode.NON_ENCRYPTION))
                    {
                        try {
                            logger.info("正在配置RSA加密...");
                            String publicKey = FileIO.readFileToString(getPublicKeyFile(),StandardCharsets.UTF_8);

                            protocol = new NormalProtocol();
                            NormalProtocol.MessageHead head = new NormalProtocol.MessageHead();
                            head.setVersion(CodeDynamicConfig.getProtocolVersion());
                            head.setType("RSAEncryption");
                            protocol.setMessageHead(head);
                            NormalProtocol.MessageBody body = new NormalProtocol.MessageBody();
                            body.setMessage(publicKey);
                            body.setFileLong(0);
                            protocol.setMessageBody(body);
                            ctx.channel().writeAndFlush(gson.toJson(protocol));
                            status = new ClientStatus(EncryptionMode.RSA_ENCRYPTION,null);
                            logger.info("RSA加密配置完成");

                            logger.info("正在配置AES加密...");
                            String RandomForClient = UUID.randomUUID(true).toString();
                            protocol = new NormalProtocol();
                            head = new NormalProtocol.MessageHead();
                            head.setType("AESEncryption");
                            head.setVersion(CodeDynamicConfig.getProtocolVersion());
                            protocol.setMessageHead(head);
                            body = new NormalProtocol.MessageBody();
                            body.setMessage(RandomForClient);
                            protocol.setMessageBody(body);
                            SendData(gson.toJson(protocol), status, ctx.channel());
                            status = new ClientStatus(EncryptionMode.RSA_ENCRYPTION,RandomForClient);//借用RSA_Encryption的Encryption_Key来保存RandomForClient
                        } catch (IOException e) {
                            logger.info("无法读取客户端密钥文件");
                            logger.info("可能是由于文件权限问题导致的");
                            logger.info("由于此问题，连接将被关闭");
                            SaveStackTrace.saveStackTrace(e);
                            stop();
                            break;
                        }
                    }
                    break;
                }
                case "AESEncryption" : {
                    String RandomForServer = protocol.getMessageBody().getMessage();
                    status = new ClientStatus(EncryptionMode.AES_ENCRYPTION,
                            Base64.encodeBase64String(GenerateKey(RandomForServer,status.encryptionKey).getEncoded()));

                    protocol = new NormalProtocol();
                    NormalProtocol.MessageHead head = new NormalProtocol.MessageHead();
                    head.setVersion(CodeDynamicConfig.getProtocolVersion());
                    head.setType("Test");
                    protocol.setMessageHead(head);
                    NormalProtocol.MessageBody body = new NormalProtocol.MessageBody();
                    body.setMessage("你好服务端");
                    protocol.setMessageBody(body);
                    SendData(gson.toJson(protocol), status, ctx.channel());
                    logger.info("AES加密配置完成");

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
                    SendData(gson.toJson(protocol), status, ctx.channel());
                    break;
                }
                case "options": {
                    logger.info("服务器响应："+protocol.getMessageBody().getMessage());
                    if (protocol.getMessageBody().getMessage().equals("AllowedTransferProtocol:Accept"))
                    {
                        logger.info("TransferProtocol配置完成");
                        logger.info("正在执行登录...");
                        if (!RequestUserToken().isEmpty())
                        {
                            LoginProtocol loginProtocol = new LoginProtocol();
                            LoginProtocol.LoginPacketHeadBean loginPacketHead = new LoginProtocol.LoginPacketHeadBean();
                            loginPacketHead.setType("token");
                            loginProtocol.setLoginPacketHead(loginPacketHead);
                            LoginProtocol.LoginPacketBodyBean loginPacketBody = new LoginProtocol.LoginPacketBodyBean();
                            LoginProtocol.LoginPacketBodyBean.ReLoginBean reLogin = new LoginProtocol.LoginPacketBodyBean.ReLoginBean();
                            reLogin.setToken(RequestUserToken());
                            loginPacketBody.setReLogin(reLogin);
                            loginProtocol.setLoginPacketBody(loginPacketBody);
                            SendData(gson.toJson(loginProtocol) , status, ctx.channel());
                        }
                        else {
                            UserNameAndPasswordLogin();
                        }
                    }
                    break;
                }
                case "Login": {
                    if ("Success".equals(protocol.getMessageBody().getMessage()))
                    {
                        logger.info("登录成功");
                        StartChatSystem(status,channel);
                    }
                    else if ("Fail".equals(protocol.getMessageBody().getMessage()))
                    {
                        if (PasswordLogin.get())
                        {
                            logger.info("登录失败，用户名或密码错误!");
                            logger.info("连接即将被关闭");
                            stop();
                        } else {
                            logger.info("登录失败，Token无效!");
                            UserNameAndPasswordLogin();
                        }
                    }
                    else {
                        try {
                            writeUserToken(protocol.getMessageBody().getMessage());
                        } catch (IOException ignored) {
                        }
                        if (LegacyLogin.get()) {
                            SendData(ChangePasswordJson, status, channel);
                            logger.info("登录成功并已自动更新密码加密!");
                        }
                        else
                            logger.info("登录成功");
                        StartChatSystem(status, channel);
                    }
                    break;
                }
                case "Chat" : {
                    logger.ChatMsg(protocol.getMessageBody().getMessage());
                    break;
                }
                default: {
                    logger.info("接收到未知消息，json为:"+msg);
                    break;
                }
            }
        }
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
                Scanner scanner = new Scanner(System.in);
                int UserInput = Integer.parseInt(scanner.nextLine());
                if (UserInput == 1)
                {
                    return true;
                }
            } catch (NumberFormatException ignored) {}
            return false;
        }
        private String ChangePasswordJson = "";
        private final AtomicBoolean LegacyLogin = new AtomicBoolean(false);
        private final AtomicBoolean PasswordLogin = new AtomicBoolean(false);
        private void UserNameAndPasswordLogin() {
            PasswordLogin.set(true);
            LegacyLogin.set(isLegacyLoginORNormalLogin());
            String UserName;
            String Password;
            if (LegacyLogin.get())
            {
                String[] UserData = RequestUserNameAndPassword();
                if (UserData.length != 2)
                    throw new RuntimeException("The RequestUserNameAndPassword Method Returned Data Is Not Support");
                UserName = UserData[0];//UserData[0]是明文用户名
                Password = UserData[1];//UserData[1]是明文密码

                NormalProtocol protocol = new NormalProtocol();
                NormalProtocol.MessageHead head = new NormalProtocol.MessageHead();
                head.setVersion(CodeDynamicConfig.getProtocolVersion());
                head.setType("ChangePassword");
                protocol.setMessageHead(head);
                NormalProtocol.MessageBody body = new NormalProtocol.MessageBody();
                body.setMessage(SecureUtil.sha256(Password));
                protocol.setMessageBody(body);
                ChangePasswordJson = gson.toJson(protocol);
            }
            else {
                String[] UserData = RequestUserNameAndPassword();
                if (UserData.length != 2)
                    throw new RuntimeException("The RequestUserNameAndPassword Method Returned Data Is Not Support");
                UserName = UserData[0];//UserData[0]是明文用户名
                Password = SecureUtil.sha256(UserData[1]);//UserData[1]是明文密码
            }
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
            SendData(gson.toJson(loginProtocol) , status, channel);
        }
        private final AtomicBoolean TimerThreadPoolStarted = new AtomicBoolean(false);
        @Override
        public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
            try
            {
                if (!(msg instanceof String Msg))
                    return;
                switch (Msg)//明文处理
                {
                    case "Hello, Client" :
                    {
                        ctx.writeAndFlush("你好，服务端");
                        return;
                    }
                    case "你好，客户端" : {
                        if (TimerThreadPoolStarted.get())
                            return;
                        TimerThreadPool.scheduleWithFixedDelay(() -> {
                            TimerThreadPoolStarted.set(true);
                            if (channel.isWritable())
                                channel.writeAndFlush("Alive");
                            else
                                TimerThreadPool.shutdownNow();
                        },0, CodeDynamicConfig.HeartbeatInterval, TimeUnit.SECONDS);

                        NormalProtocol protocol = new NormalProtocol();
                        NormalProtocol.MessageHead head = new NormalProtocol.MessageHead();
                        head.setVersion(CodeDynamicConfig.getProtocolVersion());
                        head.setType("Test");
                        protocol.setMessageHead(head);
                        NormalProtocol.MessageBody body = new NormalProtocol.MessageBody();
                        body.setMessage("你好服务端");
                        protocol.setMessageBody(body);
                        channel.writeAndFlush(gson.toJson(protocol));
                    }
                    case "Alive" :
                    {
                        return;
                    }
                    case "Decryption Error" : {
                        logger.info("服务器无法解密消息!");
                        logger.info("这一般是由于服务端公钥配置错误导致的!");
                        logger.info("请检查服务端公钥配置文件是否正确!");
                        logger.info("由于此问题，连接将被关闭");
                        stop();
                        return;
                    }
                    case "The Encryption Mode is not Support!" : {
                        logger.info("服务器不支持指定的加密模式!");
                        logger.info("可能为服务器版本过期而导致的");
                        logger.info("由于此问题，连接将被关闭");
                        stop();
                        return;
                    }
                    case "This Json have Syntax Error", "Invalid Input! Connection will be close": {
                        logger.info("服务器无法解析输入的内容!");
                        logger.info("可能为服务器版本过期导致的");
                        logger.info("由于此问题，连接将被关闭");
                        stop();
                        return;
                    }
                    case "Server have internal server error, Connection will be close": {
                        logger.info("发生服务器内部错误，连接将被关闭");
                        stop();
                        return;
                    }
                    case "The User Login is disabled on this time": {
                        logger.info("用户登录在加密未完成或已登录时发生");
                        logger.info("可能为服务器版本过期或客户端被修改导致的");
                        logger.info("由于此问题，连接将被关闭");
                        stop();
                        return;
                    }
                    case "Invalid Protocol Version,Connection will be close": {
                        logger.info("无效的协议版本");
                        logger.info("可能为服务器版本过期或客户端版本过期导致的");
                        logger.info("由于此问题，连接将被关闭");
                        stop();
                        return;
                    }
                    case "Invalid Mode! The ChangePassword Mode is disabled on this time.": {
                        logger.info("更改密码在登录前发生");
                        logger.info("可能为服务器版本过期或客户端被修改导致的");
                        logger.info("由于此问题，连接将被关闭");
                        stop();
                        return;
                    }
                    case "Invalid Mode! The Chat Mode is disabled on this time.": {
                        logger.info("聊天在登录前发生");
                        logger.info("可能为服务器版本过期或客户端被修改导致的");
                        logger.info("由于此问题，连接将被关闭");
                        stop();
                        return;
                    }
                    case "The RSAEncryption Set Mode is disabled on this time.": {
                        logger.info("RSA握手在已经建立RSA加密或AES加密的情况下发生");
                        logger.info("可能为服务器版本过期或客户端被修改导致的");
                        logger.info("由于此问题，连接将被关闭");
                        stop();
                        return;
                    }
                    case "The AESEncryption Set Mode is disabled on this time.": {
                        logger.info("AES握手在已经建立RSA加密或AES加密的情况下发生");
                        logger.info("可能为服务器版本过期或客户端被修改导致的");
                        logger.info("由于此问题，连接将被关闭");
                        stop();
                        return;
                    }
                    case "This setting is not support on this Server!": {
                        logger.info("options中的配置项目在此服务器不受支持");
                        logger.info("可能为服务器版本过期导致的");
                        logger.info("由于此问题，连接将被关闭");
                        stop();
                        return;
                    }
                    case "This mode is not Support on this Server!": {
                        logger.info("此消息header头在此服务器不受支持");
                        logger.info("可能为服务器版本过期导致的");
                        logger.info("由于此问题，连接将被关闭");
                        stop();
                        return;
                    }
                }
                if (status.encryptionMode.equals(EncryptionMode.NON_ENCRYPTION)) {
                    try {
                        UserRequestDispose(ctx, Msg);
                    } catch (CryptoException e) {
                        ctx.writeAndFlush("Decryption Error");
                    }
                }
                else if (status.encryptionMode.equals(EncryptionMode.RSA_ENCRYPTION)) {
                    try {
                        UserRequestDispose(ctx, RSA.decrypt(Msg, FileIO.readFileToString(getPrivateKeyFile())));
                    } catch (CryptoException e) {
                        ctx.writeAndFlush("Decryption Error");
                    }
                }
                else if (status.encryptionMode.equals(EncryptionMode.AES_ENCRYPTION)) {
                    UserRequestDispose(
                            ctx,
                            new AES(
                                    "ECB",
                                    "PKCS5Padding",
                                    Base64.decodeBase64(status.encryptionKey)
                            ).decryptStr(Msg)
                    );
                }
            }
            finally {
                ReferenceCountUtil.release(msg);
            }
        }

        @Override
        public void channelActive(ChannelHandlerContext ctx) throws Exception {
            logger.info("已经连接到服务器"+ctx.channel().remoteAddress());
            status = new ClientStatus(EncryptionMode.NON_ENCRYPTION,"");
            channel = ctx.channel();
            ctx.channel().writeAndFlush("Hello Server");
            ctx.fireChannelActive();
        }

        @Override
        public void channelInactive(ChannelHandlerContext ctx) throws Exception {
            logger.info("已经从服务器"+ctx.channel().remoteAddress()+"断开连接");
            stop();
            ctx.fireChannelInactive();
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
            logger.info("连接出现错误："+cause.getMessage()+"，即将断开连接");
            stop();
            SaveStackTrace.saveStackTrace(cause);
        }
    }
    /**
     * 代表需要退出程序的受检异常
     */
    protected static class QuitException extends Exception
    {
        public QuitException(String Message)
        {
            super(Message);
        }
    }
    /**
     * 客户端指令处理程序
     * @param UserInput 用户输入
     * @return {@code true} 是一条命令 {@code false} 不是一条命令
     * @throws IOException Socket IO出错
     * @throws QuitException 用户的指令是.quit
     */
    @Contract(pure = true)
    protected boolean CommandRequest(String UserInput,ClientStatus status,Channel channel) throws IOException, QuitException {
        String command;
        checks.checkArgument(UserInput == null || UserInput.isEmpty(), "User Input is Empty!");
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
            case ".help" : {
                logger.info("客户端命令系统");
                logger.info(".help 查询帮助信息");
                logger.info(".quit 离开服务器并退出程序");
                logger.info(".change-password 修改密码");
                logger.info(".about 查看程序帮助");
                return true;
            }
            case ".about" : {
                logger.info("JavaIM是根据GNU General Public License v3.0开源的自由程序（开源软件)");
                logger.info("主仓库位于：https://github.com/JavaIM/JavaIM");
                logger.info("主要开发者名单：");
                logger.info("QiLechan（柒楽)");
                logger.info("AlexLiuDev233 （阿白)");
                return true;
            }
            case ".quit" : {
                Gson gson = new Gson();
                NormalProtocol protocol = new NormalProtocol();
                NormalProtocol.MessageHead head = new NormalProtocol.MessageHead();
                head.setVersion(CodeDynamicConfig.getProtocolVersion());
                head.setType("Leave");
                protocol.setMessageHead(head);
                NormalProtocol.MessageBody body = new NormalProtocol.MessageBody();
                body.setMessage(UserInput);
                protocol.setMessageBody(body);
                SendData(gson.toJson(protocol),status,channel);
                throw new QuitException("UserRequestQuit");
            }
            case ".crash" : {
                if (CodeDynamicConfig.GetDebugMode()) {
                    throw new RuntimeException("Debug Crash");
                }
                else
                {
                    return false;
                }
            }
            case ".change-password" : {
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
                    SendData(new Gson().toJson(protocol),status,channel);
                }
                else
                {
                    logger.info("不符合命令语法！");
                    logger.info("此命令的语法为：.change-password <新密码>");
                }
                return true;
            }
            default : {
                return false;
            }
        }

    }
    private final AtomicBoolean ConnectComplete = new AtomicBoolean(false);
    private void StartChatSystem(ClientStatus status,Channel channel) {
        ConnectComplete.set(true);
        synchronized (ConnectComplete) {
            ConnectComplete.notifyAll();
        }
        new Thread(ClientThreadGroup,"Console Chat Input Request Thread")
        {
            @Override
            public void run() {
                Gson gson = new Gson();
                Scanner scanner = new Scanner(System.in);
                do {
                    String UserInput = scanner.nextLine();
                    if (UserInput.isEmpty())
                        continue;
                    try {
                        if (CommandRequest(UserInput,status,channel))
                            continue;
                    } catch (IOException e) {
                        SaveStackTrace.saveStackTrace(e);
                        NettyClient.this.stop();
                        return;
                    } catch (QuitException e) {
                        NettyClient.this.stop();
                        return;
                    }

                    NormalProtocol protocol = new NormalProtocol();//开始构造协议
                    NormalProtocol.MessageHead head = new NormalProtocol.MessageHead();
                    NormalProtocol.MessageBody body = new NormalProtocol.MessageBody();
                    head.setVersion(CodeDynamicConfig.getProtocolVersion());
                    head.setType("Chat");//类型是聊天数据包
                    protocol.setMessageHead(head);
                    body.setMessage(UserInput);
                    protocol.setMessageBody(body);
                    SendData(gson.toJson(protocol),status,channel);
                } while (scanner.ioException() == null);
            }
        }.start();
    }

    private static class ClientOutEncoder extends MessageToMessageEncoder<CharSequence>
    {
        @Override
        protected void encode(ChannelHandlerContext ctx, CharSequence msg, List<Object> out) {
            if (msg.isEmpty())
                return;

            if (msg.charAt(msg.length() - 1) == '\n')
                out.add(CharBuffer.wrap(msg));
            else
                out.add(CharBuffer.wrap(msg+"\n"));
        }
    }

    private static class ClientInDecoder extends MessageToMessageDecoder<CharSequence>
    {
        @Override
        protected void decode(ChannelHandlerContext ctx, CharSequence msg, List<Object> out) {
            out.add(msg.toString().
                    replaceAll("\r","").
                    replaceAll("\n",""));
        }
    }

    @TestOnly
    public static void main(String[] args)
    {
        ConfigFileManager prop = new ConfigFileManager();
        prop.CreateServerprop();
        prop.CreateClientprop();
        Scanner scanner = new Scanner(System.in);
        System.out.println("请依次输入Server Host，Server Port，Server Public Key");
        NettyClient.getInstance().start(
                scanner.nextLine(),
                Integer.parseInt(scanner.nextLine())
                ,scanner.nextLine());
    }
}
