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

import org.jetbrains.annotations.MustBeInvokedByOverriders;
import org.slf4j.Marker;

public abstract class Logger implements CustomLogger {

    /**
     * 自定义Logger处理
     *
     * @param format 格式化
     * @param params 请求参数
     */
    // 为插件等预留的方法
    protected abstract void CustomLoggerRequest(String format, Object... params);

    @Override
    @MustBeInvokedByOverriders
    public void trace(String msg) {
        CustomLoggerRequest(msg);
    }

    @Override
    @MustBeInvokedByOverriders
    public void trace(String format, Object arg) {
        CustomLoggerRequest(format, arg);
    }

    @Override
    @MustBeInvokedByOverriders
    public void trace(String format, Object arg1, Object arg2) {
        CustomLoggerRequest(format, arg1, arg2);
    }

    @Override
    @MustBeInvokedByOverriders
    public void trace(String format, Object... arguments) {
        CustomLoggerRequest(format, arguments);
    }

    @Override
    @MustBeInvokedByOverriders
    public void trace(String msg, Throwable t) {
        if (t == null) {
            CustomLoggerRequest(msg);
            return;
        }
        CustomLoggerRequest(msg, t);
    }

    @Override
    @MustBeInvokedByOverriders
    public void trace(Marker marker, String msg) {
        CustomLoggerRequest(msg);
    }

    @Override
    @MustBeInvokedByOverriders
    public void trace(Marker marker, String format, Object arg1, Object arg2) {
        CustomLoggerRequest(format, arg1, arg2);
    }

    @Override
    @MustBeInvokedByOverriders
    public void trace(Marker marker, String format, Object arg) {
        CustomLoggerRequest(format, arg);
    }

    @Override
    @MustBeInvokedByOverriders
    public void trace(Marker marker, String format, Object... argArray) {
        CustomLoggerRequest(format, argArray);
    }

    @Override
    @MustBeInvokedByOverriders
    public void trace(Marker marker, String msg, Throwable t) {
        if (t == null) {
            CustomLoggerRequest(msg);
            return;
        }
        CustomLoggerRequest(msg, t);
    }

    @Override
    @MustBeInvokedByOverriders
    public void debug(String s, Object o, Object o1) {
        CustomLoggerRequest(s, o, o1);
    }

    @Override
    @MustBeInvokedByOverriders
    public void debug(String s, Object... objects) {
        CustomLoggerRequest(s, objects);
    }

    @Override
    @MustBeInvokedByOverriders
    public void debug(String s, Throwable throwable) {
        if (throwable == null) {
            CustomLoggerRequest(s);
            return;
        }
        CustomLoggerRequest(s, throwable);
    }

    @Override
    @MustBeInvokedByOverriders
    public void debug(Marker marker, String s) {
        CustomLoggerRequest(s);
    }

    @Override
    @MustBeInvokedByOverriders
    public void debug(Marker marker, String s, Object o) {
        CustomLoggerRequest(s, o);
    }

    @Override
    @MustBeInvokedByOverriders
    public void debug(Marker marker, String s, Object o, Object o1) {
        CustomLoggerRequest(s, o1);
    }

    @Override
    @MustBeInvokedByOverriders
    public void debug(Marker marker, String s, Object... objects) {
        CustomLoggerRequest(s, objects);
    }

    @Override
    @MustBeInvokedByOverriders
    public void debug(Marker marker, String s, Throwable throwable) {
        if (throwable == null) {
            CustomLoggerRequest(s);
            return;
        }
        CustomLoggerRequest(s, throwable);
    }

    @Override
    @MustBeInvokedByOverriders
    public void info(String s) {
        CustomLoggerRequest(s);
    }

    @Override
    @MustBeInvokedByOverriders
    public void info(String s, Object o) {
        CustomLoggerRequest(s, o);
    }

    @Override
    @MustBeInvokedByOverriders
    public void info(String s, Object o, Object o1) {
        CustomLoggerRequest(s, o, o1);
    }

    @Override
    @MustBeInvokedByOverriders
    public void info(String s, Object... objects) {
        CustomLoggerRequest(s, objects);
    }

    @Override
    @MustBeInvokedByOverriders
    public void info(String s, Throwable throwable) {
        if (throwable == null) {
            CustomLoggerRequest(s);
            return;
        }
        CustomLoggerRequest(s, throwable);
    }

    @Override
    @MustBeInvokedByOverriders
    public void info(Marker marker, String s) {
        CustomLoggerRequest(s);
    }

    @Override
    @MustBeInvokedByOverriders
    public void info(Marker marker, String s, Object o) {
        CustomLoggerRequest(s, o);
    }

    @Override
    @MustBeInvokedByOverriders
    public void info(Marker marker, String s, Object o, Object o1) {
        CustomLoggerRequest(s, o, o1);
    }

    @Override
    @MustBeInvokedByOverriders
    public void info(Marker marker, String s, Object... objects) {
        CustomLoggerRequest(s, objects);
    }

    @Override
    @MustBeInvokedByOverriders
    public void info(Marker marker, String s, Throwable throwable) {
        if (throwable == null) {
            CustomLoggerRequest(s);
            return;
        }
        CustomLoggerRequest(s, throwable);
    }

    @Override
    @MustBeInvokedByOverriders
    public void warn(String s) {
        CustomLoggerRequest(s);
    }

    @Override
    @MustBeInvokedByOverriders
    public void warn(String s, Object o) {
        CustomLoggerRequest(s, o);
    }

    @Override
    @MustBeInvokedByOverriders
    public void warn(String s, Object... objects) {
        CustomLoggerRequest(s, objects);
    }

    @Override
    @MustBeInvokedByOverriders
    public void warn(String s, Object o, Object o1) {
        CustomLoggerRequest(s, o, o1);
    }

    @Override
    @MustBeInvokedByOverriders
    public void warn(String s, Throwable throwable) {
        if (throwable == null) {
            CustomLoggerRequest(s);
            return;
        }
        CustomLoggerRequest(s, throwable);
    }

    @Override
    @MustBeInvokedByOverriders
    public void warn(Marker marker, String s) {
        CustomLoggerRequest(s);
    }

    @Override
    @MustBeInvokedByOverriders
    public void warn(Marker marker, String s, Object o) {
        CustomLoggerRequest(s, o);
    }

    @Override
    @MustBeInvokedByOverriders
    public void warn(Marker marker, String s, Object o, Object o1) {
        CustomLoggerRequest(s, o, o1);
    }

    @Override
    @MustBeInvokedByOverriders
    public void warn(Marker marker, String s, Object... objects) {
        CustomLoggerRequest(s, objects);
    }

    @Override
    @MustBeInvokedByOverriders
    public void warn(Marker marker, String s, Throwable throwable) {
        if (throwable == null) {
            CustomLoggerRequest(s);
            return;
        }
        CustomLoggerRequest(s, throwable);
    }

    @Override
    @MustBeInvokedByOverriders
    public void error(String s) {
        CustomLoggerRequest(s);
    }

    @Override
    @MustBeInvokedByOverriders
    public void error(String s, Object o) {
        CustomLoggerRequest(s, o);
    }

    @Override
    @MustBeInvokedByOverriders
    public void error(String s, Object o, Object o1) {
        CustomLoggerRequest(s, o, o1);
    }

    @Override
    @MustBeInvokedByOverriders
    public void error(String s, Object... objects) {
        CustomLoggerRequest(s, objects);
    }

    @Override
    @MustBeInvokedByOverriders
    public void error(String s, Throwable throwable) {
        if (throwable == null) {
            CustomLoggerRequest(s);
            return;
        }
        CustomLoggerRequest(s, throwable);
    }

    @Override
    @MustBeInvokedByOverriders
    public void error(Marker marker, String s) {
        CustomLoggerRequest(s);
    }

    @Override
    @MustBeInvokedByOverriders
    public void error(Marker marker, String s, Object o) {
        CustomLoggerRequest(s, o);
    }

    @Override
    @MustBeInvokedByOverriders
    public void error(Marker marker, String s, Object o, Object o1) {
        CustomLoggerRequest(s, o, o1);
    }

    @Override
    @MustBeInvokedByOverriders
    public void error(Marker marker, String s, Object... objects) {
        CustomLoggerRequest(s, objects);
    }

    @Override
    @MustBeInvokedByOverriders
    public void error(Marker marker, String s, Throwable throwable) {
        if (throwable == null) {
            CustomLoggerRequest(s);
            return;
        }
        CustomLoggerRequest(s, throwable);
    }

    @Override
    @MustBeInvokedByOverriders
    public void ChatMsg(String msg) {
        CustomLoggerRequest(msg);
    }
}