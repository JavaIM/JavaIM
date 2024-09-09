package org.yuezhikong.Server.protocolHandler.handlers;

import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.yuezhikong.Server.IServer;
import org.yuezhikong.Server.network.NetworkServer;
import org.yuezhikong.Server.protocolHandler.ProtocolHandler;
import org.yuezhikong.Server.request.ChatRequestImpl;
import org.yuezhikong.utils.Protocol.ChatProtocol;
import org.yuezhikong.utils.logging.CustomLogger;

@Slf4j
public class ChatProtocolHandler implements ProtocolHandler {
    @Override
    public void handleProtocol(@NotNull IServer server, @NotNull String protocolData, NetworkServer.@NotNull NetworkClient client) {
        if (!client.getUser().isUserLogged()) { // 检查登录状态
            server.getServerAPI().sendMessageToUser(client.getUser(), "请先登录");
            return;
        }
        ChatProtocol protocol = server.getGson().fromJson(protocolData, ChatProtocol.class); // 反序列化 json 到 object
        if (((ChatRequestImpl) server.getRequest()).userChatRequests(client.getUser(), protocol.getMessage()))  // 检查是否允许发送消息
            return;
        ((CustomLogger) log).chatMsg("[" + client.getUser().getUserName() + "]:" + protocol.getMessage());// 打印消息到log

        ChatProtocol chatProtocol = new ChatProtocol();// 封装数据包发给所有用户
        chatProtocol.setSourceUserName(client.getUser().getUserName());
        chatProtocol.setMessage(protocol.getMessage());
        String SendProtocolData = server.getGson().toJson(chatProtocol);
        server.getServerAPI().getValidUserList(true).forEach((forEachUser) ->
                server.getServerAPI().sendJsonToClient(forEachUser, SendProtocolData, "ChatProtocol"));
    }
}