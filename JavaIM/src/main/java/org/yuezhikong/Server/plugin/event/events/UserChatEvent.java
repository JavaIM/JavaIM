package org.yuezhikong.Server.plugin.event.events;

import org.yuezhikong.Server.UserData.user;

/**
 * 用户聊天事件
 */
public class UserChatEvent implements Event{
    private boolean Cancel = false;
    private final user UserData;
    private final String ChatMessage;
    public UserChatEvent(user UserData, String ChatMessage)
    {
        this.UserData = UserData;
        this.ChatMessage = ChatMessage;
    }

    public void setCancel(boolean cancel) {
        Cancel = cancel;
    }

    public boolean isCancel() {
        return Cancel;
    }

    public user getUserData() {
        return UserData;
    }

    public String getChatMessage() {
        return ChatMessage;
    }
}
