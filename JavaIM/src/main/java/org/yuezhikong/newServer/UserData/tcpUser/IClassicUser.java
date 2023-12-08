package org.yuezhikong.newServer.UserData.tcpUser;

import org.yuezhikong.NetworkManager;
import org.yuezhikong.newServer.ServerMain;
import org.yuezhikong.newServer.UserData.user;

public interface IClassicUser extends tcpUser{
    /**
     * 设置接收消息线程
     *
     * @param thread   线程
     */
    user setRecvMessageThread(ServerMain.RecvMessageThread thread);

    /**
     * 返回用于接收消息的线程
     * @return 线程
     */
    ServerMain.RecvMessageThread getRecvMessageThread();

    /**
     * 返回用户的网络数据
     * @return 用户网络数据
     */
    NetworkManager.NetworkData getUserNetworkData();

}
