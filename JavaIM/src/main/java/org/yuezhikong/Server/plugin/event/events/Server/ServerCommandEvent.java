package org.yuezhikong.Server.plugin.event.events.Server;

import lombok.Getter;
import org.yuezhikong.Server.UserData.user;
import org.yuezhikong.Server.plugin.event.Cancellable;
import org.yuezhikong.Server.plugin.event.events.Event;

/**
 * 用户使用命令事件
 */
public class ServerCommandEvent implements Event, Cancellable {
    private boolean cancel = false;
    @Getter
    private final user UserData;
    @Getter
    private final String command;
    @Getter
    private final String[] args;

    public ServerCommandEvent(user UserData, String Command, String[] args) {
        this.UserData = UserData;
        this.command = Command;
        this.args = args;
    }

    @Override
    public void setCancelled(boolean cancel) {
        this.cancel = cancel;
    }

    @Override
    public boolean isCancelled() {
        return cancel;
    }
}
