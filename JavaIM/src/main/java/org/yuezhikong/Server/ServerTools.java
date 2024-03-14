package org.yuezhikong.Server;

import org.yuezhikong.utils.checks;

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
        if (NettyServer.getNettyNetwork().ServerStartStatus())
            return NettyServer.getNettyNetwork();
        else
            return null;
    }

    /**
     * 获取服务端实例，如果未启动则抛出异常
     * @return 服务端实例
     * @throws IllegalStateException 服务端未启动
     */
    public static IServerMain getServerInstanceOrThrow() throws IllegalStateException{
        IServerMain server = getServerInstance();
        checks.checkState(server == null, "服务器未启动");
        return server;
    }
}
