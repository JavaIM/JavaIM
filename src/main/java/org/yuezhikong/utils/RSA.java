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
    public static KeyData loadPublicKeyFromFile(String filePath)
    {
        try {
            String keyString = FileUtils.readFileToString(new File(filePath), StandardCharsets.UTF_8);
            KeyData keyData = new KeyData();
            keyData.PublicKey = keyString;
            return keyData;
        }
        catch (IOException e)
        {
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            e.printStackTrace(pw);
            pw.flush();
            sw.flush();
            org.apache.logging.log4j.Logger logger_log4j = LogManager.getLogger("Debug");
            logger_log4j.debug(sw.toString());
            pw.close();
            try {
                sw.close();
            }
            catch (IOException ex)
            {
                ex.printStackTrace();
            }
            return null;
        }
    }
    public static KeyData loadPrivateKeyFromFile(String filePath)
    {
        try {
            String keyString = FileUtils.readFileToString(new File(filePath), StandardCharsets.UTF_8);
            KeyData keyData = new KeyData();
            keyData.PrivateKey = keyString;
            return keyData;
        }
        catch (IOException e)
        {
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            e.printStackTrace(pw);
            pw.flush();
            sw.flush();
            org.apache.logging.log4j.Logger logger_log4j = LogManager.getLogger("Debug");
            logger_log4j.debug(sw.toString());
            pw.close();
            try {
                sw.close();
            }
            catch (IOException ex)
            {
                ex.printStackTrace();
            }
            return null;
        }
    }
    public static KeyData generateKeyToReturn()
    {
        KeyData keyData = new KeyData();
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
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            e.printStackTrace(pw);
            pw.flush();
            sw.flush();
            org.apache.logging.log4j.Logger logger_log4j = LogManager.getLogger("Debug");
            logger_log4j.debug(sw.toString());
            pw.close();
            try {
                sw.close();
            }
            catch (IOException ex)
            {
                ex.printStackTrace();
            }
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