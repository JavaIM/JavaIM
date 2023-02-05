package org.yuezhikong.utils;

import java.util.logging.Level;

public class Logger {
    public void info(String msg)
    {
        System.out.print("\b");
        java.util.logging.Logger.getGlobal().info(msg);
        try {
            Thread.sleep(30);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        System.out.print(">");
    }
    public void log(Level level,String msg)
    {
        System.out.print("\b");
        java.util.logging.Logger.getGlobal().log(level,msg);
        try {
            Thread.sleep(30);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        System.out.print(">");
    }
    public void warning(String msg)
    {
        System.out.print("\b");
        java.util.logging.Logger.getGlobal().warning(msg);
        try {
            Thread.sleep(30);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        System.out.print(">");
    }
}
