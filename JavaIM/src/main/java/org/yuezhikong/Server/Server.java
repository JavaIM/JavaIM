package org.yuezhikong.Server;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import org.jetbrains.annotations.NotNull;
import org.yuezhikong.CodeDynamicConfig;
import org.yuezhikong.Server.UserData.Authentication.UserAuthentication;
import org.yuezhikong.Server.UserData.ConsoleUser;
import org.yuezhikong.Server.UserData.tcpUser.tcpUser;
import org.yuezhikong.Server.UserData.user;
import org.yuezhikong.Server.api.SingleAPI;
import org.yuezhikong.Server.api.api;
import org.yuezhikong.Server.network.NetworkServer;
import org.yuezhikong.Server.plugin.PluginManager;
import org.yuezhikong.Server.plugin.SimplePluginManager;
import org.yuezhikong.Server.plugin.event.events.Server.ServerChatEvent;
import org.yuezhikong.Server.plugin.event.events.Server.ServerCommandEvent;
import org.yuezhikong.Server.plugin.event.events.Server.ServerStopEvent;
import org.yuezhikong.Server.plugin.event.events.User.UserAddEvent;
import org.yuezhikong.Server.plugin.event.events.User.UserRemoveEvent;
import org.yuezhikong.Server.plugin.userData.PluginUser;
import org.yuezhikong.utils.CustomVar;
import org.yuezhikong.utils.Logger;
import org.yuezhikong.utils.Protocol.GeneralProtocol;
import org.yuezhikong.utils.Protocol.LoginProtocol;
import org.yuezhikong.utils.Protocol.NormalProtocol;
import org.yuezhikong.utils.SaveStackTrace;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * JavaIM服务端
 */
public final class Server implements IServer {

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
     * Gson
     */
    private final Gson gson = new Gson();

    /**
     * 服务器API
     */
    private final api serverAPI = new SingleAPI(this) {
        @Override
        public void SendJsonToClient(@NotNull user User, @NotNull String InputData, @NotNull String ProtocolType) {
            GeneralProtocol protocol = new GeneralProtocol();
            protocol.setProtocolVersion(CodeDynamicConfig.getProtocolVersion());
            protocol.setProtocolName(ProtocolType);
            protocol.setProtocolData(InputData);

            String SendData = gson.toJson(protocol);
            if (User instanceof PluginUser)
                //如果是插件用户，则直接调用插件用户中的方法
                ((PluginUser) User).WriteData(SendData);
            else if (User instanceof ConsoleUser)
                logger.info(SendData);
            else if (User instanceof tcpUser)
                ((tcpUser) User).getNetworkClient().send(SendData);
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
        logger.info("正在启动JavaIM");
        if (Instance != null)
        {
            throw new RuntimeException("JavaIM Server is Already running!");
        }
        this.networkServer = networkServer;
        Instance = this;
        new Thread(() -> {
            while (true) try {
                Scanner scanner = new Scanner(System.in);
                String consoleInput = scanner.nextLine();
                //判断是指令还是消息
                if (consoleInput.startsWith("/")) {
                    CustomVar.Command command = serverAPI.CommandFormat(consoleInput);

                    //通知发生事件
                    ServerCommandEvent commandEvent = new ServerCommandEvent(consoleUser,command);
                    getPluginManager().callEvent(commandEvent);
                    //判断是否被取消
                    if (commandEvent.isCancelled())
                        return;

                    //指令处理
                    request.CommandRequest0(consoleUser,command);
                }
                else {
                    //通知发生事件
                    ServerChatEvent chatEvent = new ServerChatEvent(consoleUser,consoleInput);
                    getPluginManager().callEvent(chatEvent);
                    //判断是否被取消
                    if (chatEvent.isCancelled())
                        continue;
                    //格式化&发送
                    ChatRequest.ChatRequestInput input = new ChatRequest.ChatRequestInput(getConsoleUser(),consoleInput);
                    request.ChatFormat(input);
                    logger.ChatMsg(input.getChatMessage());
                    serverAPI.SendMessageToAllClient(input.getChatMessage());
                }
            } catch (Throwable throwable) {
                logger.error("JavaIM User Command Thread 出现异常");
                logger.error("请联系开发者");
                SaveStackTrace.saveStackTrace(throwable);
            }
        },"User Command Thread").start();
        getPluginManager().LoadPluginOnDirectory(new File("./plugins"));
        logger.info("JavaIM 启动完成");
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

    /**
     * 处理普通协议
     *
     * @param protocol  协议
     * @param user      用户
     */
    private void HandleNormalProtocol(final NormalProtocol protocol, final tcpUser user)
    {
        switch (protocol.getType()){
            case "Chat" -> {
                if (!user.isUserLogged()){
                    serverAPI.SendMessageToUser(user,"请先登录");
                    return;
                }
                ChatRequest.ChatRequestInput input = new ChatRequest.ChatRequestInput(user, protocol.getMessage());
                if (!request.UserChatRequests(input)){
                    logger.ChatMsg(input.getChatMessage());
                    serverAPI.SendMessageToAllClient(input.getChatMessage());
                }
            }
            case "ChangePassword" -> {
                if (!user.isUserLogged()){
                    serverAPI.SendMessageToUser(user,"请先登录");
                    return;
                }
                getServerAPI().ChangeUserPassword(user, protocol.getMessage());
            }
            case "options" -> {

            }
            default -> getServerAPI().SendMessageToUser(user, "暂不支持此消息类型");
        }
    }

    /**
     * 处理登录协议
     *
     * @param loginProtocol  协议
     * @param user      用户
     */
    private void HandleLoginProtocol(final LoginProtocol loginProtocol, final tcpUser user) {
        if (user.isUserLogged())
        {
            serverAPI.SendMessageToUser(user,"您已经登录过了");
            NormalProtocol protocol = new NormalProtocol();
            protocol.setType("Login");
            protocol.setMessage("Already Logged");
            String json = new Gson().toJson(protocol);
            serverAPI.SendJsonToClient(user,json, "NormalProtocol");
            return;
        }
        if ("token".equals(loginProtocol.getLoginPacketHead().getType()))
        {
            if (!Objects.requireNonNull(user.getUserAuthentication()).
                    DoLogin(loginProtocol.getLoginPacketBody().getReLogin().getToken()))
                user.UserDisconnect();
        }
        else if ("passwd".equals(loginProtocol.getLoginPacketHead().getType()))
        {
            if (!Objects.requireNonNull(user.getUserAuthentication()).
                    DoLogin(loginProtocol.getLoginPacketBody().getNormalLogin().getUserName(),
                            loginProtocol.getLoginPacketBody().getNormalLogin().getPasswd()))
                user.UserDisconnect();
        }
        else
            user.UserDisconnect();
    }

    @Override
    public void onReceiveMessage(NetworkServer.NetworkClient client, String message) {
        tcpUser user = client.getUser();
        if (user.getUserAuthentication() == null)
            user.setUserAuthentication(new UserAuthentication(user, this));
        GeneralProtocol protocol;
        try {
            protocol = gson.fromJson(message, GeneralProtocol.class);
        } catch (JsonSyntaxException e)
        {
            NormalProtocol normalProtocol = new NormalProtocol();
            normalProtocol.setType("Error");
            normalProtocol.setMessage("Protocol analysis failed");
            serverAPI.SendJsonToClient(user,gson.toJson(normalProtocol),"NormalProtocol");
            return;
        }
        switch (protocol.getProtocolName())
        {
            case "NormalProtocol" -> HandleNormalProtocol(gson.fromJson(protocol.getProtocolData(), NormalProtocol.class),user);
            case "LoginProtocol" -> HandleLoginProtocol(gson.fromJson(protocol.getProtocolData(),LoginProtocol.class),user);
            case "TransferProtocol" -> getServerAPI().SendMessageToUser(user,"正在开发中...请稍后");
            default -> getServerAPI().SendMessageToUser(user,"暂不支持此协议");
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
        UserAddEvent addEvent = new UserAddEvent(User);
        getPluginManager().callEvent(addEvent);
        if (addEvent.isCancelled())
            return false;
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
    @Override
    public void UnRegisterUser(user User) {
        UserRemoveEvent removeEvent = new UserRemoveEvent(User);
        getPluginManager().callEvent(removeEvent);
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
