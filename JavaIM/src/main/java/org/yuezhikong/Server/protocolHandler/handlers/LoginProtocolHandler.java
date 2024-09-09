package org.yuezhikong.Server.protocolHandler.handlers;

import com.google.gson.Gson;
import org.jetbrains.annotations.NotNull;
import org.yuezhikong.Server.IServer;
import org.yuezhikong.Server.network.NetworkServer;
import org.yuezhikong.Server.protocolHandler.ProtocolHandler;
import org.yuezhikong.utils.Protocol.LoginProtocol;
import org.yuezhikong.utils.Protocol.SystemProtocol;

import java.util.Objects;

public class LoginProtocolHandler implements ProtocolHandler {
    @Override
    public void handleProtocol(@NotNull IServer server, @NotNull String protocolData, NetworkServer.@NotNull NetworkClient client) {
        if (client.getUser().isUserLogged()) {// 判断登录状态
            server.getServerAPI().sendMessageToUser(client.getUser(), "您已经登录过了");
            SystemProtocol protocol = new SystemProtocol();
            protocol.setType("Login");
            protocol.setMessage("Already Logged");
            String json = new Gson().toJson(protocol);
            server.getServerAPI().sendJsonToClient(client.getUser(), json, "SystemProtocol");
            return;
        }
        LoginProtocol loginProtocol = server.getGson().fromJson(protocolData, LoginProtocol.class);// 反序列化 json 到 object
        if (loginProtocol.getLoginPacketHead() == null || loginProtocol.getLoginPacketBody() == null) {
            SystemProtocol systemProtocol = new SystemProtocol();
            systemProtocol.setType("Error");
            systemProtocol.setMessage("Invalid Packet");
            server.getServerAPI().sendJsonToClient(client.getUser(), server.getGson().toJson(systemProtocol), "SystemProtocol");
            return;
        }
        if ("token".equals(loginProtocol.getLoginPacketHead().getType())) {//根据登录模式登录
            if (!Objects.requireNonNull(client.getUser().getUserAuthentication()).
                    doLogin(loginProtocol.getLoginPacketBody().getReLogin().getToken()))
                client.getUser().disconnect();
        } else if ("passwd".equals(loginProtocol.getLoginPacketHead().getType())) {
            if (loginProtocol.getLoginPacketBody().getNormalLogin().getUserName() == null ||
                    loginProtocol.getLoginPacketBody().getNormalLogin().getUserName().contains("\n") ||
                    loginProtocol.getLoginPacketBody().getNormalLogin().getUserName().contains("\r")) {
                server.getServerAPI().sendMessageToUser(client.getUser(), "用户名中出现非法字符");
                client.getUser().disconnect();
                return;
            }
            if (!Objects.requireNonNull(client.getUser().getUserAuthentication()).
                    doLogin(loginProtocol.getLoginPacketBody().getNormalLogin().getUserName(),
                            loginProtocol.getLoginPacketBody().getNormalLogin().getPasswd())) {
                client.getUser().disconnect();
            }
        } else
            client.getUser().disconnect();
    }
}
