package org.yuezhikong.utils.totp;

import org.junit.jupiter.api.Test;
import org.yuezhikong.Server.userData.user;
import org.yuezhikong.Server.userData.userInformation;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class TOTPCodeTest {

    @Test
    void verifyTOTPCode_invalidCode_returnsFalse() {
        user mockUser = mock(user.class);
        userInformation mockInfo = mock(userInformation.class);
        when(mockUser.getUserInformation()).thenReturn(mockInfo);
        when(mockInfo.getTotpSecret()).thenReturn("JBSWY3DPEHPK3PXP"); // valid base32 secret

        assertFalse(TOTPCode.verifyTOTPCode(mockUser, "000000"));
    }

    @Test
    void verifyTOTPCode_emptyCode_returnsFalse() {
        user mockUser = mock(user.class);
        userInformation mockInfo = mock(userInformation.class);
        when(mockUser.getUserInformation()).thenReturn(mockInfo);
        when(mockInfo.getTotpSecret()).thenReturn("JBSWY3DPEHPK3PXP");

        assertFalse(TOTPCode.verifyTOTPCode(mockUser, ""));
    }

    @Test
    void verifyTOTPCode_nullSecret_returnsFalse() {
        user mockUser = mock(user.class);
        userInformation mockInfo = mock(userInformation.class);
        when(mockUser.getUserInformation()).thenReturn(mockInfo);
        when(mockInfo.getTotpSecret()).thenReturn(null);

        assertFalse(TOTPCode.verifyTOTPCode(mockUser, "123456"));
    }
}
