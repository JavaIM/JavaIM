package org.yuezhikong.utils;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.yuezhikong.GUITest.ServerGUI.Controller;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

//import java.util.logging.Level;

public class Logger {
    private final boolean IsGUIServerLogger;
    private final Controller GUIForServer;
    private final boolean IsGUIClientLogger;
    public static final org.apache.logging.log4j.Logger logger_root = LogManager.getLogger(Logger.class.getName());//配置文件没配置Log的class名字，所以用默认的Root
    public Logger(boolean GUIServerLogger, boolean GUIClientLogger, Controller GUIForServer)
    {
        this.GUIForServer = GUIForServer;
        IsGUIServerLogger = GUIServerLogger;
        IsGUIClientLogger = GUIClientLogger;
    }
    public void info(String msg, Object... params)
    {
        System.out.print("\b");
        //java.util.logging.Logger.getGlobal().info(msg);
        logger_root.info(msg,params);
        try {
            Thread.sleep(30);
        } catch (InterruptedException e) {
            SaveStackTrace.saveStackTrace(e);
        }
        if (IsGUIServerLogger)
        {
            LocalTime time = LocalTime.now();
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("[HH:mm:ss]");
            GUIForServer.WriteToServerLog(time.format(formatter)+" [info] "+msg);
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
            SaveStackTrace.saveStackTrace(e);
        }
        if (IsGUIServerLogger)
        {
            LocalTime time = LocalTime.now();
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("[HH:mm:ss]");
            GUIForServer.WriteToServerLog(time.format(formatter)+" [info] "+msg);
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
            SaveStackTrace.saveStackTrace(e);
        }
        if (IsGUIServerLogger)
        {
            LocalTime time = LocalTime.now();
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("[HH:mm:ss]");
            GUIForServer.WriteToServerLog(time.format(formatter)+" [error] "+msg);
        }
        System.out.print(">");
    }
    public void ChatMsg(String msg)
    {
        System.out.print("\b");
        //java.util.logging.Logger.getGlobal().info(msg);
        logger_root.info(msg);
        try {
            Thread.sleep(30);
        } catch (InterruptedException e) {
            SaveStackTrace.saveStackTrace(e);
        }
        if (IsGUIServerLogger)
        {
            LocalTime time = LocalTime.now();
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("[HH:mm:ss] ");
            GUIForServer.WriteToChatLog(time.format(formatter)+msg);
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
            SaveStackTrace.saveStackTrace(e);
        }
        if (IsGUIServerLogger)
        {
            LocalTime time = LocalTime.now();
            String LogLevel = "";
            if (level.equals(Level.ERROR))
            {
                LogLevel = "[error] ";
            }
            else if (level.equals(Level.DEBUG))
            {
                LogLevel = "[debug] ";
            }
            else if (level.equals(Level.FATAL))
            {
                LogLevel = "[fatal] ";
            }
            else if (level.equals(Level.INFO))
            {
                LogLevel = "[info] ";
            }
            else if (level.equals(Level.TRACE))
            {
                LogLevel = "[trace] ";
            }
            else if (level.equals(Level.WARN))
            {
                LogLevel = "[warning] ";
            }
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("[HH:mm:ss] ");
            GUIForServer.WriteToServerLog(time.format(formatter)+LogLevel+msg);
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
            SaveStackTrace.saveStackTrace(e);
        }
        if (IsGUIServerLogger)
        {
            LocalTime time = LocalTime.now();
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("[HH:mm:ss]");
            GUIForServer.WriteToServerLog(time.format(formatter)+" [warning] "+msg);
        }
        System.out.print(">");
    }
}
