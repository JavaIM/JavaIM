package org.yuezhikong.Server.UserData.tcpUser;

import org.yuezhikong.Server.UserData.user;
import org.yuezhikong.Server.network.NetworkServer;

public interface tcpUser extends user {
    /**
     * 获取此用户对应的网络客户端
     *
     * @return 网络客户端
     */
    NetworkServer.NetworkClient getNetworkClient();
}
