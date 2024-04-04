package org.yuezhikong.Server.plugin.event.events.User;

import org.yuezhikong.Server.UserData.user;
import org.yuezhikong.Server.plugin.event.Cancellable;
import org.yuezhikong.Server.plugin.event.events.Event;

/**
 * 用户聊天事件
 */
public class UserChatEvent implements Event, Cancellable {
    private final user UserData;
    private final String ChatMessage;
    public UserChatEvent(user UserData, String ChatMessage)
    {
        this.UserData = UserData;
        this.ChatMessage = ChatMessage;
    }

    private boolean cancel = false;
    @Override
    public boolean isCancelled() {
        return cancel;
    }

    @Override
    public void setCancelled(boolean cancelled) {
        cancel = cancelled;
    }

    public user getUserData() {
        return UserData;
    }

    public String getChatMessage() {
        return ChatMessage;
    }
}
