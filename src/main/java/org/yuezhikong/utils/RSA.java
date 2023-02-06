package org.yuezhikong.utils;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.FileUtils;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
 
public class RSA {
 
	/**
     * 生成密钥对并保存在本地文件中
     *
     * @param pubPath   : 公钥保存路径
     * @param priPath   : 私钥保存路径
     */
    public static void generateKeyToFile(String pubPath, String priPath) throws Exception {
        // 获取密钥对生成器
        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
        // 获取密钥对
        KeyPair keyPair = keyPairGenerator.generateKeyPair();
        // 获取公钥
        PublicKey publicKey = keyPair.getPublic();
        // 获取私钥
        PrivateKey privateKey = keyPair.getPrivate();
        // 获取byte数组
        byte[] publicKeyEncoded = publicKey.getEncoded();
        byte[] privateKeyEncoded = privateKey.getEncoded();
        // 进行Base64编码
        String publicKeyString = Base64.encodeBase64String(publicKeyEncoded);
        String privateKeyString = Base64.encodeBase64String(privateKeyEncoded);
        // 保存文件
        FileUtils.writeStringToFile(new File(pubPath), publicKeyString, StandardCharsets.UTF_8);
        FileUtils.writeStringToFile(new File(priPath), privateKeyString, StandardCharsets.UTF_8);
 
    }
 
    /**
     * 从文件中加载公钥
     *
     * @param filePath  : 文件路径
     * @return : 公钥
     */
    public static PublicKey loadPublicKeyFromFile(String filePath) throws Exception {
        // 将文件内容转为字符串
        String keyString = FileUtils.readFileToString(new File(filePath), StandardCharsets.UTF_8);
 
        return loadPublicKeyFromString(keyString);
 
    }
 
    /**
     * 从字符串中加载公钥
     *
     * @param keyString : 公钥字符串
     * @return : 公钥
     */
    public static PublicKey loadPublicKeyFromString(String keyString) throws Exception {
        // 进行Base64解码
        byte[] decode = Base64.decodeBase64(keyString);
        // 获取密钥工厂
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        // 构建密钥规范
        X509EncodedKeySpec keyspec = new X509EncodedKeySpec(decode);
        // 获取公钥
        return keyFactory.generatePublic(keyspec);
 
    }
 
    /**
     * 从文件中加载私钥
     *
     * @param filePath  : 文件路径
     * @return : 私钥
     */
    public static PrivateKey loadPrivateKeyFromFile(String filePath) throws Exception {
        // 将文件内容转为字符串
        String keyString = FileUtils.readFileToString(new File(filePath), StandardCharsets.UTF_8);
        return loadPrivateKeyFromString(keyString);
 
    }
 
    /**
     * 从字符串中加载私钥
     *
     * @param keyString : 私钥字符串
     * @return : 私钥
     */
    private static PrivateKey loadPrivateKeyFromString(String keyString) throws Exception {
        // 进行Base64解码
        byte[] decode = Base64.decodeBase64(keyString);
        // 获取密钥工厂
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        // 构建密钥规范
        PKCS8EncodedKeySpec keyspec = new PKCS8EncodedKeySpec(decode);
        // 生成私钥
        return keyFactory.generatePrivate(keyspec);
 
    }

    /**
     * RSA私钥解密
     * @param str  解密字符串
     * @param privateKey  私钥
     * @return 明文
     */
    public static String decrypt(String str, Key privateKey) {
        //64位解码加密后的字符串
        byte[] inputByte;
        String outStr = "";
        try {
            inputByte = Base64.decodeBase64(str.getBytes(StandardCharsets.UTF_8));
            //RSA解密
            Cipher cipher = Cipher.getInstance("RSA");
            cipher.init(Cipher.DECRYPT_MODE, privateKey);
            outStr = new String(cipher.doFinal(inputByte));
        } catch (NoSuchPaddingException | InvalidKeyException | IllegalBlockSizeException | BadPaddingException | NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return outStr;
    }

    /**
     *  RSA公钥加密
     * @param str 需要加密的字符串
     * @param publicKey 公钥
     * @return 密文
     * @throws Exception 加密过程中的异常信息
     */
    public static String encrypt(String str, Key publicKey) throws Exception {
        //RSA加密
        Cipher cipher = Cipher.getInstance("RSA");
        cipher.init(Cipher.ENCRYPT_MODE, publicKey);
        return Base64.encodeBase64String(cipher.doFinal(str.getBytes(StandardCharsets.UTF_8)));
    }
}
