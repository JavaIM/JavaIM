package org.yuezhikong.utils;

import org.apache.logging.log4j.LogManager;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;

/**
 * 调用堆栈保存到debug.log封装
 * @author AlexLiuDev233
 * @Date 2023/02/27
 */
public class SaveStackTrace {
    /**
     * 保存到debug.log的方法
     * @param e 发生的异常
     */
    public static void saveStackTrace(Throwable e)
    {
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
}
