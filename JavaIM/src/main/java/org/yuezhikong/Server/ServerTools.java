package org.yuezhikong.Server;

import org.yuezhikong.Server.network.NettyServer_OLD;
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
    public static IServer getServerInstance()
    {
        if (NettyServer_OLD.getNettyNetwork().ServerStartStatus())
            return NettyServer_OLD.getNettyNetwork();
        else
            return null;
    }

    /**
     * 获取服务端实例，如果未启动则抛出异常
     * @return 服务端实例
     * @throws IllegalStateException 服务端未启动
     */
    public static IServer getServerInstanceOrThrow() throws IllegalStateException{
        IServer server = getServerInstance();
        checks.checkState(server == null, "服务器未启动");
        return server;
    }
}
