package org.yuezhikong.newServer;

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
        if (ServerMain.getServer() == null && NettyServer.getNettyNetwork().ServerStartStatus())
            return NettyServer.getNettyNetwork();
        else
            return ServerMain.getServer();
    }
}
