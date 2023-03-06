package org.yuezhikong.JavaIMAndroid;

import android.util.Base64;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.PrivateKey;

import cn.hutool.crypto.SecureUtil;
import cn.hutool.crypto.asymmetric.KeyType;

    public class RSA {
        public static StringBuilder readTxt(String path) throws IOException{
            StringBuilder sb = new StringBuilder();
            File urlFile = new File(path);
            InputStreamReader isr = new InputStreamReader(new FileInputStream(urlFile), StandardCharsets.UTF_8);
            BufferedReader br = new BufferedReader(isr);
            String mimeTypeLine ;
            sb.delete(0,sb.length());
            while ((mimeTypeLine = br.readLine()) != null) {
                sb.append(mimeTypeLine).append("\n");
            }
            return  sb;
        }
        public static KeyData loadPublicKeyFromFile(String filePath)
        {
            try {
                String keyString = readTxt(filePath).toString();
                KeyData keyData = new KeyData();
                keyData.PublicKey = keyString;
                return keyData;
            }
            catch (IOException e)
            {
                e.printStackTrace();
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

        public static String encrypt(String Message, String PublicKey)
        {
            cn.hutool.crypto.asymmetric.RSA rsa = new cn.hutool.crypto.asymmetric.RSA(null,PublicKey);
            return Base64.encodeToString(rsa.encrypt(Message, KeyType.PublicKey),Base64.NO_WRAP);
        }

        public static String decrypt(String message, PrivateKey privateKey) {
            cn.hutool.crypto.asymmetric.RSA rsa = new cn.hutool.crypto.asymmetric.RSA(privateKey, null);
            return rsa.decryptStr(message, KeyType.PrivateKey);
        }

    }