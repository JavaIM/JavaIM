package org.yuezhikong;

import cn.hutool.crypto.SecureUtil;
import cn.hutool.crypto.symmetric.SymmetricAlgorithm;
import com.google.gson.Gson;
import org.apache.commons.codec.binary.Base64;
import org.jetbrains.annotations.NotNull;
import org.yuezhikong.utils.Protocol.NormalProtocol;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;

public class GeneralMethod {
    protected String GenerateKey(@NotNull String source)
    {
        try {
            byte[] KeyByte = new byte[32];
            byte[] SrcByte = Base64.encodeBase64(source.getBytes(StandardCharsets.UTF_8));
            System.arraycopy(SrcByte, 0, KeyByte, 0, 31);
            SecretKey key = SecureUtil.generateKey(SymmetricAlgorithm.AES.getValue(), KeyByte);
            return Base64.encodeBase64String(key.getEncoded());
        } catch (ArrayIndexOutOfBoundsException e)
        {
            return GenerateKey(source + source);
        }
    }
    protected NormalProtocol protocolRequest(String json)
    {
        try
        {
            Gson gson = new Gson();
            return gson.fromJson(json,NormalProtocol.class);
        } catch (Throwable e)
        {
            throw new RuntimeException("Json Request Failed",e);
        }
    }
    protected static @NotNull String unicodeToString(@NotNull String unicode) {
        StringBuilder sb = new StringBuilder();
        int i = 0;
        int len = unicode.length();
        while (i < len) {
            char c = unicode.charAt(i);
            if (c == '\\') {
                if (i < len - 5) {
                    char c1 = unicode.charAt(i + 1);
                    char c2 = unicode.charAt(i + 2);
                    char c3 = unicode.charAt(i + 3);
                    char c4 = unicode.charAt(i + 4);
                    if (c1 == 'u' && isHexDigit(c2) && isHexDigit(c3) && isHexDigit(c4)) {
                        int code = (Character.digit(c2, 16) << 12)
                                + (Character.digit(c3, 16) << 8)
                                + (Character.digit(c4, 16) << 4)
                                + Character.digit(unicode.charAt(i + 5), 16);
                        sb.append((char) code);
                        i += 6;
                        continue;
                    }
                }
            }
            sb.append(c);
            i++;
        }
        return sb.toString();
    }

    private static boolean isHexDigit(char ch) {
        return (ch >= '0' && ch <= '9') ||
                (ch >= 'a' && ch <= 'f') ||
                (ch >= 'A' && ch <= 'F');
    }
}
