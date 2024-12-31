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
import lombok.extern.slf4j.Slf4j;
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
import org.yuezhikong.SystemConfig;
import org.yuezhikong.Server.ServerTools;
import org.yuezhikong.Server.userData.users.JavaUser;
import org.yuezhikong.Server.userData.users.NetworkUser;
import org.yuezhikong.Server.userData.user;
import org.yuezhikong.utils.protocol.GeneralProtocol;
import org.yuezhikong.utils.protocol.SystemProtocol;
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
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
public class SSLNettyServer implements NetworkServer {
    private final List<NetworkClient> clientList = new ArrayList<>();
    private PrivateKey ServerSSLPrivateKey;
    private X509Certificate ServerSSLCertificate;
    private ChannelFuture future;
    private EventLoopGroup bossGroup, workerGroup;
    private DefaultEventLoopGroup RecvMessageThreadPool;

    private boolean isRunning = false;

    @Override
    public void start(@Range(from = 1, to = 65535) int ListenPort, ExecutorService IOThreadPool, ExecutorService StartUpThreadPool) throws IllegalStateException {
        if (isRunning)
            throw new IllegalStateException("The Server is already running!");
        isRunning = true;
        log.info("正在启动网络层 JavaIM...");

        Future<?> CACertTask = StartUpThreadPool.submit(() -> {
            log.info("正在生成 X.509 SSL证书");
            X509CertificateGenerate();
        });

        record NettyThreadPoolTaskReturn(EventLoopGroup bossGroup, EventLoopGroup workerGroup,
                                         DefaultEventLoopGroup RecvMessageThreadPool) {
        }
        Future<?> NettyThreadPoolTask = StartUpThreadPool.submit(() -> {
            log.info("正在创建各线程池");
            EventLoopGroup bossGroup = new NioEventLoopGroup(2, new ThreadFactory() {
                private final AtomicInteger threadNumber = new AtomicInteger(1);

                @Override
                public Thread newThread(@NotNull Runnable r) {
                    return new Thread(ServerTools.getServerInstanceOrThrow().getServerThreadGroup(),
                            r, "Netty Boss Thread #" + threadNumber.getAndIncrement());
                }
            });
            EventLoopGroup workerGroup = new NioEventLoopGroup(10, new ThreadFactory() {
                private final AtomicInteger threadNumber = new AtomicInteger(1);

                @Override
                public Thread newThread(@NotNull Runnable r) {
                    return new Thread(ServerTools.getServerInstanceOrThrow().getServerThreadGroup(),
                            r, "Netty Worker Thread #" + threadNumber.getAndIncrement());
                }
            });
            DefaultEventLoopGroup RecvMessageThreadPool = new DefaultEventLoopGroup(10, new ThreadFactory() {
                private final AtomicInteger threadNumber = new AtomicInteger(1);

                @Override
                public Thread newThread(@NotNull Runnable r) {
                    Thread t = new Thread(ServerTools.getServerInstanceOrThrow().getServerThreadGroup(),
                            r, "Recv Message Thread #" + threadNumber.getAndIncrement());
                    t.setUncaughtExceptionHandler(null);
                    return t;
                }
            });
            return new NettyThreadPoolTaskReturn(bossGroup, workerGroup, RecvMessageThreadPool);
        });

        try {
            NettyThreadPoolTaskReturn taskReturn = (NettyThreadPoolTaskReturn) NettyThreadPoolTask.get();
            bossGroup = taskReturn.bossGroup();
            workerGroup = taskReturn.workerGroup();
            RecvMessageThreadPool = taskReturn.RecvMessageThreadPool();
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException("Thread Pool Fatal", e);
        }

        log.info("正在启动Netty");
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
                                CACertTask.get();
                                pipeline.addLast(
                                        SslContextBuilder.forServer(ServerSSLPrivateKey, ServerSSLCertificate)
                                                .sslProvider(SslProvider.JDK)
                                                .clientAuth(ClientAuth.NONE)
                                                .build()
                                                .newHandler(channel.alloc())
                                );
                            } catch (SSLException | InterruptedException | ExecutionException e) {
                                throw new RuntimeException("SSL Context Generate Failed!", e);
                            }
                            pipeline.addLast(new LoggingHandler(LogLevel.DEBUG));
                            pipeline.addLast(new LineBasedFrameDecoder(Integer.MAX_VALUE));
                            pipeline.addLast(new StringDecoder(StandardCharsets.UTF_8));//IO
                            pipeline.addLast(new StringEncoder(StandardCharsets.UTF_8));
                            pipeline.addLast(new MessageToMessageEncoder<CharSequence>() {
                                @Override
                                protected void encode(ChannelHandlerContext ctx, CharSequence msg, List<Object> out) {
                                    out.add(CharBuffer.wrap(msg + "\n"));
                                }
                            });// 每行消息添加换行符
                            pipeline.addLast(RecvMessageThreadPool, new ServerInHandler());//JavaIM逻辑
                        }
                    });

            future = bootstrap.bind(ListenPort).sync();
            log.info("JavaIM网络层启动完成");
            synchronized (NetworkServer.class) {
                NetworkServer.class.notifyAll();
            }
        } catch (InterruptedException e) {
            log.error("出现错误!", e);
        }
    }

    private void X509CertificateGenerate() {
        // 生成X509证书
        if (!(new File("./ServerEncryption/cert.crt").exists() &&
                new File("./ServerEncryption/private.key").exists())) {
            log.info("正在生成 X.509 CA 证书");
            X500Name subject = new X500NameBuilder()
                    .addRDN(BCStyle.C, "CN")//证书国家代号(Country Name)
                    .addRDN(BCStyle.O, "JavaIM-Server")//证书组织名(Organization Name)
                    .addRDN(BCStyle.OU, SystemConfig.getServerName())//证书组织单位名(Organization Unit Name)
                    .addRDN(BCStyle.CN, SystemConfig.getServerName())//证书通用名(Common Name)
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
                    Base64.getEncoder().encodeToString(privateKeyEncode);
            try {
                FileUtils.writeStringToFile(new File("./ServerEncryption/private.key"), privateKeyContent, StandardCharsets.UTF_8);
            } catch (IOException e) {
                throw new RuntimeException("Failed to Write private key,Permission denied?", e);
            }
            // 自签名X509证书
            long currentTimeMillis = System.currentTimeMillis();
            ContentSigner signer;
            try {
                signer = new JcaContentSignerBuilder("SHA256WITHRSA").build(privateKey);
            } catch (OperatorCreationException e) {
                throw new RuntimeException("Generate Content Signer Failed!", e);
            }
            // 将证书写入文件
            try {
                X509CertificateHolder certHolder = new X509v3CertificateBuilder(
                        subject,//证书签发者
                        BigInteger.valueOf(currentTimeMillis),//证书序列号
                        new Date(currentTimeMillis),//证书生效时间
                        new Date(currentTimeMillis + TimeUnit.DAYS.toMillis(365 * 10)),//证书失效时间
                        subject,//证书主体
                        SubjectPublicKeyInfo.getInstance(publicKey.getEncoded())//证书主体公钥
                )
                        .addExtension(Extension.basicConstraints, true, new BasicConstraints(true))
                        .build(signer);
                Certificate certificate = certHolder.toASN1Structure();
                byte[] certificateEncode = certificate.getEncoded();
                String certificateContent =
                        "-----BEGIN CERTIFICATE-----\n" +
                                lf(Base64.getEncoder().encodeToString(certificateEncode), 64) +
                                "-----END CERTIFICATE-----";
                FileUtils.writeStringToFile(new File("./ServerEncryption/cert.crt"), certificateContent, StandardCharsets.UTF_8);
            } catch (IOException e) {
                throw new RuntimeException("Failed to write cert to file, Permission denied?", e);
            }
            log.info("CA证书创建完成");
            log.info("请分发ServerEncryption文件夹中的cert.crt到各个客户端");
            log.info("请注意，ServerEncryption文件夹中的“private.key”请保护好");
            log.info("此文件如果泄露，您与客户端的连接将可能被劫持");
        }
        // 通过CA证书签署临时SSL证书

        log.info("正在使用 X.509 CA 证书 签发新的 X.509 临时 SSL 加密证书");
        // 加载CA证书
        Certificate caCert;
        PrivateKey caPrivateKey;
        try (FileInputStream stream = new FileInputStream("./ServerEncryption/cert.crt")) {
            CertificateFactory factory = CertificateFactory.getInstance("X.509", "BC");
            caCert = new JcaX509CertificateHolder((X509Certificate) factory.generateCertificate(stream)).toASN1Structure();
            caPrivateKey = KeyFactory.getInstance("RSA").generatePrivate(
                    new PKCS8EncodedKeySpec(
                            Base64.getDecoder().decode(
                                    FileUtils.readFileToString(new File("./ServerEncryption/private.key"), StandardCharsets.UTF_8).getBytes(StandardCharsets.UTF_8)
                            )
                    )
            );
        } catch (CertificateException | NoSuchProviderException | NoSuchAlgorithmException | InvalidKeySpecException |
                 IOException e) {
            throw new RuntimeException("Failed to open X.509 CA Cert & X.509 RSA Private key, Permission denied?", e);
        }

        // 创建终端主体
        X500Name subject = new X500NameBuilder()
                .addRDN(BCStyle.C, "CN")//证书国家代号(Country Name)
                .addRDN(BCStyle.O, "JavaIM-Server")//证书组织名(Organization Name)
                .addRDN(BCStyle.OU, SystemConfig.getServerName())//证书组织单位名(Organization Unit Name)
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
            throw new RuntimeException("Generate Content Signer Failed!", e);
        }
        try {
            ServerSSLCertificate = new JcaX509CertificateConverter().getCertificate(
                    new X509v3CertificateBuilder(
                            caCert.getSubject(),//证书签发者
                            BigInteger.valueOf(currentTimeMillis),//证书序列号
                            new Date(currentTimeMillis),//证书生效时间
                            new Date(currentTimeMillis + TimeUnit.DAYS.toMillis(90)),//证书失效时间
                            subject,//证书主体
                            SubjectPublicKeyInfo.getInstance(publicKey.getEncoded())//证书主体公钥
                    )
                            .addExtension(Extension.keyUsage, true, new KeyUsage(KeyUsage.digitalSignature | KeyUsage.keyEncipherment))
                            .addExtension(Extension.extendedKeyUsage, false, new ExtendedKeyUsage(new KeyPurposeId[]{KeyPurposeId.id_kp_serverAuth, KeyPurposeId.id_kp_clientAuth}))
                            .addExtension(Extension.basicConstraints, true, new BasicConstraints(false))
                            .build(signer)
            );
        } catch (CertIOException | CertificateException e) {
            throw new RuntimeException("Generate SSL Cert Failed!", e);
        }
        log.info("X.509 SSL 证书已经签发完成");
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
        isRunning = false;
        ExitWatchdog.getInstance().addExitTask(() -> {
            log.info("JavaIM 网络层正在关闭...");
            future.channel().close();
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
            RecvMessageThreadPool.shutdownGracefully();
            log.info("JavaIM 网络层关闭完成");
        },this);
    }


    private class ServerInHandler extends ChannelInboundHandlerAdapter {
        private final HashMap<Channel, NetworkClient> clientNetworkClientPair = new HashMap<>();
        private final Gson gson = new Gson();

        @Override
        public void channelActive(ChannelHandlerContext ctx) {
            log.info("检测到新客户端连接...");
            log.info("此客户端IP地址：{}", ctx.channel().remoteAddress());
            if (!ServerTools.getServerInstance().isServerCompleteStart()) {
                SystemProtocol systemProtocol = new SystemProtocol();
                systemProtocol.setType("Error");
                systemProtocol.setMessage("Server is not start completely");

                GeneralProtocol protocol = new GeneralProtocol();
                protocol.setProtocolVersion(SystemConfig.getProtocolVersion());
                protocol.setProtocolName("SystemProtocol");
                protocol.setProtocolData(gson.toJson(systemProtocol));
                ctx.writeAndFlush(gson.toJson(protocol));
                return;
            }
            NettyUser nettyUser = new NettyUser();
            if (!ServerTools.getServerInstanceOrThrow().registerUser(nettyUser)) {
                ctx.channel().close();
                return;
            }
            NetworkClient client = new NettyNetworkClient(nettyUser, ctx.channel().remoteAddress(), ctx.channel());
            nettyUser.setNetworkClient(client);
            clientNetworkClientPair.put(ctx.channel(), client);
            clientList.add(client);
        }

        @Override
        public void channelInactive(ChannelHandlerContext ctx) {
            log.info("检测到客户端离线...");
            log.info("此客户端IP地址：{}", ctx.channel().remoteAddress());
            NetworkClient thisClient = clientNetworkClientPair.remove(ctx.channel());
            if (thisClient == null)
                return;
            ServerTools.getServerInstanceOrThrow().unRegisterUser(thisClient.getUser());
            clientList.remove(thisClient);
        }

        @Override
        public void channelRead(ChannelHandlerContext ctx, Object msg) {
            try {
                if (!(msg instanceof String Msg)) {
                    log.info(String.format("客户端：%s 发送了非String消息：%s", ctx.channel().remoteAddress(), msg.toString()));
                    return;
                }
                if (Msg.isEmpty()) {
                    SystemProtocol systemProtocol = new SystemProtocol();
                    systemProtocol.setType("Error");
                    systemProtocol.setMessage("Empty Packet");

                    GeneralProtocol protocol = new GeneralProtocol();
                    protocol.setProtocolVersion(SystemConfig.getProtocolVersion());
                    protocol.setProtocolName("SystemProtocol");
                    protocol.setProtocolData(gson.toJson(systemProtocol));
                    ctx.writeAndFlush(gson.toJson(protocol));
                    return;
                }
                NetworkClient thisClient = clientNetworkClientPair.get(ctx.channel());
                ServerTools.getServerInstanceOrThrow().onReceiveMessage(thisClient.getUser(), Msg);
            } catch (Throwable throwable) {
                log.warn(String.format("客户端：%s 处理程序出错！", ctx.channel().remoteAddress()));
                log.warn("错误为：", throwable);

                SystemProtocol systemProtocol = new SystemProtocol();
                systemProtocol.setType("Error");
                systemProtocol.setMessage("uncaught exception");

                GeneralProtocol protocol = new GeneralProtocol();
                protocol.setProtocolVersion(SystemConfig.getProtocolVersion());
                protocol.setProtocolName("SystemProtocol");
                protocol.setProtocolData(gson.toJson(systemProtocol));
                ctx.writeAndFlush(gson.toJson(protocol));
            } finally {
                ReferenceCountUtil.release(msg);
            }
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
            if (cause instanceof DecoderException) {
                Throwable exceptionCause = cause.getCause();
                if (exceptionCause instanceof SSLHandshakeException) {
                    log.warn(String.format("客户端：%s 因为SSL错误：%s已断开连接", ctx.channel().remoteAddress(), exceptionCause.getMessage()));
                    return;
                }
            }
            log.error("出现错误!", cause);
            log.warn(String.format("客户端：%s 因为：%s 已经断开连接", ctx.channel().remoteAddress(), cause.getMessage()));
            ctx.channel().close();
        }
    }

    private class NettyNetworkClient implements NetworkClient {
        private final NetworkUser user;
        private final SocketAddress address;
        private final Channel channel;

        private NettyNetworkClient(NetworkUser user, SocketAddress address, Channel channel) {
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
            checks.checkState(!isOnline(), "This user is now offline!");
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
        public NetworkUser getUser() {
            return user;
        }
    }

    private static class NettyUser extends JavaUser implements NetworkUser {
        private NetworkClient client;

        /**
         * 设置网络层客户端
         *
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
            log.info(String.format("用户：%s(IP地址：%s) 登录完成", UserName, getNetworkClient().getSocketAddress()));
            return super.onUserLogin(UserName);
        }

        @Override
        public user disconnect() {
            getNetworkClient().disconnect();
            return super.disconnect();
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
