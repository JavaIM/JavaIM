package org.yuezhikong.Server;

import com.google.gson.Gson;
import org.jetbrains.annotations.NotNull;
import org.yuezhikong.Server.UserData.Authentication.UserAuthentication;
import org.yuezhikong.Server.UserData.ConsoleUser;
import org.yuezhikong.Server.UserData.tcpUser.tcpUser;
import org.yuezhikong.Server.UserData.user;
import org.yuezhikong.Server.api.SingleAPI;
import org.yuezhikong.Server.api.api;
import org.yuezhikong.Server.network.NetworkServer;
import org.yuezhikong.Server.plugin.PluginManager;
import org.yuezhikong.Server.plugin.SimplePluginManager;
import org.yuezhikong.Server.plugin.event.events.ServerStopEvent;
import org.yuezhikong.Server.plugin.userData.PluginUser;
import org.yuezhikong.utils.Logger;
import org.yuezhikong.utils.Protocol.NormalProtocol;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * JavaIM服务端
 */
public class Server implements IServer {

    /**
     * 用户消息处理线程池
     */
    private final ExecutorService userMessageRequestThreadPool = Executors.newCachedThreadPool(new ThreadFactory() {
        private final AtomicInteger threadNumber = new AtomicInteger(1);
        @Override
        public Thread newThread(@NotNull Runnable r) {
            return new Thread(new ThreadGroup("UserThreadPool"),
                    r,"Message Request Thread #"+threadNumber.getAndIncrement());
        }
    });

    /**
     * 用户列表
     */
    private final List<user> users = new ArrayList<>();

    /**
     * 用户处理器
     */
    private final ChatRequest request = new ChatRequest(this);

    /**
     * 服务器控制台用户
     */
    private final user consoleUser = new ConsoleUser();

    /**
     * 插件管理器
     */
    private final PluginManager pluginManager = new SimplePluginManager(this);

    /**
     * 服务器API
     */
    private final api serverAPI = new SingleAPI(this) {
        @Override
        public void SendJsonToClient(@NotNull user User, @NotNull String InputData) {
            if (User instanceof PluginUser)
                //如果是插件用户，则直接调用插件用户中的方法
                ((PluginUser) User).WriteData(InputData);
            else if (User instanceof ConsoleUser)
                logger.info(InputData);
            else if (User instanceof tcpUser)
                ((tcpUser) User).getNetworkClient().send(InputData);
        }
    };

    /**
     * 日志记录器
     */
    private final Logger logger = Logger.getInstance();
    /**
     * 网络层服务器
     */
    private final NetworkServer networkServer;

    /**
     * 启动JavaIM服务端
     * @param networkServer 网络层服务器
     */
    public Server(NetworkServer networkServer) {
        this.networkServer = networkServer;
        if (Instance != null)
        {
            throw new RuntimeException("JavaIM Server is Already running!");
        }
    }

    private static Server Instance;
    /**
     * 获取JavaIM服务端实例
     * @return JavaIM服务端实例
     */
    public static Server getInstance() {
        return Instance;
    }

    @Override
    public NetworkServer getNetwork() {
        return networkServer;
    }

    @Override
    public void stop() {
        logger.info("JavaIM服务器正在关闭...");
        users.clear();
        pluginManager.callEvent(new ServerStopEvent());
        try {
            pluginManager.UnLoadAllPlugin();
        } catch (IOException ignored) {}
        userMessageRequestThreadPool.shutdownNow();
        System.gc();
        getNetwork().stop();
        Instance = null;
        logger.info("JavaIM服务器已关闭");
    }

    @Override
    public void onReceiveMessage(NetworkServer.NetworkClient client, String message) {
        tcpUser user = client.getUser();
        if (user.getUserAuthentication() == null){
            user.setUserAuthentication(new UserAuthentication(user, getIOThreadPool(), getPluginManager(), getServerAPI()));
        }
        Gson gson = new Gson();
        NormalProtocol protocol;
        protocol = gson.fromJson(message, NormalProtocol.class);
        if (protocol.getMessageHead() == null && protocol.getMessageBody() == null){
            if (user.isUserLogged()){
                return;
            }
            //如果消息头和消息体都为空，则说明是其他类型
            return;
        }
        switch (protocol.getMessageHead().getType()){
            case "Chat" -> {
                if (user.isUserLogged()){
                    return;
                }
                ChatRequest.ChatRequestInput input = new ChatRequest.ChatRequestInput(user, message);
                if (!request.UserChatRequests(input)){
                    protocol.getMessageBody().setMessage(input.getChatMessage());
                    logger.ChatMsg(input.getChatMessage());
                    serverAPI.SendJsonToClient(user, gson.toJson(protocol));
                }
            }
            case "ChangePassword" -> {
                if (user.isUserLogged()){
                    return;
                }
                getServerAPI().ChangeUserPassword(user, protocol.getMessageBody().getMessage());
            }
            case "options" -> {

            }
        }
    }

    @Override
    public ExecutorService getIOThreadPool() {
        return getNetwork().getIOThreadPool();
    }

    @Override
    public List<user> getUsers() {
        return Collections.unmodifiableList(users);
    }

    @Override
    public boolean RegisterUser(user User) {
        for (user ForEachUser : users)
        {
            if (ForEachUser.getUserName().equals(User.getUserName()))
                return false;
        }
        return users.add(User);
    }

    /**
     * 取消注册一个用户
     * @param User 用户
     */
    public void UnRegisterUser(user User) {
        users.remove(User);
    }

    @Override
    public ChatRequest getRequest() {
        return request;
    }

    @Override
    public user getConsoleUser() {
        return consoleUser;
    }

    @Override
    public PluginManager getPluginManager() {
        return pluginManager;
    }

    @Override
    public api getServerAPI() {
        return serverAPI;
    }

    @Override
    public Logger getLogger() {
        return logger;
    }
}
