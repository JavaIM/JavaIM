package org.yuezhikong.GraphicalUserInterface;

import com.google.gson.Gson;
import javafx.application.Platform;
import javafx.scene.control.Alert;
import org.yuezhikong.CodeDynamicConfig;
import org.yuezhikong.GraphicalUserInterface.Dialogs.LoginDialog;
import org.yuezhikong.GraphicalUserInterface.ServerAndKeyManagement.SavedServerFileLayout;
import org.yuezhikong.NetworkManager;
import org.yuezhikong.newClient.ClientMain;
import org.yuezhikong.utils.Logger;
import org.yuezhikong.utils.Protocol.NormalProtocol;
import org.yuezhikong.utils.SaveStackTrace;

import java.io.File;
import java.io.IOException;
import java.util.Optional;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

public class GUIClient extends ClientMain {

    private final File ServerPublicKeyFile;
    private volatile boolean ClientStartedSuccessful = false;
    private final Object ClientStartedLock = new Object();
    private final ClientUI GUIController;
    private Logger logger;
    @Override
    protected Logger LoggerInit() {
        logger = new Logger(GUIController);
        return logger;
    }

    @Override
    protected File getServerPublicKeyFile() {
        return ServerPublicKeyFile;
    }

    public GUIClient(ClientUI controller,File ServerPublicKey)
    {
        SpecialMode = true;
        GUIController = controller;
        ServerPublicKeyFile = ServerPublicKey;
    }

    //此时，SendMessage是start调用的最后一个函数
    @Override
    protected void SendMessage() {
        ClientStartedSuccessful = true;
        synchronized (ClientStartedLock)
        {
            ClientStartedLock.notifyAll();
        }
        super.SendMessage();
        StopClient();
    }


    @Override
    protected boolean CommandRequest(String UserInput) throws IOException {
        try {
            return super.CommandRequest(UserInput);
        } catch (QuitException e)
        {
            logger.info("正在关闭客户端...");
            StopClientNoSendQuitMessage();
            return true;
        }
    }

    class RequestUserNameAndPassword
    {
        private volatile boolean RequestSuccess = false;
        private final Object wait = new Object();
        private LoginDialog.DialogReturn UserInput;
        public void waitUserNameAndPassword()
        {
            if (!RequestSuccess)
            {
                synchronized (wait)
                {
                    if (!RequestSuccess)
                    {
                        try {
                            wait.wait();
                        } catch (InterruptedException e) {
                            return;
                        }
                    }
                }
            }
            if (UserInput == null)
            {
                StopClient();
            }
            else
            {
                writeRequiredInformation(UserInput.UserName(),UserInput.Password(),UserInput.isLegacyLogin());
            }
        }

        public void requestUserNameAndPassword()
        {
            Platform.runLater(() -> {
                LoginDialog dialog = new LoginDialog(DefaultController.getStage());
                Optional<LoginDialog.DialogReturn> UserLoginData = dialog.showAndWait();
                if (UserLoginData.isPresent() && UserLoginData.get().UserName() != null
                        && UserLoginData.get().Password() != null) {
                    UserInput = UserLoginData.get();
                }
                else {
                    Alert alert = new Alert(Alert.AlertType.INFORMATION);
                    alert.setHeaderText("已取消连接服务器");
                    alert.setContentText("因为您已经取消了登录");
                    alert.show();
                    UserInput = null;
                }
                RequestSuccess = true;
                synchronized (wait)
                {
                    wait.notifyAll();
                }
            });
        }
    }
    @Override
    protected boolean isLegacyLoginORNormalLogin() {
        if (UserData[0].isEmpty() && UserData[1].isEmpty())
        {
            RequestUserNameAndPassword request = new RequestUserNameAndPassword();
            request.requestUserNameAndPassword();
            request.waitUserNameAndPassword();
        }
        return LegacyLogin;
    }

    /**
     * 发送一条聊天信息到服务器
     * @param Message 聊天信息
     */
    public void SendMessageToServer(String Message) throws IOException {
        AtomicBoolean Complete = new AtomicBoolean(false);
        AtomicReference<IOException> exception = new AtomicReference<>(null);
        userRequestDisposeThreadPool.execute(() -> {
            Gson gson = new Gson();
            NormalProtocol protocol = new NormalProtocol();
            NormalProtocol.MessageHead head = new NormalProtocol.MessageHead();
            head.setVersion(CodeDynamicConfig.getProtocolVersion());
            head.setType("Chat");
            protocol.setMessageHead(head);
            NormalProtocol.MessageBody body = new NormalProtocol.MessageBody();
            body.setMessage(Message);
            body.setFileLong(0);
            protocol.setMessageBody(body);
            try {
                NetworkManager.WriteDataToRemote(getClientNetworkData(),getAes().encryptBase64(gson.toJson(protocol)));
            } catch (IOException e) {
                exception.set(e);
            }
            Complete.set(true);
            synchronized (Complete)
            {
                Complete.notifyAll();
            }
        });
        if (!Complete.get())
        {
            synchronized (Complete)
            {
                if (!Complete.get())
                {
                    try {
                        Complete.wait();
                    } catch (InterruptedException e) {
                        SaveStackTrace.saveStackTrace(e);
                    }
                }
            }
        }
        if (exception.get() != null)
            throw exception.get();
    }

    /**
     * 对用户输入进行处理
     * @param Message 信息
     */
    public void UserInputRequest(String Message)
    {
        if (getClientStopStatus())
        {
            return;
        }
        if (!ClientStartedSuccessful) {
            return;
        }

        if (needConsoleInput)
        {
            synchronized (ConsoleInputLock)
            {
                if (needConsoleInput)
                {
                    needConsoleInput = false;
                    ConsoleInput = Message;
                    ConsoleInputLock.notifyAll();
                    return;
                }
            }
        }

        try {
            if (!CommandRequest(Message))
            {
                SendMessageToServer(Message);
            }
        } catch (IOException e) {
            SaveStackTrace.saveStackTrace(e);
            logger.info("客户端疑似已经关闭!");
            StopClient();
        }
    }

    /**
     * 关闭服务器，但是不发送Quit消息
     * @throws IOException Socket关闭失败
     */
    private void StopClientNoSendQuitMessage() throws IOException {
        QuitReason = "用户界面要求关闭";
        ClientMain.getClient().getLogger().OutDate();
        if (getTimerThreadPool() != null && !getTimerThreadPool().isShutdown())
            getTimerThreadPool().shutdownNow();
        if (recvMessageThread != null) {
            recvMessageThread.interrupt();
        }
        getClientThreadGroup().interrupt();
        try {
            if (getClientNetworkData() != null)
                NetworkManager.ShutdownTCPConnection(getClientNetworkData());
        } finally {
            Instance = null;
        }
    }

    /**
     * 关闭客户端
     */
    public void StopClient() {
        if (getClientStopStatus())
        {
            return;
        }
        Gson gson = new Gson();
        NormalProtocol protocol = new NormalProtocol();
        NormalProtocol.MessageHead head = new NormalProtocol.MessageHead();
        head.setVersion(CodeDynamicConfig.getProtocolVersion());
        head.setType("Leave");
        protocol.setMessageHead(head);
        NormalProtocol.MessageBody body = new NormalProtocol.MessageBody();
        body.setMessage(".quit");
        body.setFileLong(0);
        protocol.setMessageBody(body);
        try {
            NetworkManager.WriteDataToRemote(getClientNetworkData(),getAes().encryptBase64(gson.toJson(protocol)));
        } catch (IOException e) {
            SaveStackTrace.saveStackTrace(e);
        }
        try
        {
            StopClientNoSendQuitMessage();
        } catch (IOException e) {
            SaveStackTrace.saveStackTrace(e);
        } finally {
            super.ClientStatus = true;
            getClientThreadGroup().interrupt();
        }
    }

    @Override
    public synchronized ScheduledExecutorService getTimerThreadPool() {
        return super.getTimerThreadPool();
    }

    @Override
    public ThreadGroup getClientThreadGroup() {
        return super.getClientThreadGroup();
    }

    @Override
    public NetworkManager.NetworkData getClientNetworkData() {
        return super.getClientNetworkData();
    }

    @Override
    public void start(String ServerAddress, int ServerPort) {
        ThreadGroup ClientGroup = new ThreadGroup(Thread.currentThread().getThreadGroup(),"ClientThreadGroup");
        new Thread(ClientGroup,"waitInterrupt Thread")
        {
            private final Object wait = new Object();
            @Override
            public void run() {
                synchronized (wait)
                {
                    try {
                        wait.wait();
                    } catch (InterruptedException ignored) {}
                }
                GUIController.onClientShutdown();
            }
        }.start();
        new Thread(ClientGroup,"Client Thread")
        {
            @Override
            public void run() {
                GUIClient.super.start(ServerAddress, ServerPort);
            }
        }.start();
    }

    private String[] UserData;
    private boolean LegacyLogin = false;
    @Override
    protected String[] RequestUserNameAndPassword() {
        if (UserData[0].isEmpty() && UserData[1].isEmpty())
        {
            RequestUserNameAndPassword request = new RequestUserNameAndPassword();
            request.requestUserNameAndPassword();
            request.waitUserNameAndPassword();
        }
        String[] UserData = this.UserData;
        this.UserData = new String[] {"",""};
        return UserData;
    }

    public void writeRequiredInformation(String userName, String password, boolean isLegacyLogin) {
        UserData = new String[] { userName , password };
        LegacyLogin = isLegacyLogin;
    }

    private boolean AllowShutdownTimerThreadPool = true;
    public void setTimerThreadPool(ScheduledExecutorService timerThreadPool,boolean AllowShutdownTimerThreadPool)
    {
        if (timerThreadPool == null)
            return;
        this.AllowShutdownTimerThreadPool = AllowShutdownTimerThreadPool;
        TimerThreadPool = timerThreadPool;
    }

    @Override
    protected boolean AllowShutdownScheduledExecutorService() {
        return AllowShutdownTimerThreadPool;
    }

    private SavedServerFileLayout.ServerInformationBean serverInformation;
    public void setServerInformation(SavedServerFileLayout.ServerInformationBean information)
    {
        serverInformation = information;
    }

    public SavedServerFileLayout.ServerInformationBean getServerInformation() {
        return serverInformation;
    }

    @Override
    protected String RequestUserToken() {
        return GUIController.RequestUserToken();
    }

    @Override
    protected void writeUserToken(String UserToken) {
        GUIController.writeUserToken(UserToken);
    }
}
