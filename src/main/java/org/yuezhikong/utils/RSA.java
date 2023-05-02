package org.yuezhikong.utils;

import cn.hutool.crypto.SecureUtil;
import cn.hutool.crypto.asymmetric.KeyType;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;

public class RSA {
    public static CustomVar.KeyData loadPublicKeyFromFile(String filePath)
    {
        try {
            String keyString = FileUtils.readFileToString(new File(filePath), StandardCharsets.UTF_8);
            CustomVar.KeyData keyData = new CustomVar.KeyData();
            keyData.PublicKey = keyString;
            return keyData;
        }
        catch (IOException e)
        {
            SaveStackTrace.saveStackTrace(e);
            return null;
        }
    }
    public static CustomVar.KeyData loadPrivateKeyFromFile(String filePath)
    {
        try {
            String keyString = FileUtils.readFileToString(new File(filePath), StandardCharsets.UTF_8);
            CustomVar.KeyData keyData = new CustomVar.KeyData();
            keyData.PrivateKey = keyString;
            return keyData;
        }
        catch (IOException e)
        {
            SaveStackTrace.saveStackTrace(e);
            return null;
        }
    }
    public static CustomVar.KeyData generateKeyToReturn()
    {
        CustomVar.KeyData keyData = new CustomVar.KeyData();
        KeyPair pair = SecureUtil.generateKeyPair("RSA");
        keyData.privateKey = pair.getPrivate();
        keyData.publicKey = pair.getPublic();
        return keyData;
    }
    public static void generateKeyToFile(String PublicKeyFile,String PrivateKeyFile)
    {
        KeyPair pair = SecureUtil.generateKeyPair("RSA");
        PrivateKey privateKey = pair.getPrivate();
        PublicKey publicKey = pair.getPublic();
        byte[] publicKeyEncoded = publicKey.getEncoded();
        byte[] privateKeyEncoded = privateKey.getEncoded();
        // 进行Base64编码
        String publicKeyString = Base64.encodeBase64String(publicKeyEncoded);
        String privateKeyString = Base64.encodeBase64String(privateKeyEncoded);
        // 保存文件
        try {
            FileUtils.writeStringToFile(new File(PublicKeyFile), publicKeyString, StandardCharsets.UTF_8);
            FileUtils.writeStringToFile(new File(PrivateKeyFile), privateKeyString, StandardCharsets.UTF_8);
        } catch (IOException e)
        {
            SaveStackTrace.saveStackTrace(e);
        }
    }

    public static String encrypt(String Message, String PublicKey)
    {
        cn.hutool.crypto.asymmetric.RSA rsa = new cn.hutool.crypto.asymmetric.RSA(null,PublicKey);
        return rsa.encryptBase64(Message, KeyType.PublicKey);
    }

    public static String decrypt(String message, PrivateKey privateKey) {
        cn.hutool.crypto.asymmetric.RSA rsa = new cn.hutool.crypto.asymmetric.RSA(privateKey, null);
        return rsa.decryptStr(message, KeyType.PrivateKey);
    }

    public static String decrypt(String message, String privateKey) {
        cn.hutool.crypto.asymmetric.RSA rsa = new cn.hutool.crypto.asymmetric.RSA(privateKey, null);
        return rsa.decryptStr(message, KeyType.PrivateKey);
    }
}