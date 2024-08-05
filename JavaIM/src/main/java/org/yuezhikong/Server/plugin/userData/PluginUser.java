package org.yuezhikong.Server.plugin.userData;

import org.yuezhikong.Server.userData.user;

public interface PluginUser extends user {

    /**
     * 写入信息到插件用户
     *
     * @param data 信息
     */
    void WriteData(String data);

    // 部分方法的默认处理程序
    @Override
    default boolean isServer() {
        return false;
    }
}
