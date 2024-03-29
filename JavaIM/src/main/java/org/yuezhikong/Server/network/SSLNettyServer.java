package org.yuezhikong.Server.network;

import cn.hutool.crypto.KeyUtil;
import cn.hutool.crypto.SecureUtil;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.LineBasedFrameDecoder;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.handler.codec.string.StringEncoder;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.handler.ssl.ClientAuth;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.SslProvider;
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
import org.yuezhikong.CodeDynamicConfig;
import org.yuezhikong.utils.Logger;
import org.yuezhikong.utils.SaveStackTrace;
import org.yuezhikong.utils.checks;

import javax.net.ssl.SSLException;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.math.BigInteger;
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
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

public class SSLNettyServer implements NetworkServer {

    private final List<NetworkClient> clientList = new ArrayList<>();
    private PrivateKey ServerSSLPrivateKey;
    private X509Certificate ServerSSLCertificate;
    private final Logger logger = Logger.getInstance();

    private ChannelFuture future;

    private boolean isRunning = false;
    @Override
    public void start(int ListenPort) throws IllegalStateException {
        if (isRunning)
            throw new IllegalStateException("The Server is already running!");
        isRunning = true;
        checks.checkArgument(ListenPort < 1 || ListenPort > 65535, "The Port is not in the range of [0,65535]!");
        X509CertificateGenerate();
        ThreadGroup IOThreadGroup = new ThreadGroup("Netty IO Thread");
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

            logger.info("网络层启动完成");
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

    @Override
    public ExecutorService getIOThreadPool() {
        return null;
    }

    private class ServerInHandler extends ChannelInboundHandlerAdapter {
    }
}
