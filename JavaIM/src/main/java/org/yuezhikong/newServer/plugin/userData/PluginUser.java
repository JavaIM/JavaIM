package org.yuezhikong.newServer.plugin.userData;

import org.yuezhikong.newServer.UserData.user;

public interface PluginUser extends user {

    /**
     * 写入信息到插件用户
     * @param data 信息
     */
    void WriteData(String data);

    /**
     * 等待输入
     * @return 输入
     */
    String waitData();
}
