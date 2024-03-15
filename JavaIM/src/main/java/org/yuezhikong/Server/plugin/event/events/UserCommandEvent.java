package org.yuezhikong.Server.plugin.event.events;

import org.yuezhikong.Server.UserData.user;
import org.yuezhikong.utils.CustomVar;

/**
 * 用户使用命令事件
 */
public class UserCommandEvent implements Event{
    private boolean Cancel = false;
    private final user UserData;
    private final CustomVar.Command Command;
    public UserCommandEvent(user UserData, CustomVar.Command Command)
    {
        this.UserData = UserData;
        this.Command = Command;
    }

    public void setCancel(boolean cancel) {
        Cancel = cancel;
    }

    public boolean isCancel() {
        return Cancel;
    }

    public user getUserData() {
        return UserData;
    }

    public CustomVar.Command getCommand() {
        return Command;
    }
}
