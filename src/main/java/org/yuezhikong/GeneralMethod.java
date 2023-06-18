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
}
