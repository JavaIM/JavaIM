package org.yuezhikong.utils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class SHA256 {
    private SHA256() {
    }

    /**
     * 执行sha256摘要
     *
     * @param str 原始
     * @return sha256摘要
     */
    public static String sha256(String str) {
        try {
            // 获取SHA-256 MessageDigest实例
            MessageDigest digest = MessageDigest.getInstance("SHA-256");

            // 更新要计算哈希的数据
            byte[] encodedhash = digest.digest(str.getBytes(StandardCharsets.UTF_8));

            // 完成哈希计算后，将结果转换为16进制字符串
            StringBuilder hexString = new StringBuilder();
            for (byte b : encodedhash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }

            return hexString.toString();

        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA256 Provider Not Found!", e); // SHA-256应该总是可用的
        }
    }
}
