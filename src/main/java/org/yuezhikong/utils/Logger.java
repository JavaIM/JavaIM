package org.yuezhikong.utils;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;

//import java.util.logging.Level;

public class Logger {
    public static final org.apache.logging.log4j.Logger logger_root = LogManager.getLogger(Logger.class.getName());//配置文件没配置Log的class名字，所以用默认的Root

    public void info(String msg, Object... params)
    {
        System.out.print("\b");
        //java.util.logging.Logger.getGlobal().info(msg);
        logger_root.info(msg,params);
        try {
            Thread.sleep(30);
        } catch (InterruptedException e) {
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            e.printStackTrace(pw);
            pw.flush();
            sw.flush();
            org.apache.logging.log4j.Logger logger_log4j = LogManager.getLogger("Debug");
            logger_log4j.debug(sw.toString());
            pw.close();
            try {
                sw.close();
            }
            catch (IOException ex)
            {
                ex.printStackTrace();
            }
        }
        System.out.print(">");
    }
    public void info(String msg)
    {
        System.out.print("\b");
        //java.util.logging.Logger.getGlobal().info(msg);
        logger_root.info(msg);
        try {
            Thread.sleep(30);
        } catch (InterruptedException e) {
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            e.printStackTrace(pw);
            pw.flush();
            sw.flush();
            org.apache.logging.log4j.Logger logger_log4j = LogManager.getLogger("Debug");
            logger_log4j.debug(sw.toString());
            pw.close();
            try {
                sw.close();
            }
            catch (IOException ex)
            {
                ex.printStackTrace();
            }
        }
        System.out.print(">");
    }
    public void error(String msg)
    {
        System.out.print("\b");
        //java.util.logging.Logger.getGlobal().info(msg);
        logger_root.error(msg);
        try {
            Thread.sleep(30);
        } catch (InterruptedException e) {
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            e.printStackTrace(pw);
            pw.flush();
            sw.flush();
            org.apache.logging.log4j.Logger logger_log4j = LogManager.getLogger("Debug");
            logger_log4j.debug(sw.toString());
            pw.close();
            try {
                sw.close();
            }
            catch (IOException ex)
            {
                ex.printStackTrace();
            }
        }
        System.out.print(">");
    }
    public void log(Level level, String msg)
    {
        System.out.print("\b");
        //java.util.logging.Logger.getGlobal().log(level,msg);
        logger_root.log(level,msg);
        try {
            Thread.sleep(30);
        } catch (InterruptedException e) {
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            e.printStackTrace(pw);
            pw.flush();
            sw.flush();
            org.apache.logging.log4j.Logger logger_log4j = LogManager.getLogger("Debug");
            logger_log4j.debug(sw.toString());
            pw.close();
            try {
                sw.close();
            }
            catch (IOException ex)
            {
                ex.printStackTrace();
            }
        }
        System.out.print(">");
    }
    public void warning(String msg)
    {
        System.out.print("\b");
        logger_root.warn(msg);
        //java.util.logging.Logger.getGlobal().warning(msg);
        try {
            Thread.sleep(30);
        } catch (InterruptedException e) {
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            e.printStackTrace(pw);
            pw.flush();
            sw.flush();
            org.apache.logging.log4j.Logger logger_log4j = LogManager.getLogger("Debug");
            logger_log4j.debug(sw.toString());
            pw.close();
            try {
                sw.close();
            }
            catch (IOException ex)
            {
                ex.printStackTrace();
            }
        }
        System.out.print(">");
    }
}
