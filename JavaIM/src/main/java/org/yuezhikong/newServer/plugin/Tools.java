package org.yuezhikong.newServer.plugin;

import org.yuezhikong.newServer.IServerMain;
import org.yuezhikong.newServer.ServerMain;

@SuppressWarnings("unused")
public class Tools {
    /**
     * 实用工具类不应当被实例化
     */
    private Tools() {}

    /**
     * 获取服务器实例
     * @return 服务器实例
     */
    public static IServerMain getServerInstance()
    {
        return ServerMain.getServer();
    }
}
