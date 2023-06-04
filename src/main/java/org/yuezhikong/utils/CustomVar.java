package org.yuezhikong.utils;

import org.yuezhikong.Server.plugin.Plugin;

public class CustomVar {
    public record Command(String Command, String[] argv) {
    }
    public record PluginInformation(String PluginName, String PluginAuthor, String PluginVersion, Plugin plugin,boolean Registered) {
    }
    public record CommandInformation(String Command, String Help, Plugin plugin) {
    }
    public record UserAndPassword(String Username,String PassWord)
    {

    }
    public static class KeyData {
        public java.security.PublicKey publicKey;
        public java.security.PrivateKey privateKey;
        public String PublicKey;
        public String PrivateKey;
    }
}
