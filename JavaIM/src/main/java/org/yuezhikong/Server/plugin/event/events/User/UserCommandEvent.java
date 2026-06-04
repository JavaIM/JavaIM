package org.yuezhikong.Server.plugin.event.events.User;

import lombok.Getter;
import org.yuezhikong.Server.userData.user;
import org.yuezhikong.Server.plugin.event.Cancellable;
import org.yuezhikong.Server.plugin.event.events.Event;

/**
 * 用户使用命令事件
 */
public class UserCommandEvent implements Event, Cancellable {
    private boolean cancel = false;
    @Getter
    private final user UserData;
    @Getter
    private final String command;
    @Getter
    private final String[] args;

    public UserCommandEvent(user UserData, String command, String[] args) {
        this.UserData = UserData;
        this.command = command;
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
