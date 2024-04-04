package org.yuezhikong.Server.plugin.event.events.Server;

import org.yuezhikong.Server.UserData.user;
import org.yuezhikong.Server.plugin.event.Cancellable;
import org.yuezhikong.Server.plugin.event.events.Event;

/**
 * 后台发言事件
 */
public class ServerChatEvent implements Event, Cancellable {
    private final user serverUser;
    private final String ChatMessage;
    public user getServerUser() {
        return serverUser;
    }

    public String getServerChatMessage() {
        return ChatMessage;
    }

    public ServerChatEvent(user UserData, String ChatMessage)
    {
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
