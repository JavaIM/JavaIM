package org.yuezhikong.utils.totp;

import com.google.gson.*;
import dev.samstevens.totp.recovery.RecoveryCodeGenerator;
import org.yuezhikong.Server.ServerTools;
import org.yuezhikong.Server.userData.user;

import java.util.Arrays;
import java.util.concurrent.atomic.AtomicBoolean;

public class RecoveryCode {
    private static final Gson gson = new Gson();
    private RecoveryCode() {}

    /**
     * 生成恢复码
     * @param user 用户
     */
    public static void generateRecoveryCode(user user) {
        String[] recoveryCodes = new RecoveryCodeGenerator().generateCodes(16);
        JsonArray recoveryCodeArray = new JsonArray();
        for (String recoveryCode : recoveryCodes) {
            recoveryCodeArray.add(recoveryCode);
        }
        user.getUserInformation().setTotpRecoveryCode(gson.toJson(recoveryCodeArray));
        user.setUserInformation(user.getUserInformation());//触发持久化

        ServerTools.getServerInstanceOrThrow().getServerAPI().
                sendMessageToUser(user, "生成了一组新的恢复码：" + Arrays.toString(recoveryCodes));
    }

    /**
     * 验证恢复码
     * @param code 恢复码
     * @param user 用户
     * @return 是否验证成功
     * @apiNote 验证成功后的恢复码将会被销毁
     */
    public static boolean verifyRecoveryCode(String code, user user) {
        JsonArray recoveryCodeArray = JsonParser.parseString(user.getUserInformation().getTotpRecoveryCode()).getAsJsonArray();
        AtomicBoolean isExist = new AtomicBoolean(false);
        recoveryCodeArray.asList().removeIf(element -> {
            String recoveryCode = element.getAsString();
            if (recoveryCode.equals(code)) {
                isExist.set(true);
                return true;
            }
            return false;
        });
        user.getUserInformation().setTotpRecoveryCode(gson.toJson(recoveryCodeArray));
        user.setUserInformation(user.getUserInformation());//触发持久化
        return isExist.get(); // 如果恢复码存在，则移除并返回true，否则返回false
    }
}
