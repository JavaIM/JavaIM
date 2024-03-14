package org.yuezhikong.Server.UserData.tcpUser;

import cn.hutool.crypto.symmetric.AES;
import org.yuezhikong.Server.UserData.user;

public interface tcpUser extends user {

    /**
     * 设置用户公钥
     * @param publicKey 用户公钥
     */
    user setPublicKey(String publicKey);

    /**
     * 获取用户公钥
     * @return 用户公钥
     */
    String getPublicKey();

    /**
     * 设置用户的AES加密器
     * @param userAES AES加密器
     */
    user setUserAES(AES userAES);

    /**
     * 返回用户的AES加密器
     * @return AES加密器
     */
    AES getUserAES();
}
