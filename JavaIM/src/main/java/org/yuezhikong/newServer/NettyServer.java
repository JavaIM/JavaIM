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

import cn.hutool.crypto.CryptoException;
import cn.hutool.crypto.symmetric.AES;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.LineBasedFrameDecoder;
import io.netty.handler.codec.MessageToMessageDecoder;
import io.netty.handler.codec.MessageToMessageEncoder;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.handler.codec.string.StringEncoder;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.util.ReferenceCountUtil;
import org.apache.commons.codec.binary.Base64;
import org.jetbrains.annotations.NotNull;
import org.yuezhikong.CodeDynamicConfig;
import org.yuezhikong.GeneralMethod;
import org.yuezhikong.newServer.UserData.Authentication.IUserAuthentication;
import org.yuezhikong.newServer.UserData.Authentication.UserAuthentication;
import org.yuezhikong.newServer.UserData.tcpUser.NettyUser;
import org.yuezhikong.newServer.UserData.Permission;
import org.yuezhikong.newServer.UserData.tcpUser.tcpUser;
import org.yuezhikong.newServer.UserData.user;
import org.yuezhikong.newServer.api.NettyAPI;
import org.yuezhikong.newServer.api.api;
import org.yuezhikong.newServer.plugin.PluginManager;
import org.yuezhikong.newServer.plugin.SimplePluginManager;
import org.yuezhikong.newServer.plugin.event.events.ServerChatEvent;
import org.yuezhikong.utils.*;
import org.yuezhikong.utils.Protocol.LoginProtocol;
import org.yuezhikong.utils.Protocol.NormalProtocol;

import javax.security.auth.login.AccountNotFoundException;
import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.CharBuffer;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

public final class NettyServer extends GeneralMethod implements IServerMain{
    static {
        nettyNetwork = new NettyServer();
    }
    private static NettyServer nettyNetwork;
    private ExecutorService UserRequestThreadPool;
    private NettyServer() {
    }

    public static NettyServer getNettyNetwork() {
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
    private record ClientStatus(EncryptionMode encryptionMode, String encryptionKey, tcpUser bindUser) {}
    private final ConcurrentHashMap<Channel,ClientStatus> ClientChannel = new ConcurrentHashMap<>();
    /**
     * 向用户发送信息(包括加密)
     *
     * @param msg     信息
     * @param channel 消息通道
     */
    public void SendData(String msg, Channel channel)
    {
        checks.checkArgument(msg == null || msg.isEmpty(), "The Message is null or Empty!");
        checks.checkArgument(channel == null || !channel.isWritable(), "The Channel is not init or not Writable!");
        ClientStatus status = ClientChannel.get(channel);
        org.apache.logging.log4j.Logger debugLogger = org.apache.logging.log4j.LogManager.getLogger("Debug");
        if (status.encryptionMode.equals(EncryptionMode.NON_ENCRYPTION))
        {
            debugLogger.debug("正在发送数据 {}", msg);
            channel.writeAndFlush(msg);
        }
        else if (status.encryptionMode.equals(EncryptionMode.RSA_ENCRYPTION))
        {
            String data = RSA.encrypt(msg,status.encryptionKey);
            debugLogger.debug("正在发送数据 {}", data);
            channel.writeAndFlush(data);
        }
        else if (status.encryptionMode.equals(EncryptionMode.AES_ENCRYPTION))
        {
            String data = new AES(
                    "ECB",
                    "PKCS5Padding",
                    Base64.decodeBase64(status.encryptionKey)
            ).encryptBase64(msg);
            debugLogger.debug("正在发送数据 {}", data);
            channel.writeAndFlush(data);
        }
        else {
            throw new RuntimeException("The Encryption Mode is not Support!");
        }
    }


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
        checks.checkArgument(bindPort < 1 || bindPort > 65535, "The Port is not in the range of [0,65535]!");
        checks.checkArgument(ServerPrivateKey == null || ServerPrivateKey.isEmpty(), "The Server Private Key is null or Empty!");
        this.ServerPrivateKey = ServerPrivateKey;
        UserRequestThreadPool = Executors.newCachedThreadPool(new ThreadFactory() {
            private final AtomicInteger threadNumber = new AtomicInteger(1);
            @Override
            public Thread newThread(@NotNull Runnable r) {
                return new Thread(new ThreadGroup(Thread.currentThread().getThreadGroup(), "UserRequestDispose ThreadGroup"),
                        r,"UserRequestDispose Thread #"+threadNumber.getAndIncrement());
            }
        });

        pluginManager = new SimplePluginManager(this);

        serverAPI = new NettyAPI(this);

        EventLoopGroup bossGroup = new NioEventLoopGroup(1, new ThreadFactory() {
            private final AtomicInteger threadNumber = new AtomicInteger(1);
            @Override
            public Thread newThread(@NotNull Runnable r) {
                return new Thread(IOThreadGroup,
                        r,"Netty Boss Thread #"+threadNumber.getAndIncrement());
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
        try {
            ServerBootstrap bootstrap = new ServerBootstrap();
            bootstrap.group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .handler(new LoggingHandler(LogLevel.DEBUG))
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        public void initChannel(SocketChannel channel) {
                            ChannelPipeline pipeline = channel.pipeline();
                            pipeline.addLast(new StringDecoder(StandardCharsets.UTF_8));//IO
                            pipeline.addLast(new StringEncoder(StandardCharsets.UTF_8));
                            pipeline.addLast(new LineBasedFrameDecoder(100000000));
                            pipeline.addLast(new ServerInDecoder());
                            pipeline.addLast(new ServerOutEncoder());

                            pipeline.addLast(RecvMessageThreadPool,new ServerInHandler());//JavaIM逻辑
                        }
                    });

            future = bootstrap.bind(bindPort).sync();

            UserRequestThreadPool.execute(() -> {
                while (true)
                {
                    Scanner scanner = new Scanner(System.in);
                    String input = scanner.nextLine();
                    if (input.startsWith("/"))
                        ServerCommandSend(input);
                    else
                        ServerChatMessageSend(input);
                }
            });
            pluginManager.LoadPluginOnDirectory(new File("./plugins"));
            logger.info("服务器启动完成");
            future.channel().closeFuture().sync();
        } catch (InterruptedException e) {
            SaveStackTrace.saveStackTrace(e);
        } finally {
            RecvMessageThreadPool.shutdownGracefully();
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
            try {
                pluginManager.UnLoadAllPlugin();
            } catch (IOException ignored) {}
            IOThreadPool.shutdownNow();
            UserRequestThreadPool.shutdownNow();

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
        UserRequestThreadPool.shutdownNow();
        nettyNetwork = new NettyServer();
    }
    private final List<IUserAuthentication.UserRecall> LoginRecall = new ArrayList<>();
    private final List<IUserAuthentication.UserRecall> DisconnectRecall = new ArrayList<>();

    public void AddLoginRecall(IUserAuthentication.UserRecall recall)
    {
        checks.checkArgument(recall == null,"Recall is null");
        LoginRecall.add(recall);
    }

    public void AddDisconnectRecall(IUserAuthentication.UserRecall recall)
    {
        checks.checkArgument(recall == null,"Recall is null");
        DisconnectRecall.add(recall);
    }
    public void ServerChatMessageSend(String ChatMessage)
    {
        checks.checkArgument(ChatMessage == null || ChatMessage.isEmpty(),"ChatMessage is null or empty!");
        UserRequestThreadPool.execute(() -> {
            pluginManager.callEvent(new ServerChatEvent(getConsoleUser(),ChatMessage));
            ChatRequest.ChatRequestInput input = new ChatRequest.ChatRequestInput(getConsoleUser(),ChatMessage);
            getRequest().ChatFormat(input);
            getServerAPI().SendMessageToAllClient("[服务端消息] "+input.getChatMessage());
            getLogger().ChatMsg("[服务端消息] "+input.getChatMessage());
        });
    }
    public void ServerCommandSend(String Command)
    {
        checks.checkArgument(Command == null || Command.isEmpty(),"Command is null or empty!");
        checks.checkArgument(!Command.startsWith("/"),"Command is not start with /");
        if (Command.equals("/quit"))
        {
            getLogger().info("正在关闭服务器...");
            StopNettyChatRoom();
            return;
        }
        UserRequestThreadPool.execute(() -> getRequest().CommandRequest(
                new ChatRequest.ChatRequestInput(getConsoleUser(),Command)));
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

    private final user ConsoleUser = new NettyUser(true,this)
            .SetUserPermission(Permission.ADMIN);

    private final ThreadGroup IOThreadGroup = new ThreadGroup(Thread.currentThread().getThreadGroup(), "IO ThreadGroup");
    private final ExecutorService IOThreadPool = Executors.newCachedThreadPool(new ThreadFactory() {
        private final AtomicInteger threadNumber = new AtomicInteger(1);

        @Override
        public Thread newThread(@NotNull Runnable r) {
            return new Thread(IOThreadGroup,
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
        checks.checkArgument(User == null,"User is null");
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

    private class ServerInHandler extends ChannelInboundHandlerAdapter {
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
                ctx.writeAndFlush("This Json have Syntax Error");
                return;
            }
            if (protocol.getMessageHead().getVersion() != CodeDynamicConfig.getProtocolVersion())
            {
                ctx.writeAndFlush("Invalid Protocol Version,Connection will be close");
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
                        ctx.writeAndFlush("Invalid Mode! The ChangePassword Mode is disabled on this time.");
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
                        ctx.writeAndFlush("Invalid Mode! The Chat Mode is disabled on this time.");
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
                        ctx.writeAndFlush("The RSAEncryption Set Mode is disabled on this time.");
                        return;
                    }

                    tcpUser bindUser;
                    if (status.bindUser == null)
                    {
                        bindUser = new NettyUser(ctx.channel(), NettyServer.this,users.size() + 1);
                        users.add(bindUser);
                        bindUser.setUserAuthentication(new UserAuthentication(bindUser,IOThreadPool,pluginManager,serverAPI));
                        bindUser.addLoginRecall(User -> {
                            logger.info("用户："+User.getUserName()+"登录成功！");
                            for (user user : getServerAPI().GetValidClientList(true)) {
                                getServerAPI().SendMessageToUser(user,"用户：" + User.getUserName() + "加入了聊天!");
                            }
                        });
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
                    ClientStatus clientStatus = new ClientStatus(EncryptionMode.RSA_ENCRYPTION,protocol.getMessageBody().getMessage(),bindUser);
                    ClientChannel.remove(ctx.channel());
                    ClientChannel.put(ctx.channel(),clientStatus);
                }
                case "AESEncryption" -> {
                    if (!status.encryptionMode.equals(EncryptionMode.RSA_ENCRYPTION))
                    {
                        ctx.writeAndFlush("The AESEncryption Set Mode is disabled on this time.");
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

                    ClientStatus clientStatus = new ClientStatus(EncryptionMode.AES_ENCRYPTION,Base64.encodeBase64String(GenerateKey(RandomForServer,RandomForClient).getEncoded()),status.bindUser);
                    ClientChannel.remove(ctx.channel());
                    ClientChannel.put(ctx.channel(),clientStatus);

                    clientStatus.bindUser.setUserAES(new AES(
                            "ECB",
                            "PKCS5Padding",
                            Base64.decodeBase64(status.encryptionKey)
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
                        body.setMessage("AllowedTransferProtocol:Accept");
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
                        body.setMessage("AllowedTransferProtocol:Accept");
                        body.setFileLong(0);
                        protocol.setMessageBody(body);
                        SendData(gson.toJson(protocol),ctx.channel());
                    }
                    else
                        ctx.writeAndFlush("This setting is not support on this Server!");
                }
                case "Leave" -> {
                    if (status.bindUser != null
                            && status.bindUser.getUserAuthentication() != null
                            && status.bindUser.isUserLogined()) {
                        logger.info("用户:" +
                                status.bindUser.getUserName() + "("+ctx.channel().remoteAddress()+") 正在请求离线...");
                        ctx.channel().close();
                    }
                    else {
                        logger.info("客户端:"+ctx.channel().remoteAddress()+" 正在请求离线...");
                        ctx.channel().close();
                    }
                }
                default -> ctx.writeAndFlush("This mode is not Support on this Server!");
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

                String Message = UnicodeToString.unicodeToString(Msg);
                status = ClientChannel.get(ctx.channel());
                //一些明文的直接回复
                switch (Message) {
                    case "Hello Server" -> {
                        ctx.writeAndFlush("Hello, Client");
                        return;
                    }

                    case "你好，服务端" -> {
                        ctx.writeAndFlush("你好，客户端");
                        return;
                    }

                    case "Alive" -> {
                        ctx.writeAndFlush("Alive");
                        return;
                    }
                }

                if (status.encryptionMode.equals(EncryptionMode.NON_ENCRYPTION)) {
                    try {
                        UserRequestDispose(ctx, Message, status);
                    } catch (CryptoException e) {
                        ctx.writeAndFlush("Decryption Error");
                    }
                }
                else if (status.encryptionMode.equals(EncryptionMode.RSA_ENCRYPTION)) {
                    try {
                        UserRequestDispose(ctx, RSA.decrypt(Message, ServerPrivateKey), status);
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
                            ).decryptStr(Message),
                            status
                    );
                }
                else {
                    ctx.writeAndFlush("The Encryption Mode is not Support!");
                    ctx.channel().close();
                }
            } catch (Throwable throwable)
            {
                ctx.writeAndFlush("Invalid Input! Connection will be close");
                ctx.channel().close();
            }
            finally {
                ReferenceCountUtil.release(msg);
            }
        }

        @Override
        public void channelActive(ChannelHandlerContext ctx) throws Exception {
            ClientChannel.put(ctx.channel(),new ClientStatus(EncryptionMode.NON_ENCRYPTION,"",null));
            InetSocketAddress remoteAddress = (InetSocketAddress) ctx.channel().remoteAddress();
            logger.info("客户端:"+ remoteAddress.getAddress()+"已经连接到服务器");
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
            InetSocketAddress remoteAddress = (InetSocketAddress) ctx.channel().remoteAddress();
            logger.info("客户端:"+ remoteAddress.getAddress()+"已经断开连接");
            super.channelInactive(ctx);
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
            super.exceptionCaught(ctx, cause);
            ctx.writeAndFlush("Server have internal server error, Connection will be close");
            if (ctx.channel().isActive())
                ctx.close();
        }
    }

    private static class ServerOutEncoder extends MessageToMessageEncoder<CharSequence>
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

    private static class ServerInDecoder extends MessageToMessageDecoder<CharSequence>
    {
        @Override
        protected void decode(ChannelHandlerContext ctx, CharSequence msg, List<Object> out) {
            out.add(msg.toString().replaceAll("\r","").replaceAll("\n",""));
        }
    }
}
