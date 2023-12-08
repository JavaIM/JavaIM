package org.yuezhikong.newServer.api;

import org.jetbrains.annotations.NotNull;
import org.yuezhikong.newServer.NettyNetwork;
import org.yuezhikong.newServer.UserData.tcpUser.IClassicUser;
import org.yuezhikong.newServer.UserData.tcpUser.NettyUser;
import org.yuezhikong.newServer.UserData.user;
import org.yuezhikong.newServer.plugin.userData.PluginUser;
import org.yuezhikong.utils.SaveStackTrace;

import java.util.ArrayList;
import java.util.List;

public class NettyAPI extends SingleAPI{

    @Override
    public @NotNull List<user> GetValidClientList(boolean CheckLoginStatus) {
        List<user> AllClientList = network.getUsers();
        List<user> ValidClientList = new ArrayList<>();
        for (user User : AllClientList)
        {
            if (User == null)
                continue;
            if (CheckLoginStatus && !User.isUserLogined())
                continue;
            if (User instanceof NettyUser nettyUser && nettyUser.getChannel() == null)
                continue;
            else if (User instanceof IClassicUser classicUser) {
                if (classicUser.getUserNetworkData() == null)
                    continue;
                if (classicUser.getPublicKey() == null)
                    continue;
                if (classicUser.getUserAES() == null)
                    continue;
            }
            if (User.isServer())
                continue;
            ValidClientList.add(User);
        }
        return ValidClientList;
    }

    @Override
    public void SendJsonToClient(@NotNull user User, @NotNull String InputData)
    {
        if (User instanceof PluginUser)
        {
            //如果是插件用户，则直接调用插件用户中的方法
            ((PluginUser) User).WriteData(InputData);
            return;
        }
        try {
            if (User instanceof NettyUser)
            {
                network.SendData(InputData,((NettyUser) User).getChannel());
            }
            else
            {
                throw new RuntimeException("not support!");
            }
        } catch (Exception e)
        {
            SaveStackTrace.saveStackTrace(e);
        }
    }

    private final NettyNetwork network;
    public NettyAPI(NettyNetwork instance) { super(instance); network = instance; }
}
