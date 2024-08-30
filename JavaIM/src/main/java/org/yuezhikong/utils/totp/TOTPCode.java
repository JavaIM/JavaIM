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
import org.yuezhikong.utils.Protocol.TransferProtocol;

import java.util.ArrayList;

public class TOTPCode {
    private static final Gson gson = new Gson();
    private TOTPCode() {}

    /**
     * 生成TOTP secret
     * @param user 用户
     */
    public static void generateTOTPSecret(user user) {
        String secret = new DefaultSecretGenerator().generate();
        QrData data = new QrData.Builder()
                .label(user.getUserName())
                .secret(secret)
                .issuer("JavaIM")
                .algorithm(HashingAlgorithm.SHA1)
                .digits(6)
                .period(30)
                .build();
        try {
            TransferProtocol protocol = new TransferProtocol();
            protocol.setTransferProtocolBody(new ArrayList<>());

            TransferProtocol.TransferProtocolHeadBean headBean = new TransferProtocol.TransferProtocolHeadBean();
            headBean.setType("QRCode");
            protocol.setTransferProtocolHead(headBean);

            TransferProtocol.TransferProtocolBodyBean bodyBean = new TransferProtocol.TransferProtocolBodyBean();
            bodyBean.setData(Base64.encodeBase64String(new ZxingPngQrGenerator().generate(data)));
            protocol.getTransferProtocolBody().add(bodyBean);
            ServerTools.getServerInstanceOrThrow().getServerAPI()
                    .sendJsonToClient(user, gson.toJson(protocol), "TransferProtocol");
        } catch (QrGenerationException e) {
            ServerTools.getServerInstanceOrThrow().getServerAPI().
                    sendMessageToUser(user, "制作二维码失败");
            ServerTools.getServerInstanceOrThrow().getServerAPI().
                    sendMessageToUser(user, "secret:"+secret);
            ServerTools.getServerInstanceOrThrow().getServerAPI().
                    sendMessageToUser(user, "算法:SHA256");
            ServerTools.getServerInstanceOrThrow().getServerAPI().
                    sendMessageToUser(user, "每次有效期:30秒");
            ServerTools.getServerInstanceOrThrow().getServerAPI().
                    sendMessageToUser(user, "6位数");
            throw new RuntimeException("QR Code Generate Failed",e);
        }
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
