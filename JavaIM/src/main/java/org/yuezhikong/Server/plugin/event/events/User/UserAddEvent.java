package org.yuezhikong.Server.plugin.event.events.User;

import lombok.Getter;
import org.yuezhikong.Server.UserData.user;
import org.yuezhikong.Server.plugin.event.Cancellable;
import org.yuezhikong.Server.plugin.event.events.Event;

public class UserAddEvent implements Event, Cancellable {

    @Getter
    private final user User;

    public UserAddEvent(user User) {
        this.User = User;
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
