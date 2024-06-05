package org.yuezhikong.utils.logging;

import org.apache.logging.log4j.spi.LoggerContext;
import org.apache.logging.slf4j.Log4jLoggerFactory;
import org.apache.logging.slf4j.Log4jMarkerFactory;
import org.slf4j.ILoggerFactory;
import org.slf4j.IMarkerFactory;
import org.slf4j.Logger;

/**
 * <p>用于接管 SLF4J Logger日志的Service Provider</p>
 * <p>所有调用Slf4j getLogger方法最终都会调用这里</p>
 */
@SuppressWarnings("unused")
public class SLF4JServiceProvider extends org.apache.logging.slf4j.SLF4JServiceProvider {
    private ILoggerFactory loggerFactory;
    @Override
    public ILoggerFactory getLoggerFactory() {
        return loggerFactory;
    }

    @Override
    public void initialize() {
        super.initialize();
        IMarkerFactory markerFactory = super.getMarkerFactory();
        if (!(markerFactory instanceof Log4jMarkerFactory))
            throw new IllegalStateException("can not support "+markerFactory.getClass().getName()+" only support ");
        loggerFactory = new JavaIMLoggerFactory((Log4jMarkerFactory) markerFactory);
    }

    private static class JavaIMLoggerFactory extends Log4jLoggerFactory {
        public JavaIMLoggerFactory(Log4jMarkerFactory loggerFactory) {
            super(loggerFactory);
        }

        @Override
        protected Logger newLogger(String name, LoggerContext context) {
            return new JavaIMLogger(super.newLogger(name, context));
        }
    }
}
