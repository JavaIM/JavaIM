package org.yuezhikong.newServer;

import cn.hutool.crypto.CryptoException;
import cn.hutool.crypto.SecureUtil;
import cn.hutool.crypto.symmetric.SymmetricAlgorithm;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.handler.codec.string.StringEncoder;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.FileUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.TestOnly;
import org.yuezhikong.CodeDynamicConfig;
import org.yuezhikong.GeneralMethod;
import org.yuezhikong.newServer.UserData.Authentication.IUserAuthentication;
import org.yuezhikong.newServer.UserData.Authentication.UserAuthentication;
import org.yuezhikong.newServer.UserData.NettyUser;
import org.yuezhikong.newServer.UserData.user;
import org.yuezhikong.newServer.api.NettyAPI;
import org.yuezhikong.newServer.api.api;
import org.yuezhikong.newServer.plugin.PluginManager;
import org.yuezhikong.newServer.plugin.SimplePluginManager;
import org.yuezhikong.utils.*;
import org.yuezhikong.utils.Protocol.LoginProtocol;
import org.yuezhikong.utils.Protocol.NormalProtocol;

import javax.security.auth.login.AccountNotFoundException;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

public class NettyNetwork extends GeneralMethod implements IServerMain{
    static {
        nettyNetwork = new NettyNetwork();
    }
    private static NettyNetwork nettyNetwork;
    private ExecutorService UserRequestThreadPool;
    private NettyNetwork() {
    }

    public static NettyNetwork getNettyNetwork() {
        return nettyNetwork;
    }

    private ChannelFuture future;

    public ChannelFuture getFuture() {
        return future;
    }

    /**
     * 客户端状态
     * @param encryptionMode 客户端加密状态
     * @param encryptionKey 客户端密钥
     * @param bindUser 如果握手已经完成，那么此连接所绑定的用户是什么
     */
    private record ClientStatus(ServerHandler.EncryptionMode encryptionMode, String encryptionKey, user bindUser) {}
    private final ConcurrentHashMap<Channel,ClientStatus> ClientChannel = new ConcurrentHashMap<>();
    /**
     * 向用户发送信息(包括加密)
     *
     * @param msg     信息
     * @param channel 消息通道
     */
    public void SendData(String msg, Channel channel)
    {
        ClientStatus status = ClientChannel.get(channel);
        if (status.encryptionMode.equals(ServerHandler.EncryptionMode.NON_ENCRYPTION))
        {
            channel.writeAndFlush(msg+"\n");
        }
        else if (status.encryptionMode.equals(ServerHandler.EncryptionMode.RSA_ENCRYPTION))
        {
            channel.writeAndFlush(RSA.encrypt(msg,status.encryptionKey)+"\n");
        }
        else if (status.encryptionMode.equals(ServerHandler.EncryptionMode.AES_ENCRYPTION))
        {
            channel.writeAndFlush(cn.hutool.crypto.SecureUtil.aes(
                    SecureUtil.generateKey(
                            SymmetricAlgorithm.AES.getValue(), Base64.decodeBase64(status.encryptionKey)
                    ).getEncoded()
            ).encryptBase64(msg)+"\n");
        }
        else {
            throw new RuntimeException("The Encryption Mode is not Support!");
        }
    }

    private Thread userRequestDisposeThread;

    public ExecutorService getUserRequestThreadPool() {
        return UserRequestThreadPool;
    }

    public boolean ServerStartStatus()
    {
        return future != null;
    }

    /**
     * 启动一个Netty聊天服务器
     * @apiNote 请注意，此方法在Netty关闭前不会退出，是一个同步方法!
     * @param bindPort 绑定的端口
     * @param ServerPrivateKey 服务器私钥
     */
    public void StartChatRoomServerForNetty(int bindPort,String ServerPrivateKey)
    {
        this.ServerPrivateKey = ServerPrivateKey;
        UserRequestThreadPool = Executors.newSingleThreadExecutor(new ThreadFactory() {
            private final AtomicInteger threadNumber = new AtomicInteger(1);
            @Override
            public Thread newThread(@NotNull Runnable r) {
                return new Thread(new ThreadGroup(Thread.currentThread().getThreadGroup(), "UserRequestDispose ThreadGroup"),
                        r,"UserRequestDispose Thread #"+threadNumber.getAndIncrement());
            }
        });

        pluginManager = new SimplePluginManager(this);
        pluginManager.LoadPluginOnDirectory(new File("./plugins"));

        serverAPI = new NettyAPI(this);

        EventLoopGroup bossGroup = new NioEventLoopGroup(1);
        EventLoopGroup workerGroup = new NioEventLoopGroup();
        try {
            ServerBootstrap bootstrap = new ServerBootstrap();
            bootstrap.group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .handler(new LoggingHandler(LogLevel.DEBUG))
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        public void initChannel(SocketChannel channel) {
                            ChannelPipeline pipeline = channel.pipeline();
                            pipeline.addLast(new StringDecoder(StandardCharsets.UTF_8));
                            pipeline.addLast(new StringEncoder(StandardCharsets.UTF_8));
                            pipeline.addLast(new ServerHandler());
                        }
                    });

            future = bootstrap.bind(bindPort).sync();

            userRequestDisposeThread = new Thread(Thread.currentThread().getThreadGroup(),"User Console Request Dispose Thread")
            {
                @Override
                public void run() {
                    while (true)
                    {
                        Scanner scanner = new Scanner(System.in);
                        String Command = scanner.nextLine();
                        ServerCommandSend(Command);
                    }
                }
            };
            userRequestDisposeThread.start();
            logger.info("服务器启动完成");
            future.channel().closeFuture().sync();
        } catch (InterruptedException e) {
            SaveStackTrace.saveStackTrace(e);
        } finally {
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
        }
    }

    public void StopNettyChatRoom()
    {
        if (!ServerStartStatus())
            return;
        future.channel().close();
        try {
            pluginManager.UnLoadAllPlugin();
        } catch (IOException ignored) {}
        userRequestDisposeThread.interrupt();
        UserRequestThreadPool.shutdownNow();
        nettyNetwork = new NettyNetwork();
    }
    private final List<IUserAuthentication.UserRecall> LoginRecall = new ArrayList<>();
    private final List<IUserAuthentication.UserRecall> DisconnectRecall = new ArrayList<>();

    public void AddLoginRecall(IUserAuthentication.UserRecall recall)
    {
        LoginRecall.add(recall);
    }

    public void AddDisconnectRecall(IUserAuthentication.UserRecall recall)
    {
        DisconnectRecall.add(recall);
    }
    public void ServerChatMessageSend(String ChatMessage)
    {
        UserRequestThreadPool.execute(() -> {
            ChatRequest.ChatRequestInput input = new ChatRequest.ChatRequestInput(getConsoleUser(),ChatMessage);
            getRequest().ChatFormat(input);
            getServerAPI().SendMessageToAllClient("[服务端消息] "+input.getChatMessage());
            getLogger().ChatMsg("[服务端消息] "+input.getChatMessage());
        });
    }
    public void ServerCommandSend(String Command)
    {
        if (Command.equals("/quit"))
        {
            getLogger().info("正在关闭服务器...");
            StopNettyChatRoom();
            return;
        }
        UserRequestThreadPool.execute(() -> getRequest().CommandRequest(
                new ChatRequest.ChatRequestInput(getConsoleUser(),"/"+Command)));
    }

    private String ServerPrivateKey;
    private PluginManager pluginManager;
    private api serverAPI;

    private final ChatRequest request = new ChatRequest(this);

    private final List<user> users = new ArrayList<>();
    private Logger logger = new Logger(null);

    public void setLogger(Logger logger) {
        this.logger = logger;
    }

    private final user ConsoleUser = new NettyUser(true,this);

    private final ExecutorService IOThreadPool = Executors.newCachedThreadPool(new ThreadFactory() {
        private final AtomicInteger threadNumber = new AtomicInteger(1);

        @Override
        public Thread newThread(@NotNull Runnable r) {
            return new Thread(new ThreadGroup(Thread.currentThread().getThreadGroup(), "UserRequestDispose ThreadGroup"),
                    r, "IO Thread #" + threadNumber.getAndIncrement());
        }
    });

    @Override
    public ExecutorService getIOThreadPool() {
        return IOThreadPool;
    }


    @Override
    public List<user> getUsers() {
        return users;
    }

    @Override
    public boolean RegisterUser(user User) {
        try {
            getServerAPI().GetUserByUserName(User.getUserName());
            return false;
        } catch (AccountNotFoundException e) {
            users.add(User);
            return true;
        }
    }

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
        return serverAPI;
    }

    @Override
    public Logger getLogger() {
        return logger;
    }

    private class ServerHandler extends ChannelInboundHandlerAdapter {
        /**
         * 客户端加密模式
         */
        private enum EncryptionMode
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
         * 向所有用户发送信息(包括加密)
         * @param msg 信息
         */
        private void SendDataToAllClient(String msg)
        {
            ClientChannel.forEach((channel,clientStatus) -> SendData(msg,channel));
        }
        /**
         * 用户请求处理程序，只可处理明文!
         * @param ctx ChannelHandlerContext
         * @param msg 消息
         * @param status 保存的客户端状态
         */
        private void UserRequestDispose(ChannelHandlerContext ctx,String msg,ClientStatus status)
        {
            Gson gson = new Gson();
            NormalProtocol protocol;
            try {
                protocol = gson.fromJson(msg, NormalProtocol.class);
                if (protocol.getMessageHead() == null && protocol.getMessageBody() == null)
                {
                    //登录协议
                    LoginProtocol loginProtocol = gson.fromJson(msg, LoginProtocol.class);
                    if (status.bindUser == null || status.bindUser.isUserLogined())
                    {
                        ctx.writeAndFlush("The User Login is disabled on this time");
                        return;
                    }
                    UserLoginRequestDispose(loginProtocol,status);
                    return;
                }
            } catch (JsonSyntaxException e)
            {
                ctx.writeAndFlush("This Json have Syntax Error\n");
                return;
            }
            if (protocol.getMessageHead().getVersion() != CodeDynamicConfig.getProtocolVersion())
            {
                ctx.writeAndFlush("Invalid Protocol Version,Connection will be close\n");
                if (status.bindUser != null)
                    status.bindUser.UserDisconnect();
                ctx.channel().close();
                return;
            }
            switch (protocol.getMessageHead().getType())
            {
                case "Test" -> {
                    protocol = new NormalProtocol();
                    NormalProtocol.MessageHead head = new NormalProtocol.MessageHead();
                    head.setVersion(CodeDynamicConfig.getProtocolVersion());
                    head.setType("Test");
                    protocol.setMessageHead(head);
                    NormalProtocol.MessageBody body = new NormalProtocol.MessageBody();
                    body.setMessage("你好客户端");
                    protocol.setMessageBody(body);
                    SendData(gson.toJson(protocol),ctx.channel());
                }
                case "ChangePassword" -> {
                    if (status.bindUser == null ||
                            status.bindUser.getUserAuthentication() == null ||
                            !status.bindUser.isUserLogined()
                    )
                    {
                        ctx.writeAndFlush("Invalid Mode! The ChangePassword Mode is disabled on this time.\n");
                        return;
                    }
                    getServerAPI().ChangeUserPassword(status.bindUser,protocol.getMessageBody().getMessage());
                }
                case "Chat" -> {
                    if (status.bindUser == null ||
                            status.bindUser.getUserAuthentication() == null ||
                            !status.bindUser.isUserLogined()
                    )
                    {
                        ctx.writeAndFlush("Invalid Mode! The Chat Mode is disabled on this time.\n");
                        return;
                    }
                    ChatRequest.ChatRequestInput input = new ChatRequest.ChatRequestInput(status.bindUser,protocol.getMessageBody().getMessage());
                    if (!request.UserChatRequests(input)) {
                        protocol.getMessageBody().setMessage(input.getChatMessage());
                        logger.ChatMsg(input.getChatMessage());
                        SendDataToAllClient(gson.toJson(protocol));
                    }
                }
                case "RSAEncryption" -> {
                    if (!status.encryptionMode.equals(EncryptionMode.NON_ENCRYPTION))
                    {
                        ctx.writeAndFlush("The RSAEncryption Set Mode is disabled on this time.\n");
                        return;
                    }

                    user bindUser;
                    if (status.bindUser == null)
                    {
                        bindUser = new NettyUser(ctx.channel(),NettyNetwork.this);
                        users.add(bindUser);
                        bindUser.setUserAuthentication(new UserAuthentication(bindUser,IOThreadPool,pluginManager,serverAPI));
                        bindUser.setPublicKey(protocol.getMessageBody().getMessage());
                        for (IUserAuthentication.UserRecall recall : LoginRecall)
                        {
                            Objects.requireNonNull(bindUser.getUserAuthentication()).RegisterLoginRecall(recall);
                        }
                        for (IUserAuthentication.UserRecall recall : DisconnectRecall)
                        {
                            Objects.requireNonNull(bindUser.getUserAuthentication()).RegisterLogoutRecall(recall);
                        }
                    }
                    else {
                        bindUser = status.bindUser;
                        bindUser.setPublicKey(protocol.getMessageBody().getMessage());
                    }
                    ClientStatus clientStatus = new ClientStatus(EncryptionMode.RSA_ENCRYPTION,RSA.decrypt(protocol.getMessageBody().getMessage(), ServerPrivateKey),bindUser);
                    ClientChannel.remove(ctx.channel());
                    ClientChannel.put(ctx.channel(),clientStatus);
                }
                case "AESEncryption" -> {
                    if (!status.encryptionMode.equals(EncryptionMode.RSA_ENCRYPTION))
                    {
                        ctx.writeAndFlush("The AESEncryption Set Mode is disabled on this time.\n");
                        return;
                    }
                    String RandomForServer = UUID.randomUUID().toString();
                    String RandomForClient = protocol.getMessageBody().getMessage();

                    protocol = new NormalProtocol();
                    NormalProtocol.MessageHead head = new NormalProtocol.MessageHead();
                    head.setType("AESEncryption");
                    head.setVersion(CodeDynamicConfig.getProtocolVersion());
                    protocol.setMessageHead(head);
                    NormalProtocol.MessageBody body = new NormalProtocol.MessageBody();
                    body.setMessage(RandomForServer);
                    protocol.setMessageBody(body);
                    SendData(gson.toJson(protocol),ctx.channel());

                    ClientStatus clientStatus = new ClientStatus(EncryptionMode.AES_ENCRYPTION,GenerateKey(RandomForServer+RandomForClient),status.bindUser);
                    ClientChannel.remove(ctx.channel());
                    ClientChannel.put(ctx.channel(),clientStatus);

                    clientStatus.bindUser.setUserAES(cn.hutool.crypto.SecureUtil.aes(
                            SecureUtil.generateKey(
                                    SymmetricAlgorithm.AES.getValue(), Base64.decodeBase64(clientStatus.encryptionKey)
                            ).getEncoded()
                    ));
                }
                case "options" -> {
                    if (protocol.getMessageBody().getMessage().equals("AllowedTransferProtocol:Enable"))
                    {
                        status.bindUser.setAllowedTransferProtocol(true);
                        protocol = new NormalProtocol();
                        NormalProtocol.MessageHead head = new NormalProtocol.MessageHead();
                        head.setVersion(CodeDynamicConfig.getProtocolVersion());
                        head.setType("options");
                        protocol.setMessageHead(head);
                        NormalProtocol.MessageBody body = new NormalProtocol.MessageBody();
                        body.setMessage("Accept");
                        body.setFileLong(0);
                        protocol.setMessageBody(body);
                        SendData(gson.toJson(protocol),ctx.channel());
                    }
                    else if (protocol.getMessageBody().getMessage().equals("AllowedTransferProtocol:Disabled"))
                    {
                        status.bindUser.setAllowedTransferProtocol(false);
                        protocol = new NormalProtocol();
                        NormalProtocol.MessageHead head = new NormalProtocol.MessageHead();
                        head.setVersion(CodeDynamicConfig.getProtocolVersion());
                        head.setType("options");
                        protocol.setMessageHead(head);
                        NormalProtocol.MessageBody body = new NormalProtocol.MessageBody();
                        body.setMessage("Accept");
                        body.setFileLong(0);
                        protocol.setMessageBody(body);
                        SendData(gson.toJson(protocol),ctx.channel());
                    }
                    else
                        ctx.writeAndFlush("This setting is not support on this Server!\n");
                }
                default -> ctx.writeAndFlush("This mode is not Support on this Server!\n");
            }
        }

        private void UserLoginRequestDispose(LoginProtocol loginProtocol, ClientStatus status) {
            if ("token".equals(loginProtocol.getLoginPacketHead().getType()))
            {
                if (!Objects.requireNonNull(status.bindUser.getUserAuthentication()).
                        DoLogin(loginProtocol.getLoginPacketBody().getReLogin().getToken()))
                    status.bindUser.UserDisconnect();
            }
            else if ("passwd".equals(loginProtocol.getLoginPacketHead().getType()))
            {
                if (!Objects.requireNonNull(status.bindUser.getUserAuthentication()).
                        DoLogin(loginProtocol.getLoginPacketBody().getNormalLogin().getUserName(),
                                loginProtocol.getLoginPacketBody().getNormalLogin().getPasswd()))
                    status.bindUser.UserDisconnect();
            }
            else
                status.bindUser.UserDisconnect();
        }

        @Override
        public void channelRead(ChannelHandlerContext ctx, Object msg) {
            try {
                ClientStatus status;
                if (!(msg instanceof String Msg) || Msg.isEmpty()) {
                    return;
                }

                String[] messages = Msg.replaceAll("\r","")
                        .split("\n");//有时，多个数据包会被合并，通过这种方式来分离数据包。为兼容linux，分隔符为\n，如果有\r就删除

                for (final String MSG : messages) {
                    String Message = UnicodeToString.unicodeToString(MSG);
                    status = ClientChannel.get(ctx.channel());
                    //一些明文的直接回复
                    switch (Message) {
                        case "Hello Server" -> {
                            ctx.writeAndFlush("Hello, Client\n");
                            continue;
                        }

                        case "你好，服务端" -> {
                            ctx.writeAndFlush("你好，客户端\n");
                            continue;
                        }

                        case "Alive" -> {
                            ctx.writeAndFlush("Alive\n");
                            continue;
                        }
                    }

                    if (status.encryptionMode.equals(EncryptionMode.NON_ENCRYPTION)) {
                        try {
                            UserRequestDispose(ctx, Message, status);
                        } catch (CryptoException e) {
                            ctx.writeAndFlush("Decryption Error\n");
                        }
                    } else if (status.encryptionMode.equals(EncryptionMode.RSA_ENCRYPTION)) {
                        try {
                            UserRequestDispose(ctx, RSA.decrypt(Message, ServerPrivateKey), status);
                        } catch (CryptoException e) {
                            ctx.writeAndFlush("Decryption Error\n");
                        }
                    } else if (status.encryptionMode.equals(EncryptionMode.AES_ENCRYPTION)) {
                        UserRequestDispose(
                                ctx,
                                cn.hutool.crypto.SecureUtil.aes(
                                        SecureUtil.generateKey(
                                                SymmetricAlgorithm.AES.getValue(), Base64.decodeBase64(status.encryptionKey)
                                        ).getEncoded()
                                ).decryptStr(Message),
                                status
                        );
                    } else {
                        throw new RuntimeException("The Encryption Mode is not Support!\n");
                    }
                }
            } catch (Throwable throwable)
            {
                ctx.writeAndFlush("Invalid Input! Connection will be close\n");
                ctx.close();
            }
        }

        @Override
        public void channelActive(ChannelHandlerContext ctx) throws Exception {
            ClientChannel.put(ctx.channel(),new ClientStatus(EncryptionMode.NON_ENCRYPTION,"",null));
            super.channelActive(ctx);
        }

        @Override
        public void channelInactive(ChannelHandlerContext ctx) throws Exception {
            if (ClientChannel.get(ctx.channel()).bindUser != null) {
                ClientChannel.get(ctx.channel()).bindUser.UserDisconnect();
                users.replaceAll(user -> {
                    if (ClientChannel.get(ctx.channel()).bindUser.equals(user))
                        return null;
                    else
                        return user;
                });
            }
            ClientChannel.remove(ctx.channel());
            super.channelInactive(ctx);
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
            ctx.writeAndFlush("Server have internal server error, Connection will be close\n");
            ctx.channel().close();
            super.exceptionCaught(ctx, cause);
        }
    }

    @TestOnly
    public static void main(String[] args) {
        System.out.println("请注意！此main函数仅用于netty服务器测试！不可用于正常使用！");
        NettyNetwork nettyNetwork = NettyNetwork.getNettyNetwork();
        nettyNetwork.RSA_KeyAutogenerate("./ServerRSAKey/Public.txt", "./ServerRSAKey/Private.txt", new Logger(null));
        try {
            nettyNetwork.StartChatRoomServerForNetty(10000, FileUtils.readFileToString(new File("./ServerRSAKey/Private.txt"), StandardCharsets.UTF_8));
        } catch (IOException e) {
            SaveStackTrace.saveStackTrace(e);
            throw new RuntimeException(e);
        }
    }
}
