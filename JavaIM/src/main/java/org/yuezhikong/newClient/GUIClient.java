package org.yuezhikong.newClient;

import cn.hutool.crypto.symmetric.AES;
import com.google.gson.Gson;
import javafx.application.Platform;
import javafx.scene.control.Alert;
import org.yuezhikong.CodeDynamicConfig;
import org.yuezhikong.GraphicalUserInterface.ClientUI;
import org.yuezhikong.GraphicalUserInterface.DefaultController;
import org.yuezhikong.GraphicalUserInterface.Dialogs.LoginDialog;
import org.yuezhikong.utils.Logger;
import org.yuezhikong.utils.Protocol.NormalProtocol;
import org.yuezhikong.utils.SaveStackTrace;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.nio.Buffer;
import java.util.Optional;

public class GUIClient extends ClientMain {

    private volatile boolean ClientStartedSuccessful = false;
    private final Object ClientStartedLock = new Object();
    private final ClientUI GUIController;
    private Logger logger;
    @Override
    protected Logger LoggerInit() {
        logger = new Logger(GUIController);
        return logger;
    }

    public GUIClient(ClientUI controller)
    {
        SpecialMode = true;
        GUIController = controller;
    }

    //此时，SendMessage是start调用的最后一个函数
    @Override
    protected void SendMessage(Socket socket, AES aes) {
        ClientStartedSuccessful = true;
        synchronized (ClientStartedLock)
        {
            ClientStartedLock.notifyAll();
        }
        super.SendMessage(socket, aes);
    }

    @Override
    protected boolean CommandRequest(AES aes, String UserInput, BufferedWriter writer) throws IOException {
        try {
            return super.CommandRequest(aes, UserInput, writer);
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
        if (UserData[0].equals("") && UserData[1].equals(""))
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
        if (getClientStopStatus())
        {
            return;
        }
        Gson gson = new Gson();
        NormalProtocol protocol = new NormalProtocol();
        NormalProtocol.MessageHead head = new NormalProtocol.MessageHead();
        head.setVersion(CodeDynamicConfig.getProtocolVersion());
        head.setType("Chat");
        protocol.setMessageHead(head);
        NormalProtocol.MessageBody body = new NormalProtocol.MessageBody();
        body.setMessage(Message);
        body.setFileLong(0);
        BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(getSocket().getOutputStream()));
        protocol.setMessageBody(body);
        writer.write(getAes().encryptBase64(gson.toJson(protocol)));
        writer.newLine();
        writer.flush();
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
            new Thread(getClientThreadGroup(),"RequestThread") {
                @Override
                public void run() {
                    if (!ClientStartedSuccessful) {
                        synchronized (ClientStartedLock) {
                            if (!ClientStartedSuccessful) {
                                try {
                                    ClientStartedLock.wait();
                                } catch (InterruptedException e) {
                                    SaveStackTrace.saveStackTrace(e);
                                }
                            }
                        }
                        UserInputRequest(Message);
                    }
                }
            }.start();
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
            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(getSocket().getOutputStream()));
            if (!CommandRequest(getAes(),Message,writer))
            {
                SendMessageToServer(Message);
            }
        } catch (IOException e) {
            SaveStackTrace.saveStackTrace(e);
            logger.info("客户端出现致命错误，正在关闭");
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
        if (recvMessageThread != null) {
            recvMessageThread.interrupt();
        }
        getClientThreadGroup().interrupt();
        try {
            getSocket().close();
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
            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(getSocket().getOutputStream()));
            writer.write(getAes().encryptBase64(gson.toJson(protocol)));
            writer.newLine();
            writer.flush();
            StopClientNoSendQuitMessage();
        } catch (IOException e) {
            SaveStackTrace.saveStackTrace(e);
        }
        finally {
            super.ClientStatus = true;
            getClientThreadGroup().interrupt();
        }
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
        if (UserData[0].equals("") && UserData[1].equals(""))
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
}
