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
import org.jetbrains.annotations.Contract;
import org.yuezhikong.GraphicalUserInterface.DefaultController;

import java.io.PrintStream;
import java.lang.annotation.Documented;

//import java.util.logging.Level;

public class Logger {
    static {
        //初始化System.out
        Out = System.out;
    }
    private static final PrintStream Out;

    /**
     * 获取原始Java标注输出流
     * @deprecated 插件等应当尽可能的使用Log4j2 Logger，而非使用System.out!
     * @see System#out
     * @return 标准输出流
     */
    @Deprecated
    @SuppressWarnings("unused")
    @Contract(pure = true)
    public static PrintStream getStdOut() {
        return Out;
    }

    public static final org.apache.logging.log4j.Logger logger_root = LogManager.getLogger(Logger.class.getName());//配置文件没配置Log的class名字，所以用默认的Root
    private boolean isStdOutRedistribution;
    /**
     * Logger初始化
     * @param GUIController 如果为GUI，填写GUI的Controller
     */
    public Logger(DefaultController GUIController)
    {
        if (GUIController != null)
        {
            isGUI = true;
            this.GUIController = GUIController;
        }
    }
    /**
     * Logger初始化
     * @param isStdOutRedistribution 是否为System.out的重定向logger
     */
    public Logger(boolean isStdOutRedistribution)
    {
        this.isStdOutRedistribution = isStdOutRedistribution;
    }
    //GUI相关
    private boolean isGUI;
    private DefaultController GUIController;
    private boolean OutDate = false;
    private void GUIRequest(String msg, boolean isChatMessage)
    {
        if (OutDate)
        {
            throw new RuntimeException("This Logger is OutDate!");
        }
        if (isGUI)
        {
            if (isChatMessage)
            {
                GUIController.WriteChatMessage(msg);
            }
            else
            {
                GUIController.WriteSystemLog(msg);
            }
        }

    }
    public void OutDate()
    {
        OutDate = true;
    }

    //Logger包装
    public void info(String msg, Object... params)
    {
        GUIRequest("[info] "+msg, false);
        logger_root.info(msg,params);
    }
    public void info(String msg)
    {
        String Message = msg;
        if (isStdOutRedistribution)
        {
            Message = "[stdout] "+msg;
        }
        GUIRequest("[info] " + Message, false);
        logger_root.info(Message);
    }
    public void error(String msg)
    {
        String Message = msg;
        if (isStdOutRedistribution)
        {
            Message = "[stderr] "+msg;
        }
        GUIRequest("[error] " + Message, false);
        logger_root.error(Message);
    }
    public void ChatMsg(String msg)
    {
        GUIRequest(msg, true);
        logger_root.info(msg);
    }
    public void log(Level level, String msg)
    {
        GUIRequest(msg,false);
        logger_root.log(level,msg);
    }
    public void warning(String msg)
    {
        GUIRequest("[warning] "+msg, false);
        logger_root.warn(msg);
    }
}
