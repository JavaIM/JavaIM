package org.yuezhikong.newServer.plugin.event.events;

import org.yuezhikong.newServer.UserData.user;

public class ServerChatEvent implements Event {
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
}
