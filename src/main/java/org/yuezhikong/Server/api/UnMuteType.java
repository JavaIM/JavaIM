package org.yuezhikong.Server.api;

public enum UnMuteType {
    /**
     * 禁言时间到，因此被解除禁言
     */
    TIMEOUT_UNMUTE,
    /**
     * 管理员使用指令解除禁言
     */
    ADMINISTRATOR_USE_COMMAND_UNMUTE,
    /**
     * 插件导致解除禁言
     */
    PLUGIN_UNMUTE
}
