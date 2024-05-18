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
package org.yuezhikong.utils.logging;

import org.slf4j.Marker;

import java.io.PrintWriter;
import java.io.StringWriter;

public class Logger implements CustomLogger {

    private final org.slf4j.Logger orig_logger;

    /**
     * 自定义Logger处理
     * @param format 格式化
     * @param params 请求参数
     */
    protected void CustomLoggerRequest(String format, Object ... params)
    {
        // TODO 为即将到来的Web管理面板以及插件预留的方法
    }

    /**
     * 创建 JavaIM Logger 中间层
     * @param orig_logger 原始 slf4j Logger
     */
    public Logger(org.slf4j.Logger orig_logger)
    {
        this.orig_logger = orig_logger;
    }

    @Override
    public void ChatMsg(String msg) {
        info(msg);
        CustomLoggerRequest(msg);
    }

    @Override
    public String getName() {
        return "JavaIM Logger (Backend:"+orig_logger.getName()+")";
    }

    @Override
    public boolean isTraceEnabled() {
        return orig_logger.isTraceEnabled();
    }

    @Override
    public void trace(String s) {
        if (isTraceEnabled())
        {
            orig_logger.trace(s);
            CustomLoggerRequest(s);
        }
    }

    @Override
    public void trace(String s, Object o) {
        if (isTraceEnabled())
        {
            orig_logger.trace(s,o);
            CustomLoggerRequest(s,o);
        }
    }

    @Override
    public void trace(String s, Object o, Object o1) {
        if (isTraceEnabled())
        {
            orig_logger.trace(s,o,o1);
            CustomLoggerRequest(s,o,o1);
        }
    }

    @Override
    public void trace(String s, Object... objects) {
        if (isTraceEnabled())
        {
            orig_logger.trace(s,objects);
            CustomLoggerRequest(s,objects);
        }
    }

    @Override
    public void trace(String s, Throwable throwable) {
        if (isTraceEnabled())
        {
            orig_logger.trace(s,throwable);

            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            throwable.printStackTrace(pw);
            CustomLoggerRequest(s,sw.toString());
            pw.close();
        }
    }

    @Override
    public boolean isTraceEnabled(Marker marker) {
        warn("请注意，调用 Marker 将不会打印到 Web 管理面板!");
        return orig_logger.isTraceEnabled(marker);
    }

    @Override
    public void trace(Marker marker, String s) {
        orig_logger.trace(marker,s);
        warn("请注意，调用 Marker 将不会打印到 Web 管理面板!");
    }

    @Override
    public void trace(Marker marker, String s, Object o) {
        orig_logger.trace(marker,s,o);
        warn("请注意，调用 Marker 将不会打印到 Web 管理面板!");
    }

    @Override
    public void trace(Marker marker, String s, Object o, Object o1) {
        orig_logger.trace(marker,s,o,o1);
        warn("请注意，调用 Marker 将不会打印到 Web 管理面板!");
    }

    @Override
    public void trace(Marker marker, String s, Object... objects) {
        orig_logger.trace(marker,s,objects);
        warn("请注意，调用 Marker 将不会打印到 Web 管理面板!");
    }

    @Override
    public void trace(Marker marker, String s, Throwable throwable) {
        orig_logger.trace(marker,s, throwable);
        warn("请注意，调用 Marker 将不会打印到 Web 管理面板!");
    }

    @Override
    public boolean isDebugEnabled() {
        return orig_logger.isDebugEnabled();
    }

    @Override
    public void debug(String s) {
        if (isDebugEnabled())
        {
            orig_logger.debug(s);
            CustomLoggerRequest(s);
        }
    }

    @Override
    public void debug(String s, Object o) {
        if (isDebugEnabled())
        {
            orig_logger.debug(s,o);
            CustomLoggerRequest(s,o);
        }
    }

    @Override
    public void debug(String s, Object o, Object o1) {
        if (isDebugEnabled())
        {
            orig_logger.debug(s,o,o1);
            CustomLoggerRequest(s,o,o1);
        }
    }

    @Override
    public void debug(String s, Object... objects) {
        if (isDebugEnabled())
        {
            orig_logger.debug(s,objects);
            CustomLoggerRequest(s,objects);
        }
    }

    @Override
    public void debug(String s, Throwable throwable) {
        if (isDebugEnabled())
        {
            orig_logger.debug(s,throwable);

            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            throwable.printStackTrace(pw);
            CustomLoggerRequest(s,sw.toString());
            pw.close();
        }
    }

    @Override
    public boolean isDebugEnabled(Marker marker) {
        warn("请注意，调用 Marker 将不会打印到 Web 管理面板!");
        return orig_logger.isDebugEnabled(marker);
    }

    @Override
    public void debug(Marker marker, String s) {
        warn("请注意，调用 Marker 将不会打印到 Web 管理面板!");
        orig_logger.debug(marker,s);

    }

    @Override
    public void debug(Marker marker, String s, Object o) {
        warn("请注意，调用 Marker 将不会打印到 Web 管理面板!");
        orig_logger.debug(marker,s,o);
    }

    @Override
    public void debug(Marker marker, String s, Object o, Object o1) {
        warn("请注意，调用 Marker 将不会打印到 Web 管理面板!");
        orig_logger.debug(marker,s,o,o1);
    }

    @Override
    public void debug(Marker marker, String s, Object... objects) {
        warn("请注意，调用 Marker 将不会打印到 Web 管理面板!");
        orig_logger.debug(marker,s,objects);
    }

    @Override
    public void debug(Marker marker, String s, Throwable throwable) {
        warn("请注意，调用 Marker 将不会打印到 Web 管理面板!");
        orig_logger.debug(marker,s,throwable);
    }

    @Override
    public boolean isInfoEnabled() {
        return orig_logger.isInfoEnabled();
    }

    @Override
    public void info(String s) {
        if (isInfoEnabled())
        {
            orig_logger.info(s);
            CustomLoggerRequest(s);
        }
    }

    @Override
    public void info(String s, Object o) {
        if (isInfoEnabled())
        {
            orig_logger.info(s,o);
            CustomLoggerRequest(s,o);
        }
    }

    @Override
    public void info(String s, Object o, Object o1) {
        if (isInfoEnabled())
        {
            orig_logger.info(s,o,o1);
            CustomLoggerRequest(s,o,o1);
        }
    }

    @Override
    public void info(String s, Object... objects) {
        if (isInfoEnabled())
        {
            orig_logger.info(s,objects);
            CustomLoggerRequest(s,objects);
        }
    }

    @Override
    public void info(String s, Throwable throwable) {
        if (isInfoEnabled())
        {
            orig_logger.info(s,throwable);

            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            throwable.printStackTrace(pw);
            CustomLoggerRequest(s,sw.toString());
            pw.close();
        }
    }

    @Override
    public boolean isInfoEnabled(Marker marker) {
        warn("请注意，调用 Marker 将不会打印到 Web 管理面板!");
        return orig_logger.isInfoEnabled(marker);
    }

    @Override
    public void info(Marker marker, String s) {
        warn("请注意，调用 Marker 将不会打印到 Web 管理面板!");
        orig_logger.info(marker,s);
    }

    @Override
    public void info(Marker marker, String s, Object o) {
        warn("请注意，调用 Marker 将不会打印到 Web 管理面板!");
        orig_logger.info(marker,s,o);
    }

    @Override
    public void info(Marker marker, String s, Object o, Object o1) {
        warn("请注意，调用 Marker 将不会打印到 Web 管理面板!");
        orig_logger.info(marker,s,o,o1);
    }

    @Override
    public void info(Marker marker, String s, Object... objects) {
        warn("请注意，调用 Marker 将不会打印到 Web 管理面板!");
        orig_logger.info(marker,s,objects);
    }

    @Override
    public void info(Marker marker, String s, Throwable throwable) {
        warn("请注意，调用 Marker 将不会打印到 Web 管理面板!");
        orig_logger.info(marker,s,throwable);
    }

    @Override
    public boolean isWarnEnabled() {
        return orig_logger.isWarnEnabled();
    }

    @Override
    public void warn(String s) {
        if (isWarnEnabled())
        {
            orig_logger.warn(s);
            CustomLoggerRequest(s);
        }
    }

    @Override
    public void warn(String s, Object o) {
        if (isWarnEnabled())
        {
            orig_logger.warn(s,o);
            CustomLoggerRequest(s,o);
        }
    }

    @Override
    public void warn(String s, Object... objects) {
        if (isWarnEnabled())
        {
            orig_logger.warn(s,objects);
            CustomLoggerRequest(s,objects);
        }
    }

    @Override
    public void warn(String s, Object o, Object o1) {
        if (isWarnEnabled())
        {
            orig_logger.warn(s,o,o1);
            CustomLoggerRequest(s,o,o1);
        }
    }

    @Override
    public void warn(String s, Throwable throwable) {
        if (isWarnEnabled())
        {
            orig_logger.warn(s,throwable);

            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            throwable.printStackTrace(pw);

            CustomLoggerRequest(s,sw.toString());
            pw.close();
        }
    }

    @Override
    public boolean isWarnEnabled(Marker marker) {
        return orig_logger.isWarnEnabled(marker);
    }

    @Override
    public void warn(Marker marker, String s) {
        warn("请注意，调用 Marker 将不会打印到 Web 管理面板!");
        orig_logger.warn(marker,s);
    }

    @Override
    public void warn(Marker marker, String s, Object o) {
        warn("请注意，调用 Marker 将不会打印到 Web 管理面板!");
        orig_logger.warn(marker,s,o);
    }

    @Override
    public void warn(Marker marker, String s, Object o, Object o1) {
        warn("请注意，调用 Marker 将不会打印到 Web 管理面板!");
        orig_logger.warn(marker,s,o,o1);
    }

    @Override
    public void warn(Marker marker, String s, Object... objects) {
        warn("请注意，调用 Marker 将不会打印到 Web 管理面板!");
        orig_logger.warn(marker,s,objects);
    }

    @Override
    public void warn(Marker marker, String s, Throwable throwable) {
        warn("请注意，调用 Marker 将不会打印到 Web 管理面板!");
        orig_logger.warn(marker,s,throwable);
    }

    @Override
    public boolean isErrorEnabled() {
        return orig_logger.isErrorEnabled();
    }

    @Override
    public void error(String s) {
        if (isErrorEnabled())
        {
            orig_logger.error(s);
            CustomLoggerRequest(s);
        }
    }

    @Override
    public void error(String s, Object o) {
        if (isErrorEnabled())
        {
            orig_logger.error(s,o);
            CustomLoggerRequest(s,o);
        }
    }

    @Override
    public void error(String s, Object o, Object o1) {
        if (isErrorEnabled())
        {
            orig_logger.error(s,o,o1);
            CustomLoggerRequest(s,o,o1);
        }
    }

    @Override
    public void error(String s, Object... objects) {
        if (isErrorEnabled())
        {
            orig_logger.error(s,objects);
            CustomLoggerRequest(s,objects);
        }
    }

    @Override
    public void error(String s, Throwable throwable) {
        if (isErrorEnabled())
        {
            orig_logger.error(s,throwable);

            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            throwable.printStackTrace(pw);
            CustomLoggerRequest(s,sw.toString());
            pw.close();
        }
    }

    @Override
    public boolean isErrorEnabled(Marker marker) {
        return orig_logger.isErrorEnabled(marker);
    }

    @Override
    public void error(Marker marker, String s) {
        warn("请注意，调用 Marker 将不会打印到 Web 管理面板!");
        orig_logger.error(marker,s);
    }

    @Override
    public void error(Marker marker, String s, Object o) {
        warn("请注意，调用 Marker 将不会打印到 Web 管理面板!");
        orig_logger.error(marker,s,o);
    }

    @Override
    public void error(Marker marker, String s, Object o, Object o1) {
        warn("请注意，调用 Marker 将不会打印到 Web 管理面板!");
        orig_logger.error(marker,s,o,o1);
    }

    @Override
    public void error(Marker marker, String s, Object... objects) {
        warn("请注意，调用 Marker 将不会打印到 Web 管理面板!");
        orig_logger.error(marker,s,objects);
    }

    @Override
    public void error(Marker marker, String s, Throwable throwable) {
        warn("请注意，调用 Marker 将不会打印到 Web 管理面板!");
        orig_logger.error(marker,s,throwable);
    }
}
