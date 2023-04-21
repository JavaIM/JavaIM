package org.yuezhikong.utils;

public class CustomVar {
    public record Command(String Command, String[] argv) {
    }
    public static class KeyData {
        public java.security.PublicKey publicKey;
        public java.security.PrivateKey privateKey;
        public String PublicKey;
        public String PrivateKey;
    }
}
