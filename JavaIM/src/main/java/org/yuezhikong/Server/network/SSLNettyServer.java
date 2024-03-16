package org.yuezhikong.Server.network;

import cn.hutool.crypto.SecureUtil;
import io.netty.handler.ssl.SslContextBuilder;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.FileUtils;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x500.X500NameBuilder;
import org.bouncycastle.asn1.x500.style.BCStyle;
import org.bouncycastle.asn1.x509.Certificate;
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.X509v3CertificateBuilder;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.bouncycastle.x509.X509V3CertificateGenerator;
import org.yuezhikong.GeneralMethod;
import org.yuezhikong.utils.Logger;
import org.yuezhikong.utils.checks;

import java.io.File;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ExecutorService;

public class SSLNettyServer implements NetworkServer {

    private final List<NetworkClient> clientList = new ArrayList<>();
    private final File ServerPrivateKey = new File("./ServerEncryption/Private.key");
    @Override
    public void start(int ListenPort) throws IllegalStateException {
        checks.checkArgument(ListenPort < 1 || ListenPort > 65535, "The Port is not in the range of [0,65535]!");
        // 制作RSA密钥对
        GeneralMethod method = new GeneralMethod();
        method.RSA_KeyAutogenerate(new File("./ServerEncryption/Public.key").getAbsolutePath(), ServerPrivateKey.getAbsolutePath(), new Logger());
        // 生成X509证书
        if (!(new File("./ServerEncryption/cert.crt").exists() &&
            new File("./ServerEncryption/private.key").exists())) {
            X500Name subject;
            X500NameBuilder X500Builder = new X500NameBuilder()
                    .addRDN(BCStyle.C, "CN")//证书国家代号(Country Name)
                    .addRDN(BCStyle.O, "Yuezhikong")//证书组织名(Organization Name)
                    .addRDN(BCStyle.OU, "JavaIM")//证书组织单位名(Organization Unit Name)
                    .addRDN(BCStyle.CN, "JavaIM General Encryption");//证书通用名(Common Name)
            //50%几率，证书所属州与城市为上海，50%概率为北京
            if (Math.random() > 0.5) {
                    subject = X500Builder.addRDN(BCStyle.ST, "Shanghai")//证书州或省份(State or Province Name)
                            .addRDN(BCStyle.L, "Shanghai")//证书所属城市名(Locality Name)
                            .build();
            }
            else {
                subject = X500Builder.addRDN(BCStyle.ST, "Beijing")//证书州或省份(State or Province Name)
                        .addRDN(BCStyle.L, "Beijing")//证书所属城市名(Locality Name)
                        .build();
            }
            // 创建密钥对
            KeyPair pair = SecureUtil.generateKeyPair("RSA",2048);
            PublicKey publicKey = pair.getPublic();
            PrivateKey privateKey = pair.getPrivate();
            // 根据规范将privateKey写入到文件
            byte[] privateKeyEncode = privateKey.getEncoded();
            String privateKeyContent =
                    "-----BEGIN RSA PRIVATE KEY-----\n"+
                            lf(Base64.encodeBase64String(privateKeyEncode),64)+
                            "-----END RSA PRIVATE KEY-----";
            try {
                FileUtils.writeStringToFile(new File("./ServerEncryption/X509Privatekey.key"), privateKeyContent, StandardCharsets.UTF_8);
            } catch (IOException e) {
                throw new RuntimeException("Failed to Write private key,Permission denied?",e);
            }
            // 自签名X509证书
            long currentTimeMillis = System.currentTimeMillis();
            X500Name issuer = subject;
            ContentSigner signer;
            try {
                signer = new JcaContentSignerBuilder("SHA256WITHRSA").build(privateKey);
            } catch (OperatorCreationException e) {
                throw new RuntimeException("Generate Content Signer Failed!",e);
            }
            X509CertificateHolder certHolder = new X509v3CertificateBuilder(
                    issuer,//证书签发者
                    BigInteger.valueOf(currentTimeMillis),//证书序列号
                    new Date(currentTimeMillis),//证书生效时间
                    new Date(currentTimeMillis + (long) 365*24*60*60*1000),//证书失效时间
                    subject,//证书主体
                    SubjectPublicKeyInfo.getInstance(publicKey)//证书主体公钥
            ).build(signer);
            Certificate certificate = certHolder.toASN1Structure();
            // 将证书写入文件
            try {
                byte[] certificateEncode = certificate.getEncoded();
                String certificateContent =
                        "-----BEGIN CERTIFICATE-----\n"+
                                lf(Base64.encodeBase64String(certificateEncode),64)+
                                "-----END CERTIFICATE-----";
                FileUtils.writeStringToFile(new File("./ServerEncryption/X509cert.crt"), privateKeyContent, StandardCharsets.UTF_8);
            } catch (IOException e) {
                throw new RuntimeException("Failed to write cert to file, Permission denied?",e);
            }
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
        return false;
    }

    @Override
    public void stop() throws IllegalStateException {

    }

    @Override
    public ExecutorService getIOThreadPool() {
        return null;
    }
}
