package org.yuezhikong.utils;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SHA256Test {

    @Test
    void sha256_knownInput_returnsExpectedHash() {
        // SHA-256 of "hello" is well-known
        assertEquals(
                "2cf24dba5fb0a30e26e83b2ac5b9e29e1b161e5c1fa7425e73043362938b9824",
                SHA256.sha256("hello")
        );
    }

    @Test
    void sha256_emptyString_returnsExpectedHash() {
        assertEquals(
                "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855",
                SHA256.sha256("")
        );
    }

    @Test
    void sha256_sameInput_returnsSameHash() {
        String input = "JavaIM Test String 123";
        String hash1 = SHA256.sha256(input);
        String hash2 = SHA256.sha256(input);
        assertEquals(hash1, hash2);
    }

    @Test
    void sha256_differentInputs_produceDifferentHashes() {
        String hash1 = SHA256.sha256("password1");
        String hash2 = SHA256.sha256("password2");
        assertNotEquals(hash1, hash2);
    }

    @Test
    void sha256_outputIs64HexChars() {
        String hash = SHA256.sha256("some input");
        assertEquals(64, hash.length());
        assertTrue(hash.matches("[0-9a-f]+"));
    }

    @Test
    void sha256_unicodeInput_producesValidHash() {
        String hash = SHA256.sha256("中文测试 😀");
        assertEquals(64, hash.length());
        assertTrue(hash.matches("[0-9a-f]+"));
    }

    @Test
    void sha256_nullInput_throwsNullPointerException() {
        assertThrows(NullPointerException.class, () -> SHA256.sha256(null));
    }
}
