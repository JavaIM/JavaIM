package org.yuezhikong.Server.plugin.event.events.User;

import lombok.Getter;
import org.yuezhikong.Server.UserData.user;
import org.yuezhikong.Server.plugin.event.Cancellable;
import org.yuezhikong.Server.plugin.event.events.Event;

/**
 * 用户聊天事件
 */
public class UserChatEvent implements Event, Cancellable {
    @Getter
    private final user UserData;
    @Getter
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
}
