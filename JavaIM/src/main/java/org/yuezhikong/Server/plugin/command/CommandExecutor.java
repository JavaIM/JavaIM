package org.yuezhikong.Server.plugin.command;

import org.yuezhikong.Server.UserData.user;

public interface CommandExecutor {
    /**
     * 注册的指令被执行时执行
     * @param User 用户
     * @param Command 命令名
     * @param argv 命令参数
     */
    void execute(user User, String Command, String[] argv);
}
