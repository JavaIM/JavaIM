package org.yuezhikong.newServer.UserData.tcpUser;

import io.netty.channel.Channel;

public interface INettyUser extends tcpUser{

    /**
     * 获取Netty Channel
     * @return Channel
     */
    Channel getChannel();
}
