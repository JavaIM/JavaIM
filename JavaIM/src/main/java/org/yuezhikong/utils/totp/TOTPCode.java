package org.yuezhikong.utils.totp;

import com.google.gson.Gson;
import dev.samstevens.totp.code.CodeVerifier;
import dev.samstevens.totp.code.DefaultCodeGenerator;
import dev.samstevens.totp.code.DefaultCodeVerifier;
import dev.samstevens.totp.code.HashingAlgorithm;
import dev.samstevens.totp.exceptions.QrGenerationException;
import dev.samstevens.totp.qr.QrData;
import dev.samstevens.totp.qr.ZxingPngQrGenerator;
import dev.samstevens.totp.secret.DefaultSecretGenerator;
import dev.samstevens.totp.time.SystemTimeProvider;
import org.apache.commons.codec.binary.Base64;
import org.yuezhikong.Server.ServerTools;
import org.yuezhikong.Server.userData.user;
import org.yuezhikong.utils.protocol.SystemProtocol;
import org.yuezhikong.utils.protocol.TransferProtocol;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;

public class TOTPCode {
    private static final Gson gson = new Gson();
    private TOTPCode() {}

    /**
     * 执行 URLEncode 操作
     * @param text 原始
     * @return 编码后
     */
    private static String urlEncode(String text) {
        return URLEncoder.encode(text, StandardCharsets.UTF_8).replaceAll("\\+", "%20");
    }

    /**
     * 生成TOTP secret
     * @param user 用户
     */
    public static void generateTOTPSecret(user user) {
        String secret = new DefaultSecretGenerator().generate();
        SystemProtocol protocol = new SystemProtocol();
        protocol.setType("TOTP");
        protocol.setMessage("otpauth://"+
                urlEncode("totp")+"/"+ // type
                urlEncode(user.getUserName())+ // label
                "?secret="+urlEncode(secret)+ // secret
                "&issuer="+urlEncode("JavaIM")+ // issuer
                "&algorithm="+urlEncode(HashingAlgorithm.SHA1.getFriendlyName())+ // algorithm
                "&digits="+6+// digits
                "&period="+30// period
        );
        ServerTools.getServerInstanceOrThrow().getServerAPI()
                .sendJsonToClient(user, gson.toJson(protocol), "SystemProtocol");
        user.getUserInformation().setTotpSecret(secret);
        user.setUserInformation(user.getUserInformation());//触发持久化
    }

    /**
     * 验证 TOTP code
     * @param user 用户
     * @param code 代码
     * @return 是否验证成功
     */
    public static boolean verifyTOTPCode(user user, String code) {
        String secret = user.getUserInformation().getTotpSecret();
        CodeVerifier verifier = new DefaultCodeVerifier(new DefaultCodeGenerator(), new SystemTimeProvider());
        return verifier.isValidCode(secret, code);
    }
}
