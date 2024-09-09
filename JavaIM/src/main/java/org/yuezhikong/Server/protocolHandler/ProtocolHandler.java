package org.yuezhikong.Server.protocolHandler;

import org.jetbrains.annotations.NotNull;
import org.yuezhikong.Server.IServer;
import org.yuezhikong.Server.network.NetworkServer;

/**
 * 协议处理器接口
 */
public interface ProtocolHandler {
    /**
     * 处理协议
     * @param server       服务器实例
     * @param protocolData 协议 json 数据
     * @param client       客户端
     */
    void handleProtocol(@NotNull IServer server,
                        @NotNull String protocolData,
                        @NotNull NetworkServer.NetworkClient client);
}
