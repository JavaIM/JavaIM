package org.yuezhikong.Server;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.session.SqlSession;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Range;
import org.jline.reader.EndOfFileException;
import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.reader.UserInterruptException;
import org.jline.terminal.Terminal;
import org.slf4j.LoggerFactory;
import org.yuezhikong.CodeDynamicConfig;
import org.yuezhikong.Main;
import org.yuezhikong.Server.network.ExitWatchdog;
import org.yuezhikong.Server.protocolHandler.ProtocolHandler;
import org.yuezhikong.Server.protocolHandler.handlers.*;
import org.yuezhikong.Server.userData.auth.UserAuthentication;
import org.yuezhikong.Server.userData.*;
import org.yuezhikong.Server.userData.users.ConsoleUser;
import org.yuezhikong.Server.userData.users.NetworkUser;
import org.yuezhikong.Server.api.SingleAPI;
import org.yuezhikong.Server.api.api;
import org.yuezhikong.Server.network.NetworkServer;
import org.yuezhikong.Server.plugin.PluginManager;
import org.yuezhikong.Server.plugin.SimplePluginManager;
import org.yuezhikong.Server.plugin.event.events.Server.ServerChatEvent;
import org.yuezhikong.Server.plugin.event.events.Server.ServerCommandEvent;
import org.yuezhikong.Server.plugin.event.events.Server.ServerStartSuccessfulEvent;
import org.yuezhikong.Server.plugin.event.events.Server.ServerStopEvent;
import org.yuezhikong.Server.plugin.event.events.User.UserAddEvent;
import org.yuezhikong.Server.plugin.event.events.User.UserRemoveEvent;
import org.yuezhikong.Server.plugin.userData.PluginUser;
import org.yuezhikong.Server.request.ChatRequest;
import org.yuezhikong.Server.request.ChatRequestImpl;
import org.yuezhikong.utils.Protocol.*;
import org.yuezhikong.utils.checks;
import org.yuezhikong.utils.database.DatabaseHelper;
import org.yuezhikong.utils.logging.CustomLogger;
import org.yuezhikong.utils.logging.PluginLoggingBridge;

import java.io.File;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * JavaIM服务端
 */
@Slf4j
public final class Server implements IServer {

    /**
     * 用户列表
     */
    private final List<user> users = new CopyOnWriteArrayList<>();

    /**
     * 协议处理器列表
     */
    private final Map<String, ProtocolHandler> protocolHandlerBind = new ConcurrentHashMap<>();

    /**
     * IO线程池
     */
    private final ExecutorService IOThreadPool = Executors.newCachedThreadPool(new ThreadFactory() {
        private final AtomicInteger threadNumber = new AtomicInteger(1);

        @Override
        public Thread newThread(@NotNull Runnable r) {
            return new Thread(getServerThreadGroup(),
                    r, "IO Thread #" + threadNumber.getAndIncrement());
        }
    });

    @Getter
    private final ThreadGroup serverThreadGroup = Thread.currentThread().getThreadGroup();
    /**
     * 用户处理器
     */
    private ChatRequestImpl request;

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
    @Getter
    private final Gson gson = new Gson();

    /**
     * 服务器API
     */
    private api serverAPI;
    /**
     * 网络层服务器列表
     */
    private final List<NetworkServer> networkServerList = new ArrayList<>();

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
    public boolean isServerCompleteStart() {
        return startSuccessful;
    }

    @Override
    public void registerNetworkServer(NetworkServer server) {
        networkServerList.add(server);
    }

    @Override
    public void unregisterNetworkServer(NetworkServer server) {
        networkServerList.remove(server);
    }

    private boolean beginRun = false;
    @Getter
    private boolean stopped = false;

    /**
     * 启动JavaIM服务端
     * @param serverPort    服务器端口
     * @param initServer    网络层服务器
     */
    public void start(
            @Range(from = 1, to = 65535) int serverPort,
            NetworkServer initServer
    ) {
        checks.checkArgument(initServer == null, "Network Server can not be null!");
        checks.checkState(beginRun,"JavaIM Server is already running.");
        beginRun = true;
        registerNetworkServer(initServer);

        long startUnix = System.currentTimeMillis();
        log.info("正在启动JavaIM");
        if (Instance != null)
            throw new RuntimeException("JavaIM Server is already running!");
        Instance = this;
        PluginLoggingBridge.reset();

        ExecutorService StartUpThreadPool = Executors.newCachedThreadPool(new ThreadFactory() {
            private final AtomicInteger threadNumber = new AtomicInteger(0);

            @Override
            public Thread newThread(@NotNull Runnable r) {
                return new Thread(r, "StartUp Thread #" + threadNumber.getAndIncrement());
            }
        });

        log.info("正在预加载插件");
        getPluginManager().preloadPluginOnDirectory(new File("./plugins"), StartUpThreadPool);
        log.info("插件预加载完成");

        serverAPI = new SingleAPI(this) {
            @Override
            public void sendJsonToClient(@NotNull user User, @NotNull String InputData, @NotNull String ProtocolType) {
                GeneralProtocol protocol = new GeneralProtocol();
                protocol.setProtocolVersion(CodeDynamicConfig.getProtocolVersion());
                protocol.setProtocolName(ProtocolType);
                protocol.setProtocolData(InputData);

                String SendData = gson.toJson(protocol);
                if (User instanceof PluginUser)
                    //如果是插件用户，则直接调用插件用户中的方法
                    ((PluginUser) User).writeData(SendData);
                else if (User instanceof ConsoleUser)
                    log.info(SendData);
                else if (User instanceof NetworkUser)
                    ((NetworkUser) User).getNetworkClient().send(SendData);
            }
        };// 初始化Server API
        request = new ChatRequestImpl();

        new Thread(() -> {
            Future<?> DatabaseStartTask = StartUpThreadPool.submit(() -> {
                log.info("正在处理数据库");
                String JDBCUrl;
                try {
                    //获取JDBCUrl,创建表与自动更新数据库
                    JDBCUrl = DatabaseHelper.InitDataBase();
                } catch (Throwable throwable) {
                    log.error("数据库启动失败", throwable);
                    if (!initServer.isRunning()) {
                        synchronized (NetworkServer.class) {
                            if (!initServer.isRunning()) {
                                try {
                                    NetworkServer.class.wait();
                                } catch (InterruptedException ignored) {

                                }
                            }
                        }
                    }
                    StartUpThreadPool.shutdownNow();
                    log.error("JavaIM启动失败，因为数据库出错");
                    try {
                        stop();
                    } catch (NullPointerException ignored) {
                    }
                    log.info("JavaIM服务器已经关闭");
                    return;
                }
                //初始化Mybatis
                sqlSession = DatabaseHelper.InitMybatis(JDBCUrl);
                log.info("数据库启动完成");
            });

            Future<?> PluginLoadTask = StartUpThreadPool.submit(() -> {
                log.info("正在加载插件");
                getPluginManager().loadPluginOnDirectory(new File("./plugins"), StartUpThreadPool);
                log.info("插件加载完成");
            });

            Future<?> HandlerInitTask = StartUpThreadPool.submit(() -> {
                protocolHandlerBind.put("ChatProtocol", new ChatProtocolHandler());
                protocolHandlerBind.put("LoginProtocol", new LoginProtocolHandler());
                protocolHandlerBind.put("SystemProtocol", new SystemProtocolHandler());
                protocolHandlerBind.put("TransferProtocol", new TransferProtocolHandler());
            });

            Thread UserCommandRequestThread = new Thread(() -> {
                Terminal terminal = Main.getTerminal();
                LineReader reader = LineReaderBuilder.builder().completer(request.getCompleter()).terminal(terminal).build();
                while (true) {
                    try {
                        String line = reader.readLine(">").trim();
                        if (line.isEmpty())
                            continue;
                        if (!line.startsWith("/")) {
                            //通知发生事件
                            ServerChatEvent chatEvent = new ServerChatEvent(consoleUser, line);
                            getPluginManager().callEvent(chatEvent);
                            //判断是否被取消
                            if (chatEvent.isCancelled())
                                continue;
                            //格式化&发送
                            ((CustomLogger) log).chatMsg("[Server]:" + line);
                            ChatProtocol chatProtocol = new ChatProtocol();
                            chatProtocol.setSourceUserName("Server");
                            chatProtocol.setMessage(line);
                            String SendProtocolData = gson.toJson(chatProtocol);
                            serverAPI.getValidUserList(true).forEach((user) ->
                                    serverAPI.sendJsonToClient(user, SendProtocolData, "ChatProtocol"));
                            continue;
                        }
                        List<String> tmp = reader.getParsedLine().words();
                        String command = tmp.get(0).substring(1);
                        String[] args;
                        if (!tmp.get(tmp.size() - 1).isEmpty()) {
                            args = new String[tmp.size() - 1];
                            System.arraycopy(tmp.toArray(new String[0]), 1, args, 0, tmp.size() - 1);
                        } else {
                            args = new String[tmp.size() - 2];
                            System.arraycopy(tmp.toArray(new String[0]), 1, args, 0, tmp.size() - 2);
                        }

                        //通知发生事件
                        ServerCommandEvent commandEvent = new ServerCommandEvent(getConsoleUser(), command, args);
                        pluginManager.callEvent(commandEvent);
                        if (commandEvent.isCancelled())
                            return;
                        getRequest().commandRequest(command, args, getConsoleUser());
                    } catch (Throwable throwable) {
                        if (throwable instanceof UserInterruptException) {
                            log.info("Ctrl+C已被按下...");
                            log.info("正在关闭JavaIM");
                            stop();
                            return;
                        }
                        if (throwable instanceof EndOfFileException) {
                            continue;
                        }
                        log.error("出现错误!", throwable);
                    }
                }
            }, "User Command Thread");
            log.info("正在等待网络层启动完成");
            if (!initServer.isRunning()) {
                synchronized (NetworkServer.class) {
                    if (!initServer.isRunning()) {
                        try {
                            NetworkServer.class.wait();
                        } catch (InterruptedException ignored) {

                        }
                    }
                }
            }

            try {
                log.info("正在等待协议handler初始化完成");
                HandlerInitTask.get();
                log.info("正在等待插件启动完成");
                PluginLoadTask.get();
                log.info("正在等待数据库启动完成");
                DatabaseStartTask.get();
            } catch (InterruptedException | ExecutionException e) {
                throw new RuntimeException("Thread Pool Fatal", e);
            }

            log.info("正在启动用户指令处理线程");
            UserCommandRequestThread.start();
            log.info("用户指令处理线程启动完成");

            log.info("JavaIM 启动完成 (耗时:{}ms)", System.currentTimeMillis() - startUnix);
            startSuccessful = true;
            StartUpThreadPool.shutdownNow();
            getPluginManager().callEvent(new ServerStartSuccessfulEvent());
        }, "Server Thread").start();

        initServer.start(serverPort, IOThreadPool, StartUpThreadPool);
    }

    @Getter
    private static Server Instance;

    @Override
    public NetworkServer[] getNetworkServers() {
        return networkServerList.toArray(new NetworkServer[0]);
    }

    @Override
    public void stop() {
        log.info("JavaIM服务器正在关闭...");
        stopped = true;
        getServerAPI().sendMessageToAllClient("服务器已关闭");
        for (user requestUser : getServerAPI().getValidUserList(false)) {
            requestUser.disconnect();
        }
        users.clear();
        pluginManager.callEvent(new ServerStopEvent());
        try {
            pluginManager.unloadAllPlugin();
        } catch (Throwable ignored) {
        }
        System.gc();
        for (NetworkServer networkServer : networkServerList) {
            networkServer.stop();
        }
        Instance = null;
        sqlSession.close();
        try {
            ExitWatchdog.getInstance().onJavaIMExit();
        } catch (IllegalStateException ignored) {}
        log.info("JavaIM服务器已关闭");
    }

    @Override
    public void onReceiveMessage(user user, String message) {
        if (user.getUserAuthentication() == null)
            user.setUserAuthentication(new UserAuthentication(user, this));
        GeneralProtocol protocol;
        try {
            protocol = gson.fromJson(message, GeneralProtocol.class);
        } catch (JsonSyntaxException e) {
            SystemProtocol systemProtocol = new SystemProtocol();
            systemProtocol.setType("Error");
            systemProtocol.setMessage("Protocol analysis failed");
            serverAPI.sendJsonToClient(user, gson.toJson(systemProtocol), "SystemProtocol");
            return;
        }
        if (protocol.getProtocolVersion() != CodeDynamicConfig.getProtocolVersion()) {
            SystemProtocol systemProtocol = new SystemProtocol();
            systemProtocol.setType("Error");
            systemProtocol.setMessage("Protocol version not support");
            serverAPI.sendJsonToClient(user, gson.toJson(systemProtocol), "SystemProtocol");
            return;
        }
        ProtocolHandler handler = protocolHandlerBind.get(protocol.getProtocolName());
        if (handler == null) {
            getServerAPI().sendMessageToUser(user, "暂不支持此协议");
            log.warn("客户端发送了未知协议:{}", protocol.getProtocolName());
            return;
        }
        handler.handleProtocol(this, protocol.getProtocolData(), user);
    }

    @Override
    public ExecutorService getIOThreadPool() {
        return IOThreadPool;
    }

    @Override
    public List<user> getUsers() {
        return Collections.unmodifiableList(users);
    }

    @Override
    public boolean registerUser(user User) {
        UserAddEvent addEvent = new UserAddEvent(User);
        getPluginManager().callEvent(addEvent);
        if (addEvent.isCancelled())
            return false;
        for (user ForEachUser : users) {
            if (ForEachUser.getUserName().equals(User.getUserName()))
                return false;
        }
        return users.add(User);
    }

    /**
     * 取消注册一个用户
     *
     * @param User 用户
     */
    @Override
    public void unRegisterUser(user User) {
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

    @SuppressWarnings("removal")
    @Override
    public CustomLogger getLogger() {
        try {
            return (CustomLogger) LoggerFactory.getLogger(Class.forName(new Throwable().getStackTrace()[1].getClassName()));
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("Compatibility failed",e);
        }
    }
}
