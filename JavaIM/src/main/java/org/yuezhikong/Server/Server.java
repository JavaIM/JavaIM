package org.yuezhikong.Server;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import org.apache.commons.io.FileUtils;
import org.apache.ibatis.session.SqlSession;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Range;
import org.slf4j.LoggerFactory;
import org.yuezhikong.CodeDynamicConfig;
import org.yuezhikong.Main;
import org.yuezhikong.Server.UserData.*;
import org.yuezhikong.Server.UserData.Authentication.UserAuthentication;
import org.yuezhikong.Server.UserData.tcpUser.tcpUser;
import org.yuezhikong.Server.api.SingleAPI;
import org.yuezhikong.Server.api.api;
import org.yuezhikong.Server.network.NetworkServer;
import org.yuezhikong.Server.network.SSLNettyServer;
import org.yuezhikong.Server.plugin.PluginManager;
import org.yuezhikong.Server.plugin.SimplePluginManager;
import org.yuezhikong.Server.plugin.event.events.Server.ServerChatEvent;
import org.yuezhikong.Server.plugin.event.events.Server.ServerCommandEvent;
import org.yuezhikong.Server.plugin.event.events.Server.ServerStopEvent;
import org.yuezhikong.Server.plugin.event.events.Server.ServerStartSuccessfulEvent;
import org.yuezhikong.Server.plugin.event.events.User.UserAddEvent;
import org.yuezhikong.Server.plugin.event.events.User.UserRemoveEvent;
import org.yuezhikong.Server.plugin.userData.PluginUser;
import org.yuezhikong.utils.Protocol.*;
import org.yuezhikong.utils.database.dao.userInformationDao;
import org.yuezhikong.utils.database.dao.userUploadFileDao;
import org.yuezhikong.utils.logging.CustomLogger;
import org.yuezhikong.utils.CustomVar;
import org.yuezhikong.utils.database.DatabaseHelper;
import org.yuezhikong.utils.SaveStackTrace;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.util.*;
import java.util.concurrent.*;
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
    private final List<user> users = new CopyOnWriteArrayList<>();

    /**
     * 用户处理器
     */
    private ChatRequest request;

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
    private api serverAPI;

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
    public void start(@Range(from = 1, to = 65535) int ListenPort) {
        long startUnix = System.currentTimeMillis();
        LoggerFactory.getLogger(Main.class).info("正在启动JavaIM");
        if (Instance != null)
            throw new RuntimeException("JavaIM Server is already running!");
        Instance = this;

        ExecutorService StartUpThreadPool = Executors.newCachedThreadPool(new ThreadFactory() {
            private final AtomicInteger threadNumber = new AtomicInteger(0);
            @Override
            public Thread newThread(@NotNull Runnable r) {
                return new Thread(r,"StartUp Thread #"+threadNumber.getAndIncrement());
            }
        });

        LoggerFactory.getLogger(Main.class).info("正在预加载插件");
        getPluginManager().PreloadPluginOnDirectory(new File("./plugins"), StartUpThreadPool);
        LoggerFactory.getLogger(Main.class).info("插件预加载完成");

        logger = (CustomLogger) LoggerFactory.getLogger(Server.class);//初始化日志
        serverAPI = new SingleAPI(this) {
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
        };// 初始化Server API
        request = new ChatRequest(this);

        networkServer = new SSLNettyServer();
        new Thread(() -> {
            Future<?> DatabaseStartTask = StartUpThreadPool.submit(() -> {
                logger.info("正在处理数据库");
                String JDBCUrl;
                try {
                    //获取JDBCUrl,创建表与自动更新数据库
                    JDBCUrl = DatabaseHelper.InitDataBase();
                } catch (Throwable throwable)
                {
                    logger.error("数据库启动失败",throwable);
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
                    StartUpThreadPool.shutdownNow();
                    logger.error("JavaIM启动失败，因为数据库出错");
                    try {
                        stop();
                    } catch (NullPointerException ignored) {}
                    logger.info("JavaIM服务器已经关闭");
                    return;
                }
                //初始化Mybatis
                sqlSession = DatabaseHelper.InitMybatis(JDBCUrl);
                logger.info("数据库启动完成");
            });

            Future<?> PluginLoadTask = StartUpThreadPool.submit(() -> {
                logger.info("正在加载插件");
                getPluginManager().LoadPluginOnDirectory(new File("./plugins"), StartUpThreadPool);
                logger.info("插件加载完成");
            });

            Thread UserCommandRequestThread = new Thread(() -> {
                while (true) try {
                    Scanner scanner = new Scanner(System.in);
                    String consoleInput = scanner.nextLine();
                    if (consoleInput.isEmpty())
                        continue;
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
                        serverAPI.GetValidClientList(true).forEach((user) ->
                                serverAPI.SendJsonToClient(user,SendProtocolData,"ChatProtocol"));
                    }
                } catch (Throwable throwable) {
                    logger.error("JavaIM User Command Thread 出现异常");
                    logger.error("请联系开发者");
                    SaveStackTrace.saveStackTrace(throwable);
                }
            },"User Command Thread");

            logger.info("正在等待网络层启动完成");
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

            try {
                logger.info("正在等待插件启动完成");
                PluginLoadTask.get();
                logger.info("正在等待数据库启动完成");
                DatabaseStartTask.get();
            } catch (InterruptedException | ExecutionException e) {
                throw new RuntimeException("Thread Pool Fatal",e);
            }

            logger.info("正在启动用户指令处理线程");
            UserCommandRequestThread.start();
            logger.info("用户指令处理线程启动完成");

            logger.info("JavaIM 启动完成 (耗时:{}ms)",System.currentTimeMillis() - startUnix);
            startSuccessful = true;
            StartUpThreadPool.shutdownNow();
            getPluginManager().callEvent(new ServerStartSuccessfulEvent());
        }, "Server Thread").start();

        Thread.currentThread().setName("Network Thread");
        networkServer.start(ListenPort, StartUpThreadPool);
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
        } catch (Throwable ignored) {}
        userMessageRequestThreadPool.shutdownNow();
        System.gc();
        getNetwork().stop();
        Instance = null;
        sqlSession.close();
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
            logger.ChatMsg("["+user.getUserName()+"]:"+input.getChatMessage());

            ChatProtocol chatProtocol = new ChatProtocol();
            chatProtocol.setSourceUserName(user.getUserName());
            chatProtocol.setMessage(input.getChatMessage());
            String SendProtocolData = gson.toJson(chatProtocol);
            serverAPI.GetValidClientList(true).forEach((forEachUser) ->
                    serverAPI.SendJsonToClient(forEachUser,SendProtocolData,"ChatProtocol"));
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
        class utils {

            /**
             * 发送文件
             * @param fileId    文件Id
             * @param user      用户
             * @param serverApi 服务端API
             * @param gson      Gson
             * @param fileName  文件名
             */
            public static void sendFile(String fileId, tcpUser user, api serverApi, Gson gson, String fileName) {
                File file = new File("./uploadFiles",fileId);
                String content;
                try {
                    content = Base64.getEncoder().encodeToString(FileUtils.readFileToByteArray(file));
                } catch (IOException e) {
                    SystemProtocol systemProtocol = new SystemProtocol();
                    systemProtocol.setType("Error");
                    systemProtocol.setMessage("File Not Found");
                    serverApi.SendJsonToClient(user,gson.toJson(systemProtocol),"SystemProtocol");
                    return;
                }

                TransferProtocol transferProtocol = new TransferProtocol();
                transferProtocol.setTransferProtocolHead(new TransferProtocol.TransferProtocolHeadBean());
                transferProtocol.getTransferProtocolHead().setTargetUserName("");
                transferProtocol.getTransferProtocolHead().setType("download");
                transferProtocol.setTransferProtocolBody(new ArrayList<>());

                TransferProtocol.TransferProtocolBodyBean fileNameBean = new TransferProtocol.TransferProtocolBodyBean();
                fileNameBean.setData(fileName);
                TransferProtocol.TransferProtocolBodyBean fileContentBean = new TransferProtocol.TransferProtocolBodyBean();
                fileContentBean.setData(content);

                transferProtocol.getTransferProtocolBody().add(fileNameBean);
                transferProtocol.getTransferProtocolBody().add(fileContentBean);

                serverApi.SendJsonToClient(user,gson.toJson(transferProtocol),"TransferProtocol");
            }

            /**
             * 根据用户名获取文件
             *
             * @param sqlSession SQL会话
             * @param user       用户
             */
            public static List<userUploadFile> getFileByUserId(SqlSession sqlSession, tcpUser user) {
                userUploadFileDao mapper = sqlSession.getMapper(userUploadFileDao.class);
                return mapper.getUploadFilesByUserId(user.getUserInformation().getUserId());
            }
        }
        switch (protocol.getType()){
            case "ChangePassword" -> {
                if (!user.isUserLogged()) {
                    getServerAPI().SendMessageToUser(user,"请先登录");
                    return;
                }
                getServerAPI().ChangeUserPassword(user, protocol.getMessage());
            }
            case "DownloadOwnFileByFileName" -> {
                if (!user.isUserLogged()) {
                    getServerAPI().SendMessageToUser(user,"请先登录");
                    return;
                }
                List<userUploadFile> uploadFiles = utils.getFileByUserId(sqlSession,user);
                if (uploadFiles == null) {
                    SystemProtocol systemProtocol = new SystemProtocol();
                    systemProtocol.setType("Error");
                    systemProtocol.setMessage("File Not Found");
                    serverAPI.SendJsonToClient(user,gson.toJson(systemProtocol),"SystemProtocol");
                    return;
                }

                String FileId = null;
                for (userUploadFile uploadFile : uploadFiles) {
                    if (uploadFile.getOrigFileName().equals(protocol.getMessage())) {
                        FileId = uploadFile.getOwnFile();
                        break;
                    }
                }
                if (FileId == null) {
                    SystemProtocol systemProtocol = new SystemProtocol();
                    systemProtocol.setType("Error");
                    systemProtocol.setMessage("File Not Found");
                    serverAPI.SendJsonToClient(user,gson.toJson(systemProtocol),"SystemProtocol");
                    return;
                }

                utils.sendFile(FileId,user,serverAPI,gson,protocol.getMessage());
            }
            case "DownloadFileByFileId" -> {
                if (!user.isUserLogged()) {
                    getServerAPI().SendMessageToUser(user,"请先登录");
                    return;
                }
                userUploadFileDao mapper = sqlSession.getMapper(userUploadFileDao.class);
                userUploadFile uploadFile = mapper.getUploadFileByFileId(protocol.getMessage());
                if (uploadFile == null) {
                    SystemProtocol systemProtocol = new SystemProtocol();
                    systemProtocol.setType("Error");
                    systemProtocol.setMessage("File Not Found");
                    serverAPI.SendJsonToClient(user,gson.toJson(systemProtocol),"SystemProtocol");
                    return;
                }
                utils.sendFile(uploadFile.getOwnFile(),user,serverAPI,gson,uploadFile.getOrigFileName());
            }
            case "DeleteUploadFileByFileId" -> {
                if (!user.isUserLogged()) {
                    getServerAPI().SendMessageToUser(user,"请先登录");
                    return;
                }
                userUploadFileDao mapper = sqlSession.getMapper(userUploadFileDao.class);
                userUploadFile uploadFile = mapper.getUploadFileByFileId(protocol.getMessage());
                if (uploadFile == null) {
                    SystemProtocol systemProtocol = new SystemProtocol();
                    systemProtocol.setType("Error");
                    systemProtocol.setMessage("File Not Found");
                    serverAPI.SendJsonToClient(user,gson.toJson(systemProtocol),"SystemProtocol");
                    return;
                }

                // 如果操作者不是文件拥有者，且操作者不是管理员，则禁止操作
                if (!user.getUserInformation().getUserId().equals(uploadFile.getUserId()) && !Permission.ADMIN.equals(user.getUserPermission())) {
                    SystemProtocol systemProtocol = new SystemProtocol();
                    systemProtocol.setType("Error");
                    systemProtocol.setMessage("Permission denied");
                    serverAPI.SendJsonToClient(user,gson.toJson(systemProtocol),"SystemProtocol");
                    return;
                }

                File file = new File("./uploadFiles",uploadFile.getOwnFile());
                if (!file.delete()) {
                    SystemProtocol systemProtocol = new SystemProtocol();
                    systemProtocol.setType("Error");
                    systemProtocol.setMessage("Permission denied by platform");
                    serverAPI.SendJsonToClient(user,gson.toJson(systemProtocol),"SystemProtocol");
                    return;
                }

                if (!mapper.deleteFile(uploadFile)) {
                    SystemProtocol systemProtocol = new SystemProtocol();
                    systemProtocol.setType("Error");
                    systemProtocol.setMessage("Permission denied by platform");
                    serverAPI.SendJsonToClient(user,gson.toJson(systemProtocol),"SystemProtocol");
                    return;
                }
                serverAPI.SendMessageToUser(user, "操作成功完成。");
            }
            case "GetFileIdByFileName" -> {
                if (!user.isUserLogged()) {
                    getServerAPI().SendMessageToUser(user,"请先登录");
                    return;
                }
                userUploadFileDao mapper = sqlSession.getMapper(userUploadFileDao.class);
                List<userUploadFile> uploadFiles = mapper.getUploadFilesByUserId(user.getUserInformation().getUserId());
                if (uploadFiles == null) {
                    SystemProtocol systemProtocol = new SystemProtocol();
                    systemProtocol.setType("Error");
                    systemProtocol.setMessage("File Not Found");
                    serverAPI.SendJsonToClient(user,gson.toJson(systemProtocol),"SystemProtocol");
                    return;
                }
                userUploadFile uploadFile = null;
                for (userUploadFile userUploadFile : uploadFiles) {
                    if (userUploadFile.getOrigFileName().equals(protocol.getMessage())) {
                        uploadFile = userUploadFile;
                        break;
                    }
                }
                if (uploadFile == null) {
                    SystemProtocol systemProtocol = new SystemProtocol();
                    systemProtocol.setType("Error");
                    systemProtocol.setMessage("File Not Found");
                    serverAPI.SendJsonToClient(user,gson.toJson(systemProtocol),"SystemProtocol");
                    return;
                }

                String FileId = uploadFile.getOwnFile();
                SystemProtocol systemProtocol = new SystemProtocol();
                systemProtocol.setType("GetFileIdByFileNameResult");
                systemProtocol.setMessage(FileId);
                serverAPI.SendJsonToClient(user,gson.toJson(systemProtocol),"SystemProtocol");
            }
            case "GetFileNameByFileId" -> {
                if (!user.isUserLogged()) {
                    getServerAPI().SendMessageToUser(user,"请先登录");
                    return;
                }
                userUploadFileDao mapper = sqlSession.getMapper(userUploadFileDao.class);
                userUploadFile uploadFile = mapper.getUploadFileByFileId(protocol.getMessage());
                if (uploadFile == null) {
                    SystemProtocol systemProtocol = new SystemProtocol();
                    systemProtocol.setType("Error");
                    systemProtocol.setMessage("File Not Found");
                    serverAPI.SendJsonToClient(user,gson.toJson(systemProtocol),"SystemProtocol");
                    return;
                }
                String FileName = uploadFile.getOrigFileName();
                SystemProtocol systemProtocol = new SystemProtocol();
                systemProtocol.setType("GetFileNameByFileIdResult");
                systemProtocol.setMessage(FileName);
                serverAPI.SendJsonToClient(user,gson.toJson(systemProtocol),"SystemProtocol");
            }
            case "GetUploadFileList" -> {
                if (!user.isUserLogged()) {
                    getServerAPI().SendMessageToUser(user,"请先登录");
                    return;
                }
                List<userUploadFile> uploadFiles = utils.getFileByUserId(sqlSession,user);
                if (uploadFiles == null) {
                    SystemProtocol systemProtocol = new SystemProtocol();
                    systemProtocol.setType("Error");
                    systemProtocol.setMessage("File Not Found");
                    serverAPI.SendJsonToClient(user,gson.toJson(systemProtocol),"SystemProtocol");
                    return;
                }

                TransferProtocol transferProtocol = new TransferProtocol();
                transferProtocol.setTransferProtocolHead(new TransferProtocol.TransferProtocolHeadBean());
                transferProtocol.getTransferProtocolHead().setTargetUserName("");
                transferProtocol.getTransferProtocolHead().setType("fileList");
                transferProtocol.setTransferProtocolBody(new ArrayList<>());

                uploadFiles.forEach((file -> {
                    TransferProtocol.TransferProtocolBodyBean bodyBean = new TransferProtocol.TransferProtocolBodyBean();
                    bodyBean.setData(file.getOrigFileName());
                    transferProtocol.getTransferProtocolBody().add(bodyBean);
                }));
                serverAPI.SendJsonToClient(user,gson.toJson(transferProtocol),"TransferProtocol");
            }
            case "SetAvatarId" -> {
                if (!user.isUserLogged()) {
                    getServerAPI().SendMessageToUser(user,"请先登录");
                    return;
                }
                // 检测是否存在
                userUploadFileDao mapper = sqlSession.getMapper(userUploadFileDao.class);
                userUploadFile uploadFile = mapper.getUploadFileByFileId(protocol.getMessage());
                if (uploadFile == null) {
                    SystemProtocol systemProtocol = new SystemProtocol();
                    systemProtocol.setType("Error");
                    systemProtocol.setMessage("File Not Found");
                    serverAPI.SendJsonToClient(user,gson.toJson(systemProtocol),"SystemProtocol");
                    return;
                }
                // 如果存在，设置
                user.getUserInformation().setAvatar(protocol.getMessage());
            }
            case "GetAvatarIdByUserName" -> {
                if (!user.isUserLogged()) {
                    getServerAPI().SendMessageToUser(user,"请先登录");
                    return;
                }
                userInformationDao informationDao = sqlSession.getMapper(userInformationDao.class);
                userInformation information = informationDao.getUser(null,protocol.getMessage(),null,null);
                if (information == null) {
                    SystemProtocol systemProtocol = new SystemProtocol();
                    systemProtocol.setType("Error");
                    systemProtocol.setMessage("User Not Found");
                    serverAPI.SendJsonToClient(user,gson.toJson(systemProtocol),"SystemProtocol");
                    return;
                }

                SystemProtocol systemProtocol = new SystemProtocol();
                systemProtocol.setType("GetAvatarIdByUserNameResult");
                systemProtocol.setMessage(information.getAvatar());
                serverAPI.SendJsonToClient(user,gson.toJson(systemProtocol),"SystemProtocol");
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
     * 处理转发协议
     *
     * @param protocol  协议
     * @param user      用户
     */
    private void HandleTransferProtocol(final TransferProtocol protocol, final tcpUser user) {
        if (!user.isUserLogged()) {
            getServerAPI().SendMessageToUser(user, "请先登录");
            return;
        }
        if (protocol.getTransferProtocolHead() == null || protocol.getTransferProtocolBody() == null) {
            SystemProtocol systemProtocol = new SystemProtocol();
            systemProtocol.setType("Error");
            systemProtocol.setMessage("Invalid Packet");
            serverAPI.SendJsonToClient(user,gson.toJson(systemProtocol),"SystemProtocol");
            return;
        }
        if (!"upload".equals(protocol.getTransferProtocolHead().getType())) {
            serverAPI.SendMessageToUser(user,"正在开发中");
            return;
        }

        List<TransferProtocol.TransferProtocolBodyBean> data = protocol.getTransferProtocolBody();
        if (data.size() != 2 || data.get(0).getData() == null || data.get(1).getData() == null) {
            SystemProtocol systemProtocol = new SystemProtocol();
            systemProtocol.setType("Error");
            systemProtocol.setMessage("Invalid Packet");
            serverAPI.SendJsonToClient(user,gson.toJson(systemProtocol),"SystemProtocol");
            return;
        }
        String fileName = data.get(0).getData();

        userUploadFileDao uploadFileDao = sqlSession.getMapper(userUploadFileDao.class);
        List<userUploadFile> uploadFiles = uploadFileDao.getUploadFilesByUserId(user.getUserInformation().getUserId());
        if (uploadFiles != null)
            for (userUploadFile uploadFile : uploadFiles) {
                if (uploadFile.getOrigFileName().equals(fileName)) {
                    SystemProtocol systemProtocol = new SystemProtocol();
                    systemProtocol.setType("Error");
                    systemProtocol.setMessage("File Already Exists");
                    serverAPI.SendJsonToClient(user,gson.toJson(systemProtocol),"SystemProtocol");
                    return;
                }
            }

        byte[] fileContent;
        try {
            fileContent = Base64.getDecoder().decode(data.get(1).getData().getBytes(StandardCharsets.UTF_8));
        } catch (IllegalArgumentException e) {
            SystemProtocol systemProtocol = new SystemProtocol();
            systemProtocol.setType("Error");
            systemProtocol.setMessage("Invalid Packet");
            serverAPI.SendJsonToClient(user,gson.toJson(systemProtocol),"SystemProtocol");
            return;
        }

        String fileId;
        do {
            fileId = UUID.randomUUID().toString();
            userUploadFile uploadFile = uploadFileDao.getUploadFileByFileId(fileId);
            if (uploadFile == null) {
                break;
            }
        } while (true);
        uploadFileDao.addFile(new userUploadFile(user.getUserInformation().getUserId(),fileId,fileName));

        File uploadFileDirectory = new File("./uploadFiles");
        File uploadFile = new File(uploadFileDirectory,fileId);
        try {
            try {
                Files.createDirectory(uploadFileDirectory.toPath());
            } catch (FileAlreadyExistsException ignored) {}
            Files.createFile(uploadFile.toPath());

            FileUtils.writeByteArrayToFile(uploadFile,fileContent);
        } catch (IOException e) {
            throw new RuntimeException("write File Failed",e);
        }
        serverAPI.SendMessageToUser(user, "操作成功完成。");
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
        if (loginProtocol.getLoginPacketHead() == null || loginProtocol.getLoginPacketBody() == null) {
            SystemProtocol systemProtocol = new SystemProtocol();
            systemProtocol.setType("Error");
            systemProtocol.setMessage("Invalid Packet");
            serverAPI.SendJsonToClient(user,gson.toJson(systemProtocol),"SystemProtocol");
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
        if (protocol.getProtocolVersion() != CodeDynamicConfig.getProtocolVersion()) {
            SystemProtocol systemProtocol = new SystemProtocol();
            systemProtocol.setType("Error");
            systemProtocol.setMessage("Protocol version not support");
            serverAPI.SendJsonToClient(user, gson.toJson(systemProtocol), "SystemProtocol");
            return;
        }

        switch (protocol.getProtocolName())
        {
            case "SystemProtocol" -> HandleSystemProtocol(gson.fromJson(protocol.getProtocolData(), SystemProtocol.class),user);
            case "LoginProtocol" -> HandleLoginProtocol(gson.fromJson(protocol.getProtocolData(),LoginProtocol.class),user);
            case "ChatProtocol" -> HandleChatProtocol(gson.fromJson(protocol.getProtocolData(),ChatProtocol.class),user);
            case "TransferProtocol" -> HandleTransferProtocol(gson.fromJson(protocol.getProtocolData(), TransferProtocol.class),user);
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

    @SuppressWarnings("removal")
    @Override
    public CustomLogger getLogger() {
        return logger;
    }
}
