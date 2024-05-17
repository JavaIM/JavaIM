package org.yuezhikong.Server.plugin.event.events.Server;

import org.slf4j.Logger;
import org.yuezhikong.Server.plugin.event.events.Event;

/**
 * 当创建受到JavaIM管理的Logger
 */
public class CreateProvidedLoggerEvent implements Event {
    private Logger logger;

    public CreateProvidedLoggerEvent(Logger logger) {
        this.logger = logger;
    }
    /**
     * 获取logger
     * @return logger
     */
    public Logger getLogger() {
        return logger;
    }

    /**
     * 设置Logger
     * @apiNote
     * <p>这个API用于设置返回的Logger</p>
     * <p><b>请在实现中委托给getLogger获取到的Logger</b></p>
     * @param logger 要设置的Logger
     */
    public void setLogger(Logger logger) {
        this.logger = logger;
    }
}
