/*
 * Simplified Chinese (简体中文)
 *
 * 版权所有 (C) 2023 QiLechan <qilechan@outlook.com> 和本程序的贡献者
 *
 * 本程序是自由软件：你可以再分发之和/或依照由自由软件基金会发布的 GNU 通用公共许可证修改之，无论是版本 3 许可证，还是 3 任何以后版都可以。
 * 发布该程序是希望它能有用，但是并无保障;甚至连可销售和符合某个特定的目的都不保证。请参看 GNU 通用公共许可证，了解详情。
 * 你应该随程序获得一份 GNU 通用公共许可证的副本。如果没有，请看 <https://www.gnu.org/licenses/>。
 * English (英语)
 *
 * Copyright (C) 2023 QiLechan <qilechan@outlook.com> and contributors to this program
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or 3 any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program. If not, see <https://www.gnu.org/licenses/>.
 */
package org.yuezhikong.utils;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;

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
            SaveStackTrace.saveStackTrace(e);
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
        System.out.print(">");
    }
}
