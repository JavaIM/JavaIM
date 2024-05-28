package org.yuezhikong.Server.network;

import com.google.gson.Gson;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.DecoderException;
import io.netty.handler.codec.LineBasedFrameDecoder;
import io.netty.handler.codec.MessageToMessageEncoder;
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
import org.bouncycastle.asn1.x509.Certificate;
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
import org.jetbrains.annotations.Range;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yuezhikong.CodeDynamicConfig;
import org.yuezhikong.Server.ServerTools;
import org.yuezhikong.Server.UserData.JavaUser;
import org.yuezhikong.Server.UserData.tcpUser.tcpUser;
import org.yuezhikong.Server.UserData.user;
import org.yuezhikong.utils.Protocol.GeneralProtocol;
import org.yuezhikong.utils.Protocol.SystemProtocol;
import org.yuezhikong.utils.SaveStackTrace;
import org.yuezhikong.utils.checks;

import javax.net.ssl.SSLException;
import javax.net.ssl.SSLHandshakeException;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.net.SocketAddress;
import java.nio.CharBuffer;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

public class SSLNettyServer implements NetworkServer {
    private final List<NetworkClient> clientList = new ArrayList<>();
    private PrivateKey ServerSSLPrivateKey;
    private X509Certificate ServerSSLCertificate;
    private static Logger logger;



    private ChannelFuture future;

    private boolean isRunning = false;
    private boolean run = false;
    @Override
    public void start(@Range(from = 1, to = 65535) int ListenPort, ExecutorService StartUpThreadPool) throws IllegalStateException {
        if (run)
            throw new IllegalStateException("The Server is already running!");
        run = true;

        logger = LoggerFactory.getLogger(SSLNettyServer.class);// Netty Server Logger

        logger.info("正在启动网络层 JavaIM...");

        Future<?> CACertTask = StartUpThreadPool.submit(() -> {
            logger.info("正在生成 X.509 SSL证书");
            X509CertificateGenerate();
        });


        record NettyThreadPoolTaskReturn(EventLoopGroup bossGroup, EventLoopGroup workerGroup,DefaultEventLoopGroup RecvMessageThreadPool) {}
        Future<?> NettyThreadPoolTask = StartUpThreadPool.submit(() -> {
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
            EventLoopGroup bossGroup = new NioEventLoopGroup(2, new ThreadFactory() {
                private final AtomicInteger threadNumber = new AtomicInteger(1);
                @Override
                public Thread newThread(@NotNull Runnable r) {
                    return new Thread(IOThreadGroup,
                            r,"Netty Boss Thread #"+threadNumber.getAndIncrement());
                }
            });
            EventLoopGroup workerGroup = new NioEventLoopGroup(10,new ThreadFactory() {
                private final AtomicInteger threadNumber = new AtomicInteger(1);
                @Override
                public Thread newThread(@NotNull Runnable r) {
                    return new Thread(IOThreadGroup,
                            r,"Netty Worker Thread #"+threadNumber.getAndIncrement());
                }
            });
            DefaultEventLoopGroup RecvMessageThreadPool = new DefaultEventLoopGroup(10,new ThreadFactory() {
                private final AtomicInteger threadNumber = new AtomicInteger(1);
                @Override
                public Thread newThread(@NotNull Runnable r) {
                    return new Thread(new ThreadGroup(Thread.currentThread().getThreadGroup(), "Recv Message Thread Group"),
                            r,"Recv Message Thread #"+threadNumber.getAndIncrement());
                }
            });
            return new NettyThreadPoolTaskReturn(bossGroup, workerGroup, RecvMessageThreadPool);
        });

        EventLoopGroup bossGroup,workerGroup;
        DefaultEventLoopGroup RecvMessageThreadPool;
        try {
            CACertTask.get();
            NettyThreadPoolTaskReturn taskReturn = (NettyThreadPoolTaskReturn) NettyThreadPoolTask.get();
            bossGroup = taskReturn.bossGroup();
            workerGroup = taskReturn.workerGroup();
            RecvMessageThreadPool = taskReturn.RecvMessageThreadPool();
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException("Thread Pool Fatal",e);
        }

        logger.info("正在启动Netty");
        try {
            ServerBootstrap bootstrap = new ServerBootstrap();
            bootstrap.group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .handler(new LoggingHandler(LogLevel.DEBUG)) // Channel Debug等级日志记录器，用于调试
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
                            pipeline.addLast(new LoggingHandler(LogLevel.DEBUG));
                            pipeline.addLast(new LineBasedFrameDecoder(Integer.MAX_VALUE));
                            pipeline.addLast(new StringDecoder(StandardCharsets.UTF_8));//IO
                            pipeline.addLast(new StringEncoder(StandardCharsets.UTF_8));
                            pipeline.addLast(new MessageToMessageEncoder<CharSequence>() {
                                @Override
                                protected void encode(ChannelHandlerContext ctx, CharSequence msg, List<Object> out) {
                                    out.add(CharBuffer.wrap(msg+"\n"));
                                }
                            });// 每行消息添加换行符
                            pipeline.addLast(RecvMessageThreadPool,new ServerInHandler());//JavaIM逻辑
                        }
                    });

            future = bootstrap.bind(ListenPort).sync();
            logger.info("JavaIM网络层启动完成");
            synchronized (SSLNettyServer.class) {
                SSLNettyServer.class.notifyAll();
            }
            isRunning = true;
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
                    .addRDN(BCStyle.CN, "JavaIM Server("+CodeDynamicConfig.ServerName+") CA")//证书通用名(Common Name)
                    .addRDN(BCStyle.ST, "Beijing")//证书州或省份(State or Province Name);
                    .addRDN(BCStyle.L, "Beijing")//证书所属城市名(Locality Name)
                    .build();
            // 创建密钥对
            KeyPair pair;
            try {
                KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
                generator.initialize(2048);
                pair = generator.generateKeyPair();
            } catch (NoSuchAlgorithmException e) {
                throw new RuntimeException("Failed to generate keypair!", e);
            }
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
            // 将证书写入文件
            try {
                X509CertificateHolder certHolder = new X509v3CertificateBuilder(
                        subject,//证书签发者
                        BigInteger.valueOf(currentTimeMillis),//证书序列号
                        new Date(currentTimeMillis),//证书生效时间
                        new Date(currentTimeMillis + (long) 365*24*60*60*1000),//证书失效时间
                        subject,//证书主体
                        SubjectPublicKeyInfo.getInstance(publicKey.getEncoded())//证书主体公钥
                )
                        .addExtension(Extension.basicConstraints, true, new BasicConstraints(true))
                        .build(signer);
                Certificate certificate = certHolder.toASN1Structure();
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
            caPrivateKey = KeyFactory.getInstance("RSA").generatePrivate(
                    new PKCS8EncodedKeySpec(
                            Base64.decodeBase64(
                                    FileUtils.readFileToString(new File("./ServerEncryption/private.key"),StandardCharsets.UTF_8)
                            )
                    )
            );
        } catch (CertificateException | NoSuchProviderException | NoSuchAlgorithmException | InvalidKeySpecException | IOException e) {
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
        KeyPair pair;
        try {
            KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
            generator.initialize(2048);
            pair = generator.generateKeyPair();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Failed to generate keypair!", e);
        }
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
        logger.info("JavaIM 网络层正在关闭...");
        future.channel().close();
        logger.info("服务器关闭完成");
    }

    private ExecutorService IOThreadPool;
    @Override
    public ExecutorService getIOThreadPool() {
        return IOThreadPool;
    }

    private class ServerInHandler extends ChannelInboundHandlerAdapter {
        private final HashMap<Channel,NetworkClient> clientNetworkClientPair = new HashMap<>();
        private final Gson gson = new Gson();
        @Override
        public void channelActive(ChannelHandlerContext ctx) {
            logger.info("检测到新客户端连接...");
            logger.info("此客户端IP地址："+ctx.channel().remoteAddress());
            if (!ServerTools.getServerInstance().isServerCompleateStart()) {
                SystemProtocol systemProtocol = new SystemProtocol();
                systemProtocol.setType("Error");
                systemProtocol.setMessage("Server is not start completely");

                GeneralProtocol protocol = new GeneralProtocol();
                protocol.setProtocolVersion(CodeDynamicConfig.getProtocolVersion());
                protocol.setProtocolName("SystemProtocol");
                protocol.setProtocolData(gson.toJson(systemProtocol));
                ctx.writeAndFlush(gson.toJson(protocol));
                return;
            }
            NettyUser nettyUser = new NettyUser();
            if (!ServerTools.getServerInstanceOrThrow().RegisterUser(nettyUser))
            {
                ctx.channel().close();
                return;
            }
            NetworkClient client = new NettyNetworkClient(nettyUser,ctx.channel().remoteAddress(),ctx.channel());
            nettyUser.setNetworkClient(client);
            clientNetworkClientPair.put(ctx.channel(),client);
            clientList.add(client);
        }

        @Override
        public void channelInactive(ChannelHandlerContext ctx) {
            logger.info("检测到客户端离线...");
            logger.info("此客户端IP地址："+ctx.channel().remoteAddress());
            NetworkClient thisClient = clientNetworkClientPair.remove(ctx.channel());
            if (thisClient == null)
                return;
            ServerTools.getServerInstanceOrThrow().UnRegisterUser(thisClient.getUser());
            clientList.remove(thisClient);
        }

        @Override
        public void channelRead(ChannelHandlerContext ctx, Object msg) {
            try {
                if (!(msg instanceof String Msg)) {
                    logger.info(String.format("客户端：%s 发送了非String消息：%s", ctx.channel().remoteAddress(), msg.toString()));
                    return;
                }
                if (Msg.isEmpty())
                {
                    SystemProtocol systemProtocol = new SystemProtocol();
                    systemProtocol.setType("Error");
                    systemProtocol.setMessage("Empty Packet");

                    GeneralProtocol protocol = new GeneralProtocol();
                    protocol.setProtocolVersion(CodeDynamicConfig.getProtocolVersion());
                    protocol.setProtocolName("SystemProtocol");
                    protocol.setProtocolData(gson.toJson(systemProtocol));
                    ctx.writeAndFlush(gson.toJson(protocol));
                    return;
                }
                NetworkClient thisClient = clientNetworkClientPair.get(ctx.channel());
                ServerTools.getServerInstanceOrThrow().onReceiveMessage(thisClient, Msg);
            } catch (Throwable throwable) {
                logger.warn(String.format("客户端：%s 处理程序出错！", ctx.channel().remoteAddress()));
                logger.warn("错误为："+throwable.getMessage());
                SaveStackTrace.saveStackTrace(throwable);

                SystemProtocol systemProtocol = new SystemProtocol();
                systemProtocol.setType("Error");
                systemProtocol.setMessage("uncaught exception");

                GeneralProtocol protocol = new GeneralProtocol();
                protocol.setProtocolVersion(CodeDynamicConfig.getProtocolVersion());
                protocol.setProtocolName("SystemProtocol");
                protocol.setProtocolData(gson.toJson(systemProtocol));
                ctx.writeAndFlush(gson.toJson(protocol));
            } finally {
                ReferenceCountUtil.release(msg);
            }
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
            if (cause instanceof DecoderException)
            {
                Throwable exceptionCause = cause.getCause();
                if (exceptionCause instanceof SSLHandshakeException)
                {
                    logger.warn(String.format("客户端：%s 因为SSL错误：%s已断开连接",ctx.channel().remoteAddress(),exceptionCause.getMessage()));
                    return;
                }
            }
            SaveStackTrace.saveStackTrace(cause);
            logger.warn(String.format("客户端：%s 因为：%s 已经断开连接",ctx.channel().remoteAddress(),cause.getMessage()));
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
    private static class NettyUser extends JavaUser implements tcpUser{
        private NetworkClient client;
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
        public boolean isServer() {
            return false;
        }

        @Override
        public user onUserLogin(String UserName) {
            logger.info(String.format("用户：%s(IP地址：%s) 登录完成",UserName,getNetworkClient().getSocketAddress()));
            return super.onUserLogin(UserName);
        }

        @Override
        public user UserDisconnect() {
            getNetworkClient().disconnect();
            return super.UserDisconnect();
        }

        @Override
        public boolean isAllowedTransferProtocol() {
            return false;//当前版本暂不支持
        }

        @Override
        public user setAllowedTransferProtocol(boolean allowedTransferProtocol) {
            throw new UnsupportedOperationException("This version of JavaIM NetworkServer not support TransferProtocol");
        }
    }
}
