package org.yuezhikong.Server.plugin.event.events.Server;

import lombok.Getter;
import org.yuezhikong.Server.UserData.user;
import org.yuezhikong.Server.plugin.event.Cancellable;
import org.yuezhikong.Server.plugin.event.events.Event;

/**
 * 后台发言事件
 */

public class ServerChatEvent implements Event, Cancellable {
    @Getter
    private final user serverUser;
    @Getter
    private final String ChatMessage;

    public ServerChatEvent(user UserData, String ChatMessage) {
        this.serverUser = UserData;
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
