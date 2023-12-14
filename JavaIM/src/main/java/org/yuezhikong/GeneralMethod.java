/*
 * Simplified Chinese (简体中文)
 *
 * 版权所有 (C) 2023 QiLechan <qilechan@outlook.com> 和本程序的贡献者
 *
 * 本程序是自由软件：你可以再分发之和/或依照由自由软件基金会发布的 GNU 通用公共许可证修改之，无论是版本 3 许可证，还是 3 任何以后版都可以。
 * 发布该程序是希望它能有用，但是并无保障;甚至连可销售和符合某个特定的目的都不保证。请参看 GNU 通用公共许可证，了解详情。
 * 你应该随程序获得一份 GNU 通用公共许可证的副本。如果没有，请看 <https://www.gnu.org/licenses/>。
 * English (英语)
 *
 * Copyright (C) 2023 QiLechan <qilechan@outlook.com> and contributors to this program
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or 3 any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program. If not, see <https://www.gnu.org/licenses/>.
 */
package org.yuezhikong;

import cn.hutool.crypto.SecureUtil;
import cn.hutool.crypto.symmetric.PBKDF2;
import cn.hutool.crypto.symmetric.SymmetricAlgorithm;
import com.google.gson.Gson;
import org.jetbrains.annotations.NotNull;
import org.yuezhikong.utils.Logger;
import org.yuezhikong.utils.Protocol.NormalProtocol;
import org.yuezhikong.utils.RSA;
import org.yuezhikong.utils.SaveStackTrace;

import javax.crypto.SecretKey;
import java.io.File;
import java.nio.charset.StandardCharsets;

public class GeneralMethod implements GeneralMethodInterface{
    @Override
    public SecretKey GenerateKey(@NotNull String source1,@NotNull String source2)
    {
        return SecureUtil.generateKey(SymmetricAlgorithm.AES.getValue(), //生成SecretKey，算法为AES
                new PBKDF2("PBKDF2WithHmacSHA256",256,1000).//算法为PBKDF2WithHmacSHA256，最终密钥长度为512，重复1000次
                        encrypt(source1.toCharArray(),source2.getBytes(StandardCharsets.UTF_8)//根据输入的Password和salt进行生成
                        )
        );
    }
    @Override
    public NormalProtocol protocolRequest(String json)
    {
        try
        {
            Gson gson = new Gson();
            return gson.fromJson(json,NormalProtocol.class);
        } catch (Throwable e)
        {
            System.out.println("Json处理失败，原始输入为:"+json);
            throw new RuntimeException("Json Request Failed",e);
        }
    }
    @Override
    public void RSA_KeyAutogenerate(String PublicKeyFile, String PrivateKeyFile, Logger logger)
    {
        boolean PublicKeyFileExist = new File(PublicKeyFile).exists();
        boolean PrivateKeyFileExist = new File(PrivateKeyFile).exists();
        if (!PublicKeyFileExist && !PrivateKeyFileExist)
        {
            try {
                RSA.generateKeyToFile(PublicKeyFile, PrivateKeyFile);
            }
            catch (Exception e)
            {
                SaveStackTrace.saveStackTrace(e);
            }
        }
        else if (!PublicKeyFileExist)
        {
            logger.warning("系统检测到您的目录下不存在公钥，但，存在私钥，系统将为您覆盖一个新的rsa key");
            try {
                RSA.generateKeyToFile(PublicKeyFile, PrivateKeyFile);
            }
            catch (Exception e)
            {
                SaveStackTrace.saveStackTrace(e);
            }
        }
        else if (!PrivateKeyFileExist)
        {
            logger.warning("系统检测到您的目录下存在公钥，但，不存在私钥，系统将为您覆盖一个新的rsa key");
            try {
                RSA.generateKeyToFile(PublicKeyFile, PrivateKeyFile);
            }
            catch (Exception e)
            {
                SaveStackTrace.saveStackTrace(e);
            }
        }
    }
}
