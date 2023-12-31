package org.yuezhikong.GraphicalUserInterface;

import javafx.application.Platform;
import javafx.scene.control.Alert;
import org.yuezhikong.GraphicalUserInterface.Dialogs.LoginDialog;
import org.yuezhikong.GraphicalUserInterface.ServerAndKeyManagement.SavedServerFileLayout;
import org.yuezhikong.newClient.NettyClient;
import org.yuezhikong.utils.Logger;

import java.util.Optional;

public class NettyClientGUIDependsData extends NettyClient.ClientDependsData {
    private final ClientUI GUIController;

    @Override
    protected Logger initLogger() {
        return new Logger(GUIController);
    }

    public NettyClientGUIDependsData(ClientUI controller)
    {
        GUIController = controller;
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
                NettyClient.getInstance().stop();
            }
            else
            {
                writeLoginInformation(UserInput.UserName(),UserInput.Password(),UserInput.isLegacyLogin());
            }
        }

        public void requestUserNameAndPassword()
        {
            Platform.runLater(() -> {
                try {
                    LoginDialog dialog = new LoginDialog(DefaultController.getStage());
                    Optional<LoginDialog.DialogReturn> UserLoginData = dialog.showAndWait();
                    if (UserLoginData.isPresent() && UserLoginData.get().UserName() != null
                            && UserLoginData.get().Password() != null) {
                        UserInput = UserLoginData.get();
                    } else {
                        Alert alert = new Alert(Alert.AlertType.INFORMATION);
                        alert.setHeaderText("已取消连接服务器");
                        alert.setContentText("因为您已经取消了登录");
                        alert.show();
                        UserInput = null;
                    }
                }
                finally {
                    RequestSuccess = true;
                    synchronized (wait)
                    {
                        wait.notifyAll();
                    }
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
    private String[] UserData = new String[] {};
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

    public void writeLoginInformation(String userName, String password, boolean isLegacyLogin) {
        UserData = new String[] { userName , password };
        LegacyLogin = isLegacyLogin;
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
