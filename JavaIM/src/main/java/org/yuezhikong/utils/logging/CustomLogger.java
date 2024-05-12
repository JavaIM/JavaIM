package org.yuezhikong.utils.logging;

import org.slf4j.Logger;

public interface CustomLogger extends Logger {
    /**
     * 聊天消息
     * @param msg 消息
     */
    void ChatMsg(String msg);
}
