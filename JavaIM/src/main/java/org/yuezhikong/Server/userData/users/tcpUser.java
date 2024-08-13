package org.yuezhikong.Server.userData.users;

import org.yuezhikong.Server.network.NetworkServer;
import org.yuezhikong.Server.userData.user;

public interface tcpUser extends user {
    /**
     * 获取此用户对应的网络客户端
     *
     * @return 网络客户端
     */
    NetworkServer.NetworkClient getNetworkClient();
}
