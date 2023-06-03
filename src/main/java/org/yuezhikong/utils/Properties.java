package org.yuezhikong.utils;

import java.io.*;

public class Properties {
    public void CreateServerprop(){
        java.util.Properties sprop = new java.util.Properties();
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
            ex.printStackTrace();
        }
    }
    public void CreateClientprop(){
        java.util.Properties cprop = new java.util.Properties();
        try {
            cprop.setProperty("GUIMode", "true");
            cprop.store(new FileOutputStream("client.properties"), null);
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }
    public static java.util.Properties LoadClientProperties(){
        java.util.Properties prop = new java.util.Properties();
        try {
            prop.load(new FileInputStream("client.properties"));
        } catch (IOException ex) {
            ex.printStackTrace();
        }
        return prop;
    }
    public static java.util.Properties LoadServerProperties(){
        java.util.Properties prop = new java.util.Properties();
        try {
            prop.load(new FileInputStream("server.properties"));
        } catch (IOException ex) {
            ex.printStackTrace();
        }
        return prop;
    }
}