package org.yuezhikong.Server;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import org.apache.ibatis.session.SqlSession;
import org.jetbrains.annotations.NotNull;
import org.slf4j.LoggerFactory;
import org.yuezhikong.CodeDynamicConfig;
import org.yuezhikong.Main;
import org.yuezhikong.Server.UserData.Authentication.UserAuthentication;
import org.yuezhikong.Server.UserData.ConsoleUser;
import org.yuezhikong.Server.UserData.tcpUser.tcpUser;
import org.yuezhikong.Server.UserData.user;
import org.yuezhikong.Server.api.SingleAPI;
import org.yuezhikong.Server.api.api;
import org.yuezhikong.Server.network.NetworkServer;
import org.yuezhikong.Server.network.SSLNettyServer;
import org.yuezhikong.Server.plugin.PluginManager;
import org.yuezhikong.Server.plugin.SimplePluginManager;
import org.yuezhikong.Server.plugin.event.events.Server.ServerChatEvent;
import org.yuezhikong.Server.plugin.event.events.Server.ServerCommandEvent;
import org.yuezhikong.Server.plugin.event.events.Server.ServerStopEvent;
import org.yuezhikong.Server.plugin.event.events.Server.onServerStartSuccessfulEvent;
import org.yuezhikong.Server.plugin.event.events.User.UserAddEvent;
import org.yuezhikong.Server.plugin.event.events.User.UserRemoveEvent;
import org.yuezhikong.Server.plugin.userData.PluginUser;
import org.yuezhikong.utils.logging.CustomLogger;
import org.yuezhikong.utils.CustomVar;
import org.yuezhikong.utils.DatabaseHelper;
import org.yuezhikong.utils.Protocol.ChatProtocol;
import org.yuezhikong.utils.Protocol.GeneralProtocol;
import org.yuezhikong.utils.Protocol.LoginProtocol;
import org.yuezhikong.utils.Protocol.SystemProtocol;
import org.yuezhikong.utils.SaveStackTrace;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * JavaIM服务端
 */
public final class Server implements IServer{

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
    private CustomLogger logger;
    /**
     * 网络层服务器
     */
    private NetworkServer networkServer;

    /**
     * Mybatis会话
     */
    private SqlSession sqlSession;

    @Override
    public SqlSession getSqlSession() {
        return sqlSession;
    }

    boolean startSuccessful = false;
    @Override
    public boolean isServerCompleateStart() {
        return startSuccessful;
    }

    /**
     * 启动JavaIM服务端
     * @param ListenPort 监听的端口
     */
    public void start(int ListenPort) {
        long startUnix = System.currentTimeMillis();
        LoggerFactory.getLogger(Main.class).info("正在启动JavaIM");
        if (Instance != null)
            throw new RuntimeException("JavaIM Server is already running!");
        Instance = this;

        ForkJoinPool forkJoinPool = new ForkJoinPool();

        LoggerFactory.getLogger(Main.class).info("正在预加载插件");
        getPluginManager().PreloadPluginOnDirectory(new File("./plugins"), forkJoinPool);
        LoggerFactory.getLogger(Main.class).info("插件预加载完成");

        logger = (CustomLogger) LoggerFactory.getLogger(Server.class);//初始化日志

        new Thread(() -> {
            logger.info("正在处理数据库");
            //获取JDBCUrl,创建表与自动更新数据库
            String JDBCUrl = DatabaseHelper.InitDataBase();
            //初始化Mybatis
            sqlSession = DatabaseHelper.InitMybatis(JDBCUrl);
            logger.info("数据库启动完成");

            logger.info("正在加载插件");
            getPluginManager().LoadPluginOnDirectory(new File("./plugins"), forkJoinPool);
            logger.info("插件加载完成");

            Thread UserCommandRequestThread = new Thread(() -> {
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
                        logger.ChatMsg("[Server]:"+consoleInput);
                        ChatProtocol chatProtocol = new ChatProtocol();
                        chatProtocol.setSourceUserName("Server");
                        chatProtocol.setMessage(consoleInput);
                        String SendProtocolData = gson.toJson(chatProtocol);
                        serverAPI.GetValidClientList(true).forEach((user) -> {
                            serverAPI.SendJsonToClient(user,SendProtocolData,"ChatProtocol");
                        });
                    }
                } catch (Throwable throwable) {
                    logger.error("JavaIM User Command Thread 出现异常");
                    logger.error("请联系开发者");
                    SaveStackTrace.saveStackTrace(throwable);
                }
            },"User Command Thread");
            if (!networkServer.isRunning()) {
                synchronized (SSLNettyServer.class) {
                    if (!networkServer.isRunning()) {
                        try {
                            SSLNettyServer.class.wait();
                        } catch (InterruptedException ignored) {

                        }
                    }
                }
            }

            logger.info("正在启动用户指令处理线程");
            UserCommandRequestThread.start();
            logger.info("用户指令处理线程启动完成");

            forkJoinPool.shutdownNow();
            logger.info("JavaIM 启动完成 (耗时:{}ms)",System.currentTimeMillis() - startUnix);
            startSuccessful = true;
            getPluginManager().callEvent(new onServerStartSuccessfulEvent());
        }, "Server Thread").start();

        Thread.currentThread().setName("Network Thread");
        networkServer = new SSLNettyServer();
        networkServer.start(ListenPort, forkJoinPool);
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
        sqlSession.close();
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
     * 处理聊天协议
     * @param protocol 聊天协议
     * @param user 用户
     */
    private void HandleChatProtocol(final ChatProtocol protocol, final tcpUser user)
    {
        if (!user.isUserLogged()){
            getServerAPI().SendMessageToUser(user,"请先登录");
            return;
        }
        ChatRequest.ChatRequestInput input = new ChatRequest.ChatRequestInput(user, protocol.getMessage());
        if (!getRequest().UserChatRequests(input)){
            getLogger().ChatMsg("["+user.getUserName()+"]:"+input.getChatMessage());

            ChatProtocol chatProtocol = new ChatProtocol();
            chatProtocol.setSourceUserName(user.getUserName());
            chatProtocol.setMessage(input.getChatMessage());
            String SendProtocolData = gson.toJson(chatProtocol);
            serverAPI.GetValidClientList(true).forEach((forEachUser) -> {
                serverAPI.SendJsonToClient(forEachUser,SendProtocolData,"ChatProtocol");
            });
        }
    }

    /**
     * 处理系统协议
     *
     * @param protocol  协议
     * @param user      用户
     */
    private void HandleSystemProtocol(final SystemProtocol protocol, final tcpUser user)
    {
        switch (protocol.getType()){
            case "ChangePassword" -> {
                if (!user.isUserLogged()){
                    getServerAPI().SendMessageToUser(user,"请先登录");
                    return;
                }
                getServerAPI().ChangeUserPassword(user, protocol.getMessage());
            }
            case "Login","Error","DisplayMessage" -> {
                SystemProtocol systemProtocol = new SystemProtocol();
                systemProtocol.setType("Error");
                systemProtocol.setMessage("Invalid Protocol Type, Please Check it");
                getServerAPI().SendJsonToClient(user, gson.toJson(systemProtocol), "SystemProtocol");
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
            SystemProtocol protocol = new SystemProtocol();
            protocol.setType("Login");
            protocol.setMessage("Already Logged");
            String json = new Gson().toJson(protocol);
            serverAPI.SendJsonToClient(user,json, "SystemProtocol");
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
            if (loginProtocol.getLoginPacketBody().getNormalLogin().getUserName() == null ||
                    loginProtocol.getLoginPacketBody().getNormalLogin().getUserName().contains("\n") ||
                    loginProtocol.getLoginPacketBody().getNormalLogin().getUserName().contains("\r"))
            {
                getServerAPI().SendMessageToUser(user, "用户名中出现非法字符");
                user.UserDisconnect();
                return;
            }
            if (!Objects.requireNonNull(user.getUserAuthentication()).
                    DoLogin(loginProtocol.getLoginPacketBody().getNormalLogin().getUserName(),
                            loginProtocol.getLoginPacketBody().getNormalLogin().getPasswd())) {
                user.UserDisconnect();
            }
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
            SystemProtocol systemProtocol = new SystemProtocol();
            systemProtocol.setType("Error");
            systemProtocol.setMessage("Protocol analysis failed");
            serverAPI.SendJsonToClient(user,gson.toJson(systemProtocol),"SystemProtocol");
            return;
        }
        if (protocol.getProtocolVersion() != CodeDynamicConfig.getProtocolVersion())
        {
            SystemProtocol systemProtocol = new SystemProtocol();
            systemProtocol.setType("Error");
            systemProtocol.setMessage("Protocol version not support");
            serverAPI.SendJsonToClient(user,gson.toJson(systemProtocol),"SystemProtocol");
            return;
        }

        switch (protocol.getProtocolName())
        {
            case "SystemProtocol" -> HandleSystemProtocol(gson.fromJson(protocol.getProtocolData(), SystemProtocol.class),user);
            case "LoginProtocol" -> HandleLoginProtocol(gson.fromJson(protocol.getProtocolData(),LoginProtocol.class),user);
            case "ChatProtocol" -> HandleChatProtocol(gson.fromJson(protocol.getProtocolData(),ChatProtocol.class),user);
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
    public CustomLogger getLogger() {
        return logger;
    }
}
