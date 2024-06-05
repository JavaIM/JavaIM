package org.yuezhikong.Server.plugin.event.events.User;

import lombok.Getter;
import org.yuezhikong.Server.UserData.user;
import org.yuezhikong.Server.plugin.event.Cancellable;
import org.yuezhikong.Server.plugin.event.events.Event;
import org.yuezhikong.utils.CustomVar;

/**
 * 用户使用命令事件
 */
public class UserCommandEvent implements Event, Cancellable {
    private boolean cancel = false;
    @Getter
    private final user UserData;
    @Getter
    private final CustomVar.Command Command;
    public UserCommandEvent(user UserData, CustomVar.Command Command)
    {
        this.UserData = UserData;
        this.Command = Command;
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
