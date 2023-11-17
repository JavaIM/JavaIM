package org.yuezhikong.newServer;

import org.yuezhikong.newServer.IServerMain;
import org.yuezhikong.newServer.NettyNetwork;
import org.yuezhikong.newServer.ServerMain;

@SuppressWarnings("unused")
public class ServerTools {
    /**
     * 实用工具类不应当被实例化
     */
    private ServerTools() {}

    /**
     * 获取服务器实例
     * @return 服务器实例
     */
    public static IServerMain getServerInstance()
    {
        if (ServerMain.getServer() == null && NettyNetwork.getNettyNetwork().ServerStartStatus())
            return NettyNetwork.getNettyNetwork();
        else
            return ServerMain.getServer();
    }
}
