package org.yuezhikong.Server.network;

import cn.hutool.crypto.KeyUtil;
import cn.hutool.crypto.SecureUtil;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.DecoderException;
import io.netty.handler.codec.LineBasedFrameDecoder;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.handler.codec.string.StringEncoder;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.handler.ssl.ClientAuth;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.SslProvider;
import io.netty.util.ReferenceCountUtil;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.FileUtils;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x500.X500NameBuilder;
import org.bouncycastle.asn1.x500.style.BCStyle;
import org.bouncycastle.asn1.x509.*;
import org.bouncycastle.cert.CertIOException;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.X509v3CertificateBuilder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509CertificateHolder;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.yuezhikong.CodeDynamicConfig;
import org.yuezhikong.Server.Server;
import org.yuezhikong.Server.UserData.Authentication.IUserAuthentication;
import org.yuezhikong.Server.UserData.Permission;
import org.yuezhikong.Server.UserData.tcpUser.tcpUser;
import org.yuezhikong.Server.UserData.user;
import org.yuezhikong.utils.Logger;
import org.yuezhikong.utils.SaveStackTrace;
import org.yuezhikong.utils.checks;

import javax.net.ssl.SSLException;
import javax.net.ssl.SSLHandshakeException;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.net.SocketAddress;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

public class SSLNettyServer implements NetworkServer {
    private final List<NetworkClient> clientList = new ArrayList<>();
    private PrivateKey ServerSSLPrivateKey;
    private X509Certificate ServerSSLCertificate;
    private final Logger logger = Logger.getInstance();

    private Server JavaIMServer;

    private ChannelFuture future;

    private boolean isRunning = false;
    @Override
    public void start(int ListenPort) throws IllegalStateException {
        checks.checkArgument(ListenPort < 1 || ListenPort > 65535, "The Port is not in the range of [0,65535]!");
        if (isRunning)
            throw new IllegalStateException("The Server is already running!");
        isRunning = true;
        logger.info("正在启动网络层 JavaIM...");
        logger.info("正在生成 X.509 SSL证书");
        X509CertificateGenerate();

        logger.info("正在创建各线程池");
        ThreadGroup IOThreadGroup = new ThreadGroup("JavaIM IO Thread");
        IOThreadPool = Executors.newCachedThreadPool(new ThreadFactory() {
            private final AtomicInteger threadNumber = new AtomicInteger(1);
            @Override
            public Thread newThread(@NotNull Runnable r) {
                return new Thread(IOThreadGroup,
                        r,"IO Thread #"+threadNumber.getAndIncrement());
            }
        });
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

        logger.info("正在启动Netty");
        try {
            ServerBootstrap bootstrap = new ServerBootstrap();
            bootstrap.group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .handler(new LoggingHandler(LogLevel.DEBUG))
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        public void initChannel(SocketChannel channel) {
                            ChannelPipeline pipeline = channel.pipeline();
                            try {
                                pipeline.addLast(
                                        SslContextBuilder.forServer(ServerSSLPrivateKey,ServerSSLCertificate)
                                                .sslProvider(SslProvider.JDK)
                                                .clientAuth(ClientAuth.NONE)
                                                .build()
                                                .newHandler(channel.alloc())
                                );
                            } catch (SSLException e) {
                                throw new RuntimeException("SSL Context Generate Failed!",e);
                            }
                            pipeline.addLast(new LineBasedFrameDecoder(100000000));
                            pipeline.addLast(new StringDecoder(StandardCharsets.UTF_8));//IO
                            pipeline.addLast(new StringEncoder(StandardCharsets.UTF_8));

                            pipeline.addLast(RecvMessageThreadPool,new ServerInHandler());//JavaIM逻辑
                        }
                    });

            future = bootstrap.bind(ListenPort).sync();
            logger.info("JavaIM网络层启动完成");
            if (Server.getInstance() == null)
            {
                JavaIMServer = new Server(this);
            }
            else {
                future.channel().close();
                throw new RuntimeException("JavaIM Start Failed, Application JavaIM is already running");
            }
            future.channel().closeFuture().sync();
        } catch (InterruptedException e) {
            SaveStackTrace.saveStackTrace(e);
        } finally {
            RecvMessageThreadPool.shutdownGracefully();
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
        }
    }

    private void X509CertificateGenerate()
    {
        // 生成X509证书
        if (!(new File("./ServerEncryption/cert.crt").exists() &&
                new File("./ServerEncryption/private.key").exists())) {
            logger.info("正在生成 X.509 CA 证书");
            X500Name subject = new X500NameBuilder()
                    .addRDN(BCStyle.C, "CN")//证书国家代号(Country Name)
                    .addRDN(BCStyle.O, "JavaIM-Server")//证书组织名(Organization Name)
                    .addRDN(BCStyle.OU, CodeDynamicConfig.ServerName)//证书组织单位名(Organization Unit Name)
                    .addRDN(BCStyle.CN, "JavaIM Server("+CodeDynamicConfig.ServerName+") CA")
                    .addRDN(BCStyle.ST, "Beijing")//证书州或省份(State or Province Name);//证书通用名(Common Name)
                    .addRDN(BCStyle.L, "Beijing")//证书所属城市名(Locality Name)
                    .build();
            // 创建密钥对
            KeyPair pair = SecureUtil.generateKeyPair("RSA",2048);
            PublicKey publicKey = pair.getPublic();
            PrivateKey privateKey = pair.getPrivate();
            // 根据规范将privateKey写入到文件
            byte[] privateKeyEncode = privateKey.getEncoded();
            String privateKeyContent =
                    Base64.encodeBase64String(privateKeyEncode);
            try {
                FileUtils.writeStringToFile(new File("./ServerEncryption/private.key"), privateKeyContent, StandardCharsets.UTF_8);
            } catch (IOException e) {
                throw new RuntimeException("Failed to Write private key,Permission denied?",e);
            }
            // 自签名X509证书
            long currentTimeMillis = System.currentTimeMillis();
            ContentSigner signer;
            try {
                signer = new JcaContentSignerBuilder("SHA256WITHRSA").build(privateKey);
            } catch (OperatorCreationException e) {
                throw new RuntimeException("Generate Content Signer Failed!",e);
            }
            X509CertificateHolder certHolder = new X509v3CertificateBuilder(
                    subject,//证书签发者
                    BigInteger.valueOf(currentTimeMillis),//证书序列号
                    new Date(currentTimeMillis),//证书生效时间
                    new Date(currentTimeMillis + (long) 365*24*60*60*1000),//证书失效时间
                    subject,//证书主体
                    SubjectPublicKeyInfo.getInstance(publicKey.getEncoded())//证书主体公钥
            ).build(signer);
            Certificate certificate = certHolder.toASN1Structure();
            // 将证书写入文件
            try {
                byte[] certificateEncode = certificate.getEncoded();
                String certificateContent =
                        "-----BEGIN CERTIFICATE-----\n"+
                                lf(Base64.encodeBase64String(certificateEncode),64)+
                                "-----END CERTIFICATE-----";
                FileUtils.writeStringToFile(new File("./ServerEncryption/cert.crt"), certificateContent, StandardCharsets.UTF_8);
            } catch (IOException e) {
                throw new RuntimeException("Failed to write cert to file, Permission denied?",e);
            }
            logger.info("CA证书创建完成");
            logger.info("请分发ServerEncryption文件夹中的cert.crt到各个客户端");
            logger.info("请注意，ServerEncryption文件夹中的“private.key”请保护好");
            logger.info("此文件如果泄露，您与客户端的连接将可能被劫持");
        }
        // 通过CA证书签署临时SSL证书

        logger.info("正在使用 X.509 CA 证书 签发新的 X.509 临时 SSL 加密证书");
        // 加载CA证书
        Certificate caCert;
        PrivateKey caPrivateKey;
        try (FileInputStream stream = new FileInputStream("./ServerEncryption/cert.crt")){
            CertificateFactory factory = CertificateFactory.getInstance("X.509","BC");
            caCert = new JcaX509CertificateHolder((X509Certificate) factory.generateCertificate(stream)).toASN1Structure();
            caPrivateKey = KeyUtil.generatePrivateKey("RSA",
                    Base64.decodeBase64(
                            FileUtils.readFileToString(new File("./ServerEncryption/private.key"),StandardCharsets.UTF_8)
                    )
            );
        } catch (CertificateException | NoSuchProviderException | IOException e) {
            throw new RuntimeException("Failed to open X.509 CA Cert & X.509 RSA Private key, Permission denied?",e);
        }

        // 创建终端主体
        X500Name subject = new X500NameBuilder()
                .addRDN(BCStyle.C, "CN")//证书国家代号(Country Name)
                .addRDN(BCStyle.O, "JavaIM-Server")//证书组织名(Organization Name)
                .addRDN(BCStyle.OU, CodeDynamicConfig.ServerName)//证书组织单位名(Organization Unit Name)
                .addRDN(BCStyle.CN, "JavaIM Server Encryption")//证书通用名(Common Name)
                .addRDN(BCStyle.ST, "Shanghai")//证书州或省份(State or Province Name)
                .addRDN(BCStyle.L, "Shanghai")//证书所属城市名(Locality Name)
                .build();

        // 创建密钥对
        KeyPair pair = SecureUtil.generateKeyPair("RSA",2048);
        PublicKey publicKey = pair.getPublic();
        ServerSSLPrivateKey = pair.getPrivate();

        // 签署并生成证书
        long currentTimeMillis = System.currentTimeMillis();
        ContentSigner signer;
        try {
            signer = new JcaContentSignerBuilder("SHA256WITHRSA").build(caPrivateKey);
        } catch (OperatorCreationException e) {
            throw new RuntimeException("Generate Content Signer Failed!",e);
        }
        try {
            ServerSSLCertificate = new JcaX509CertificateConverter().getCertificate(
                    new X509v3CertificateBuilder(
                            caCert.getSubject(),//证书签发者
                            BigInteger.valueOf(currentTimeMillis),//证书序列号
                            new Date(currentTimeMillis),//证书生效时间
                            new Date(currentTimeMillis + (long) 365*24*60*60*1000),//证书失效时间
                            subject,//证书主体
                            SubjectPublicKeyInfo.getInstance(publicKey.getEncoded())//证书主体公钥
                    )
                            .addExtension(Extension.keyUsage, true, new KeyUsage(KeyUsage.digitalSignature | KeyUsage.keyEncipherment))
                            .addExtension(Extension.extendedKeyUsage, false, new ExtendedKeyUsage(new KeyPurposeId[]{KeyPurposeId.id_kp_serverAuth, KeyPurposeId.id_kp_clientAuth}))
                            .addExtension(Extension.basicConstraints, true, new BasicConstraints(false))
                            .build(signer)
            );
        } catch (CertIOException | CertificateException e) {
            throw new RuntimeException("Generate SSL Cert Failed!",e);
        }
        logger.info("X.509 SSL 证书已经签发完成");
    }

    private static String lf(String str, int length) {
        StringBuilder builder = new StringBuilder();
        char[] chars = str.toCharArray();
        int count = 0;
        for (char c : chars) {
            builder.append(c);
            count++;
            if (count == length) {
                builder.append("\n");
                count = 0;
            }
        }
        if (count != 0) {
            builder.append("\n");
        }
        return builder.toString();
    }

    @Override
    public NetworkClient[] getOnlineClients() {
        return clientList.toArray(new NetworkClient[0]);
    }

    @Override
    public boolean isRunning() {
        return isRunning;
    }

    @Override
    public void stop() throws IllegalStateException {
        if (!isRunning()) {
            throw new IllegalStateException("Server is not running");
        }
        logger.info("Server is stopping...");
        future.channel().close();
        logger.info("Server stopped");
    }

    private ExecutorService IOThreadPool;
    @Override
    public ExecutorService getIOThreadPool() {
        return IOThreadPool;
    }

    private class ServerInHandler extends ChannelInboundHandlerAdapter {
        private final HashMap<Channel,NetworkClient> clientNetworkClientPair = new HashMap<>();
        @Override
        public void channelActive(ChannelHandlerContext ctx) throws Exception {
            NettyUser nettyUser = new NettyUser();
            JavaIMServer.RegisterUser(nettyUser);
            NetworkClient client = new NettyNetworkClient(nettyUser,ctx.channel().remoteAddress(),ctx.channel());
            nettyUser.setNetworkClient(client);
            clientNetworkClientPair.put(ctx.channel(),client);
            clientList.add(client);
            logger.info("检测到新客户端连接...");
            logger.info("此客户端IP地址："+ctx.channel().remoteAddress());
        }

        @Override
        public void channelInactive(ChannelHandlerContext ctx) throws Exception {
            NetworkClient thisClient = clientNetworkClientPair.remove(ctx.channel());
            JavaIMServer.UnRegisterUser(thisClient.getUser());
            clientList.remove(thisClient);
            logger.info("检测到客户端离线...");
            logger.info("此客户端IP地址："+thisClient.getSocketAddress());
        }

        @Override
        public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
            try {
                if (!(msg instanceof String Msg)) {
                    logger.info(String.format("客户端：%s 发送了非String消息：%s", ctx.channel().remoteAddress(), msg.toString()));
                    return;
                }
                NetworkClient thisClient = clientNetworkClientPair.remove(ctx.channel());
                JavaIMServer.onReceiveMessage(thisClient, Msg);
            } catch (Throwable throwable) {
                logger.warning(String.format("客户端：%s 处理程序出错！", ctx.channel().remoteAddress()));
                logger.warning("错误为："+throwable.getMessage());
                SaveStackTrace.saveStackTrace(throwable);
            } finally {
                ReferenceCountUtil.release(msg);
            }
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
            if (cause instanceof DecoderException)
            {
                Throwable exceptionCause = cause.getCause();
                if (exceptionCause instanceof SSLHandshakeException)
                {
                    logger.warning(String.format("客户端：%s 因为SSL错误：%s已断开连接",ctx.channel().remoteAddress(),exceptionCause.getMessage()));
                    return;
                }
            }
            SaveStackTrace.saveStackTrace(cause);
            logger.warning(String.format("客户端：%s 因为：%s 已经断开连接",ctx.channel().remoteAddress(),cause.getMessage()));
            ctx.channel().close();
        }
    }

    private class NettyNetworkClient implements NetworkClient
    {
        private final tcpUser user;
        private final SocketAddress address;
        private final Channel channel;
        private NettyNetworkClient(tcpUser user, SocketAddress address, Channel channel)
        {
            this.user = user;
            this.address = address;
            this.channel = channel;
        }

        @Override
        public SocketAddress getSocketAddress() {
            return address;
        }

        @Override
        public void send(String message) throws IllegalStateException {
            checks.checkState(!isOnline(),"This user is now offline!");
            channel.writeAndFlush(message);
        }

        @Override
        public boolean isOnline() {
            return clientList.contains(this);
        }

        @Override
        public void disconnect() {
            if (isOnline())
                channel.disconnect();
        }

        @Override
        public tcpUser getUser() {
            return user;
        }
    }

    private class NettyUser implements tcpUser{
        private NetworkClient client;
        private IUserAuthentication authentication;

        private Permission permission;

        /**
         * 设置网络层客户端
         * @param client 客户端
         */
        private void setNetworkClient(NetworkClient client) {
            this.client = client;
        }
        @Override
        public NetworkClient getNetworkClient() {
            return client;
        }

        @Override
        public String getUserName() {
            return (authentication == null) ? "" : authentication.getUserName();
        }

        @Override
        public user UserLogin(String UserName) {
            if (!JavaIMServer.RegisterUser(this))
            {
                throw new RuntimeException("Register User Failed");
            }
            return this;
        }

        @Override
        public boolean isUserLogged() {
            return authentication != null && authentication.isLogin();
        }

        @Override
        public user UserDisconnect() {
            JavaIMServer.UnRegisterUser(this);
            client.disconnect();
            return this;
        }

        @Override
        public user SetUserPermission(int permissionLevel, boolean FlashPermission) {
            if (!FlashPermission)
                logger.info(String.format("用户：%s的权限发生更新，新权限等级：%s",getUserName(),permissionLevel));
            permission = Permission.ToPermission(permissionLevel);
            return this;
        }

        @Override
        public user SetUserPermission(Permission permission) {
            this.permission = permission;
            return this;
        }

        @Override
        public Permission getUserPermission() {
            return permission;
        }

        @Override
        public boolean isServer() {
            return false;
        }

        @Override
        public boolean isAllowedTransferProtocol() {
            return false;//当前版本暂不支持
        }

        @Override
        public user setAllowedTransferProtocol(boolean allowedTransferProtocol) {
            throw new UnsupportedOperationException("This version of JavaIM NetworkServer not support TransferProtocol");
        }

        @Override
        public user addLoginRecall(IUserAuthentication.UserRecall code) {
            checks.checkState(authentication == null,"Authentication is not init");
            authentication.RegisterLoginRecall(code);
            return this;
        }

        @Override
        public user addDisconnectRecall(IUserAuthentication.UserRecall code) {
            checks.checkState(authentication == null,"Authentication is not init");
            authentication.RegisterLogoutRecall(code);
            return this;
        }

        @Override
        public user setUserAuthentication(@Nullable IUserAuthentication Authentication) {
            authentication = Authentication;
            return this;
        }

        @Override
        public @Nullable IUserAuthentication getUserAuthentication() {
            return authentication;
        }
    }
}
