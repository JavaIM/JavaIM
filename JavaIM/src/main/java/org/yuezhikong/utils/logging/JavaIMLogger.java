package org.yuezhikong.utils.logging;

import org.slf4j.Marker;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

class JavaIMLogger extends Logger {

    private final org.slf4j.Logger orig_logger;

    private static final List<CustomLogger> loggers = new CopyOnWriteArrayList<>();

    static void clearLoggers() {
        loggers.clear();
    }

    static void addLogger(CustomLogger logger) {
        loggers.add(logger);
    }

    static void removeLogger(CustomLogger logger) {
        loggers.remove(logger);
    }

    /**
     * 创建 JavaIM Logger 中间层
     *
     * @param orig_logger 原始 slf4j Logger
     */
    public JavaIMLogger(org.slf4j.Logger orig_logger) {
        this.orig_logger = orig_logger;
    }

    @Override
    public void ChatMsg(String msg) {
        super.ChatMsg(msg);
        loggers.forEach(logger -> logger.ChatMsg(msg));
        orig_logger.info(msg);
    }

    @Override
    protected void CustomLoggerRequest(String format, Object... params) {

    }

    @Override
    public String getName() {
        return "JavaIM Logger (Backend:" + orig_logger.getName() + ")";
    }

    @Override
    public boolean isTraceEnabled() {
        return orig_logger.isTraceEnabled();
    }


    @Override
    public void trace(String s) {
        super.trace(s);
        if (isTraceEnabled()) {
            orig_logger.trace(s);
            loggers.forEach(logger -> logger.trace(s));
        }
    }

    @Override
    public void trace(String s, Object o) {
        super.trace(s, o);
        if (isTraceEnabled()) {
            orig_logger.trace(s, o);
            loggers.forEach(logger -> logger.trace(s, o));
        }
    }

    @Override
    public void trace(String s, Object o, Object o1) {
        super.trace(s, o, o1);
        if (isTraceEnabled()) {
            orig_logger.trace(s, o, o1);
            loggers.forEach(logger -> logger.trace(s, o, o1));
        }
    }

    @Override
    public void trace(String s, Object... objects) {
        super.trace(s, objects);
        if (isTraceEnabled()) {
            orig_logger.trace(s, objects);
            loggers.forEach(logger -> logger.trace(s, objects));
        }
    }

    @Override
    public void trace(String s, Throwable throwable) {
        super.trace(s, throwable);
        if (isTraceEnabled()) {
            orig_logger.trace(s, throwable);
            loggers.forEach(logger -> logger.trace(s, throwable));
        }
    }

    @Override
    public boolean isTraceEnabled(Marker marker) {
        return orig_logger.isTraceEnabled(marker);
    }

    @Override
    public void trace(Marker marker, String s) {
        super.trace(marker, s);
        loggers.forEach(logger -> logger.trace(marker, s));
        orig_logger.trace(marker, s);
    }

    @Override
    public void trace(Marker marker, String s, Object o) {
        super.trace(marker, s, o);
        loggers.forEach(logger -> logger.trace(marker, s, o));
        orig_logger.trace(marker, s, o);
    }

    @Override
    public void trace(Marker marker, String s, Object o, Object o1) {
        super.trace(marker, s, o, o1);
        loggers.forEach(logger -> logger.trace(marker, s, o, o1));
        orig_logger.trace(marker, s, o, o1);
    }

    @Override
    public void trace(Marker marker, String s, Object... objects) {
        super.trace(marker, s, objects);
        loggers.forEach(logger -> logger.trace(marker, s, objects));
        orig_logger.trace(marker, s, objects);
    }

    @Override
    public void trace(Marker marker, String s, Throwable throwable) {
        super.trace(marker, s, throwable);
        loggers.forEach(logger -> logger.trace(marker, s, throwable));
        orig_logger.trace(marker, s, throwable);
    }

    @Override
    public boolean isDebugEnabled() {
        return orig_logger.isDebugEnabled();
    }

    @Override
    public void debug(String s) {
        if (isDebugEnabled()) {
            orig_logger.debug(s);
            loggers.forEach(logger -> logger.debug(s));
        }
    }

    @Override
    public void debug(String s, Object o) {
        if (isDebugEnabled()) {
            orig_logger.debug(s, o);
            loggers.forEach(logger -> logger.debug(s, o));
        }
    }

    @Override
    public void debug(String s, Object o, Object o1) {
        super.debug(s, o, o1);
        if (isDebugEnabled()) {
            orig_logger.debug(s, o, o1);
            loggers.forEach(logger -> logger.debug(s, o, o1));
        }
    }

    @Override
    public void debug(String s, Object... objects) {
        super.debug(s, objects);
        if (isDebugEnabled()) {
            orig_logger.debug(s, objects);
            loggers.forEach(logger -> logger.debug(s, objects));
        }
    }

    @Override
    public void debug(String s, Throwable throwable) {
        super.debug(s, throwable);
        if (isDebugEnabled()) {
            orig_logger.debug(s, throwable);
            loggers.forEach(logger -> logger.debug(s, throwable));
        }
    }

    @Override
    public boolean isDebugEnabled(Marker marker) {
        return orig_logger.isDebugEnabled(marker);
    }

    @Override
    public void debug(Marker marker, String s) {
        super.debug(marker, s);
        orig_logger.debug(marker, s);
        loggers.forEach(logger -> logger.debug(marker, s));
    }

    @Override
    public void debug(Marker marker, String s, Object o) {
        super.debug(marker, s, o);
        orig_logger.debug(marker, s, o);
        loggers.forEach(logger -> logger.debug(marker, s, o));
    }

    @Override
    public void debug(Marker marker, String s, Object o, Object o1) {
        super.debug(marker, s, o, o1);
        orig_logger.debug(marker, s, o, o1);
        loggers.forEach(logger -> logger.debug(marker, s, o, o1));
    }

    @Override
    public void debug(Marker marker, String s, Object... objects) {
        super.debug(marker, s, objects);
        orig_logger.debug(marker, s, objects);
        loggers.forEach(logger -> logger.debug(marker, s, objects));
    }

    @Override
    public void debug(Marker marker, String s, Throwable throwable) {
        super.debug(marker, s, throwable);
        orig_logger.debug(marker, s, throwable);
        loggers.forEach(logger -> logger.debug(marker, s, throwable));
    }

    @Override
    public boolean isInfoEnabled() {
        return orig_logger.isInfoEnabled();
    }

    @Override
    public void info(String s) {
        super.info(s);
        if (isInfoEnabled()) {
            orig_logger.info(s);
            loggers.forEach(logger -> logger.info(s));
        }
    }

    @Override
    public void info(String s, Object o) {
        super.info(s, o);
        if (isInfoEnabled()) {
            orig_logger.info(s, o);
            loggers.forEach(logger -> logger.info(s, o));
        }
    }

    @Override
    public void info(String s, Object o, Object o1) {
        super.info(s, o, o1);
        if (isInfoEnabled()) {
            orig_logger.info(s, o, o1);
            loggers.forEach(logger -> logger.info(s, o, o1));
        }
    }

    @Override
    public void info(String s, Object... objects) {
        super.info(s, objects);
        if (isInfoEnabled()) {
            orig_logger.info(s, objects);
            loggers.forEach(logger -> logger.info(s, objects));
        }
    }

    @Override
    public void info(String s, Throwable throwable) {
        super.info(s, throwable);
        if (isInfoEnabled()) {
            orig_logger.info(s, throwable);
            loggers.forEach(logger -> logger.info(s, throwable));
        }
    }

    @Override
    public boolean isInfoEnabled(Marker marker) {
        return orig_logger.isInfoEnabled(marker);
    }

    @Override
    public void info(Marker marker, String s) {
        super.info(marker, s);
        orig_logger.info(marker, s);
        loggers.forEach(logger -> logger.info(marker, s));
    }

    @Override
    public void info(Marker marker, String s, Object o) {
        super.info(marker, s, o);
        orig_logger.info(marker, s, o);
        loggers.forEach(logger -> logger.info(marker, s, o));
    }

    @Override
    public void info(Marker marker, String s, Object o, Object o1) {
        super.info(marker, s, o, o1);
        orig_logger.info(marker, s, o, o1);
        loggers.forEach(logger -> logger.info(marker, s, o, o1));
    }

    @Override
    public void info(Marker marker, String s, Object... objects) {
        super.info(marker, s, objects);
        orig_logger.info(marker, s, objects);
        loggers.forEach(logger -> logger.info(marker, s, objects));
    }

    @Override
    public void info(Marker marker, String s, Throwable throwable) {
        super.info(marker, s, throwable);
        orig_logger.info(marker, s, throwable);
        loggers.forEach(logger -> logger.info(marker, s, throwable));
    }

    @Override
    public boolean isWarnEnabled() {
        return orig_logger.isWarnEnabled();
    }

    @Override
    public void warn(String s) {
        super.warn(s);
        if (isWarnEnabled()) {
            orig_logger.warn(s);
            loggers.forEach(logger -> logger.warn(s));
        }
    }

    @Override
    public void warn(String s, Object o) {
        super.warn(s, o);
        if (isWarnEnabled()) {
            orig_logger.warn(s, o);
            loggers.forEach(logger -> logger.warn(s, o));
        }
    }

    @Override
    public void warn(String s, Object... objects) {
        super.warn(s, objects);
        if (isWarnEnabled()) {
            orig_logger.warn(s, objects);
            loggers.forEach(logger -> logger.warn(s, objects));
        }
    }

    @Override
    public void warn(String s, Object o, Object o1) {
        super.warn(s, o, o1);
        if (isWarnEnabled()) {
            orig_logger.warn(s, o, o1);
            loggers.forEach(logger -> logger.warn(s, o, o1));
        }
    }

    @Override
    public void warn(String s, Throwable throwable) {
        super.warn(s, throwable);
        if (isWarnEnabled()) {
            orig_logger.warn(s, throwable);
            loggers.forEach(logger -> logger.warn(s, throwable));
        }
    }

    @Override
    public boolean isWarnEnabled(Marker marker) {
        return orig_logger.isWarnEnabled(marker);
    }

    @Override
    public void warn(Marker marker, String s) {
        super.warn(marker, s);
        orig_logger.warn(marker, s);
        loggers.forEach(logger -> logger.warn(marker, s));
    }

    @Override
    public void warn(Marker marker, String s, Object o) {
        super.warn(marker, s, o);
        orig_logger.warn(marker, s, o);
        loggers.forEach(logger -> logger.warn(marker, s, o));
    }

    @Override
    public void warn(Marker marker, String s, Object o, Object o1) {
        super.warn(marker, s, o, o1);
        orig_logger.warn(marker, s, o, o1);
        loggers.forEach(logger -> logger.warn(marker, s, o, o1));
    }

    @Override
    public void warn(Marker marker, String s, Object... objects) {
        super.warn(marker, s, objects);
        orig_logger.warn(marker, s, objects);
        loggers.forEach(logger -> logger.warn(marker, s, objects));
    }

    @Override
    public void warn(Marker marker, String s, Throwable throwable) {
        super.warn(marker, s, throwable);
        orig_logger.warn(marker, s, throwable);
        loggers.forEach(logger -> logger.warn(marker, s, throwable));
    }

    @Override
    public boolean isErrorEnabled() {
        return orig_logger.isErrorEnabled();
    }

    @Override
    public void error(String s) {
        super.error(s);
        if (isErrorEnabled()) {
            orig_logger.error(s);
            loggers.forEach(logger -> logger.error(s));
        }
    }

    @Override
    public void error(String s, Object o) {
        super.error(s, o);
        if (isErrorEnabled()) {
            orig_logger.error(s, o);
            loggers.forEach(logger -> logger.error(s, o));
        }
    }

    @Override
    public void error(String s, Object o, Object o1) {
        super.error(s, o, o1);
        if (isErrorEnabled()) {
            orig_logger.error(s, o, o1);
            loggers.forEach(logger -> logger.error(s, o, o1));
        }
    }

    @Override
    public void error(String s, Object... objects) {
        super.error(s, objects);
        if (isErrorEnabled()) {
            orig_logger.error(s, objects);
            loggers.forEach(logger -> logger.error(s, objects));
        }
    }

    @Override
    public void error(String s, Throwable throwable) {
        super.error(s, throwable);
        if (isErrorEnabled()) {
            orig_logger.error(s, throwable);
            loggers.forEach(logger -> logger.error(s, throwable));
        }
    }

    @Override
    public boolean isErrorEnabled(Marker marker) {
        return orig_logger.isErrorEnabled(marker);
    }

    @Override
    public void error(Marker marker, String s) {
        super.error(marker, s);
        orig_logger.error(marker, s);
        loggers.forEach(logger -> logger.error(marker, s));
    }

    @Override
    public void error(Marker marker, String s, Object o) {
        super.error(marker, s, o);
        orig_logger.error(marker, s, o);
        loggers.forEach(logger -> logger.error(marker, s, o));
    }

    @Override
    public void error(Marker marker, String s, Object o, Object o1) {
        super.error(marker, s, o, o1);
        orig_logger.error(marker, s, o, o1);
        loggers.forEach(logger -> logger.error(marker, s, o, o1));
    }

    @Override
    public void error(Marker marker, String s, Object... objects) {
        super.error(marker, s, objects);
        orig_logger.error(marker, s, objects);
        loggers.forEach(logger -> logger.error(marker, s, objects));
    }

    @Override
    public void error(Marker marker, String s, Throwable throwable) {
        super.error(marker, s, throwable);
        orig_logger.error(marker, s, throwable);
        loggers.forEach(logger -> logger.error(marker, s, throwable));
    }
}
