package org.yuezhikong.Server.plugin.event.events.User;

import org.yuezhikong.Server.UserData.user;
import org.yuezhikong.Server.plugin.event.events.Event;

public class UserRemoveEvent implements Event {

    private final user User;
    public UserRemoveEvent(user User)
    {
        this.User = User;
    }

    public user getUser() {
        return User;
    }
}
