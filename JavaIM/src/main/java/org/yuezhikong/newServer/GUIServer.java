package org.yuezhikong.newServer;

import org.jetbrains.annotations.NotNull;
import org.yuezhikong.CrashReport;
import org.yuezhikong.GraphicalUserInterface.DefaultController;
import org.yuezhikong.GraphicalUserInterface.ServerUI;
import org.yuezhikong.newServer.UserData.user;
import org.yuezhikong.utils.Logger;
import org.yuezhikong.utils.SaveStackTrace;

import java.io.IOException;
import java.util.Scanner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

public class GUIServer extends ServerMain{
    private final ServerUI GUIController;
    private final ThreadGroup ServerThreadGroup = new ThreadGroup(Thread.currentThread().getThreadGroup(), "ServerGroup");
    private final ExecutorService UserRequestThreadPool;
    @Override
    protected Logger initLogger() {
        return new Logger(GUIController);
    }
    public GUIServer(ServerUI controller)
    {
        GUIController = controller;
        UserRequestThreadPool = Executors.newSingleThreadExecutor(new ThreadFactory() {
            private final AtomicInteger threadNumber = new AtomicInteger(1);
            @Override
            public Thread newThread(@NotNull Runnable r) {
                return new Thread(new ThreadGroup(ServerThreadGroup, "UserRequestDispose ThreadGroup"),
                        r,"UserRequestDispose Thread #"+threadNumber.getAndIncrement());
            }
        });
    }

    public void ServerChatMessageSend(String ChatMessage)
    {
        UserRequestThreadPool.execute(() -> {
            ChatRequest.ChatRequestInput input = new ChatRequest.ChatRequestInput(getConsoleUser(),ChatMessage);
            getRequest().ChatFormat(input);
            getServerAPI().SendMessageToAllClient("[服务端消息] "+input.getChatMessage(),getServer());
            getLogger().ChatMsg("[服务端消息] "+input.getChatMessage());
        });
    }

    @Override
    protected void ServerCleanup() {
        super.ServerCleanup();
        if (!(UserRequestThreadPool.isShutdown()))
        {
            UserRequestThreadPool.shutdownNow();
        }
        GUIController.onServerShutdown();
    }

    @Override
    public void start(int bindPort) {
        Thread ServerThread = new Thread(ServerThreadGroup,() -> super.start(bindPort),"Server Thread");
        ServerThread.setUncaughtExceptionHandler(CrashReport.getCrashReport());
        ServerThread.start();
    }

    @Override
    protected void StartCommandSystem() {
        new Thread(getServerGroup(),"Command Thread")
        {
            @Override
            public void run() {
                this.setUncaughtExceptionHandler(CrashReport.getCrashReport());
                //IO初始化
                Scanner scanner = new Scanner(System.in);
                //ChatRequest初始化

                //命令系统
                while (true)
                {
                    String Command = "/"+scanner.nextLine();
                    if ("/ShutdownConsoleCommandSystem".equals(Command))
                    {
                        getLogger().info("已退出控制台命令系统");
                        break;
                    }
                    ServerCommandSend(Command);
                }
            }
        }.start();
    }

    @Override
    protected void StartRecvMessageThread(int ClientID) {
        super.StartRecvMessageThread(ClientID);
        user ConnectUser = getUsers().get(ClientID);
        ConnectUser.addLoginRecall(() -> GUIController.UpdateUser(true,ConnectUser.getUserName()));
        ConnectUser.addDisconnectRecall(() -> {
            if (ConnectUser.isUserLogined())
            {
                GUIController.UpdateUser(false,ConnectUser.getUserName());
            }
        });
    }

    public void StopServer() {
        getServerAPI().SendMessageToAllClient("服务器已关闭",ServerMain.getServer());
        for (user User : getUsers())
        {
            User.UserDisconnect();
        }
        authThread.interrupt();
        try {
            ServerMain.getServer().getPluginManager().UnLoadAllPlugin();
        } catch (IOException e) {
            SaveStackTrace.saveStackTrace(e);
        }
        getLogger().OutDate();
        started = false;
        if (socket != null && !socket.isClosed())
        {
            try {
                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
                SaveStackTrace.saveStackTrace(e);
            }
        }
        server = null;
        getServerGroup().interrupt();
    }

    public void ServerCommandSend(String Command) {
        if (Command.equals("/quit"))
        {
            getLogger().info("正在关闭服务器...");
            DefaultController.StopServer();
            return;
        }
        UserRequestThreadPool.execute(() -> getRequest().CommandRequest(
                new ChatRequest.ChatRequestInput(getConsoleUser(),"/"+Command)));
    }
}
