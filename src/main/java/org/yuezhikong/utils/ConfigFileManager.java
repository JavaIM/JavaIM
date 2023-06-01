package org.yuezhikong.utils;

import org.jetbrains.annotations.NotNull;

import java.io.*;
import java.util.Properties;

public class ConfigFileManager {
    public void CreateServerprop(){
        Properties sprop = new Properties();
        try {
            sprop.setProperty("MAX_CLIENT", "-1");
            sprop.setProperty("EnableLoginSystem", "true");
            sprop.setProperty("Use_SQLITE_Mode", "true");
            sprop.setProperty("MySQLDataBaseHost", "127.0.0.1");
            sprop.setProperty("MySQLDataBasePort", "3306");
            sprop.setProperty("MySQLDataBaseName", "JavaIM");
            sprop.setProperty("MySQLDataBaseUser", "JavaIM");
            sprop.setProperty("MySQLDataBasePasswd", "JavaIM");
            sprop.store(new FileOutputStream("server.properties"), null);
        } catch (IOException ex) {
            SaveStackTrace.saveStackTrace(ex);
        }
    }
    public void CreateClientprop(){
        Properties cprop = new Properties();
        try {
            cprop.setProperty("GUIMode", "true");
            cprop.store(new FileOutputStream("client.properties"), null);
        } catch (IOException ex) {
            SaveStackTrace.saveStackTrace(ex);
        }
    }
    public static @NotNull Properties LoadClientProperties(){
        Properties prop = new Properties();
        try {
            prop.load(new FileInputStream("client.properties"));
        } catch (IOException ex) {
            SaveStackTrace.saveStackTrace(ex);
        }
        return prop;
    }
    public static @NotNull Properties LoadServerProperties(){
        Properties prop = new Properties();
        try {
            prop.load(new FileInputStream("server.properties"));
        } catch (IOException ex) {
            SaveStackTrace.saveStackTrace(ex);
        }
        return prop;
    }
}