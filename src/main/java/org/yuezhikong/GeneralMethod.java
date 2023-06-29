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
import cn.hutool.crypto.symmetric.SymmetricAlgorithm;
import com.google.gson.Gson;
import org.apache.commons.codec.binary.Base64;
import org.jetbrains.annotations.NotNull;
import org.yuezhikong.utils.Protocol.NormalProtocol;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;

public class GeneralMethod implements GeneralMethodInterface{
    public String GenerateKey(@NotNull String source)
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
    public NormalProtocol protocolRequest(String json)
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
    public @NotNull String unicodeToString(@NotNull String unicode) {
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
