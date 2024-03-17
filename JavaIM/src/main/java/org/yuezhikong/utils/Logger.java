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
    private final boolean isStdOutRedistribution;
    /**
     * Logger初始化
     * @param isStdOutRedistribution 是否为System.out的重定向logger
     */
    public Logger(boolean isStdOutRedistribution)
    {
        this.isStdOutRedistribution = isStdOutRedistribution;
    }

    private static Logger Instance;
    /**
     * 获取Logger实例
     * @return Logger实例
     */
    public synchronized static Logger getInstance()
    {
        if (Instance == null)
            new Logger();
        return Instance;
    }
    private Logger()
    {
        this(false);
        Instance = this;
    }



    /**
     * 调用web管理页面的日志输出
     * @param msg 消息
     * @param isChatMessage 是否为聊天消息
     */
    private static void WebLoggerRequest(String msg, boolean isChatMessage)
    {
        //TODO 此处为对于web管理页面的预留，暂时不使用，作为后续进行扩展的接口
    }

    //Logger包装

    /**
     * 输出普通消息
     * @param msg 消息
     * @param params 参数
     */
    public void info(String msg, Object... params)
    {
        WebLoggerRequest("[普通] "+msg, false);
        logger_root.info(msg,params);
    }

    /**
     * 输出普通消息
     * @param msg 消息
     */
    public void info(String msg)
    {
        String Message = msg;
        if (isStdOutRedistribution)
        {
            Message = "[STDOUT] "+msg;
        }
        WebLoggerRequest("[普通] " + Message, false);
        logger_root.info(Message);
    }

    /**
     * 输出错误消息
     * @param msg 消息
     */
    public void error(String msg)
    {
        String Message = msg;
        if (isStdOutRedistribution)
        {
            Message = "[STDERR] "+msg;
        }
        WebLoggerRequest("[错误] " + Message, false);
        logger_root.error(Message);
    }

    /**
     * 输出聊天消息
     * @param msg 消息
     */
    public void ChatMsg(String msg)
    {
        WebLoggerRequest("[聊天消息] "+msg, true);
        logger_root.info(msg);
    }

    /**
     * 输出日志
     * @param level 消息等级
     * @param msg 消息
     */
    public void log(Level level, String msg)
    {
        WebLoggerRequest("[特殊日志] "+msg,false);
        logger_root.log(level,msg);
    }

    /**
     * 输出警告消息
     * @param msg 消息
     */
    public void warning(String msg)
    {
        WebLoggerRequest("[警告] "+msg, false);
        logger_root.warn(msg);
    }
}
