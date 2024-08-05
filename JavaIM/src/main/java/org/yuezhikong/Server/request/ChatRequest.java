package org.yuezhikong.Server.request;

import org.yuezhikong.Server.UserData.user;
import org.yuezhikong.Server.command.Command;
import org.yuezhikong.Server.plugin.Plugin.Plugin;

import java.util.List;

public interface ChatRequest {

    /**
     * 指令信息
     *
     * @param plugin          插件
     * @param commandInstance 指令
     * @param command         指令名
     * @apiNote 插件如果为null，则为本体创建的指令
     */
    record CommandInformation(Plugin plugin, Command commandInstance, String command) {
    }

    /**
     * 指令处理
     *
     * @param command 指令
     * @param args    参数
     * @param User    用户实例
     */
    void commandRequest(String command, String[] args, user User);

    /**
     * 获取注册的指令列表
     *
     * @return 指令信息列表
     */
    List<CommandInformation> getRegisterCommands();

    /**
     * 注册一条指令
     *
     * @param information 指令信息
     */
    void registerCommand(CommandInformation information);

    /**
     * 取消注册指令
     *
     * @param information 指令信息
     */
    void unregisterCommand(CommandInformation information);

    /**
     * 取消注册指令
     *
     * @param plugin 插件实例
     */
    void unregisterCommand(Plugin plugin);
}