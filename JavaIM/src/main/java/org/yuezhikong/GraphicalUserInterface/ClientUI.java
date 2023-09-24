package org.yuezhikong.GraphicalUserInterface;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import org.yuezhikong.CodeDynamicConfig;
import org.yuezhikong.GraphicalUserInterface.Dialogs.LoginDialog;
import org.yuezhikong.newClient.ClientMain;
import org.yuezhikong.newClient.GUIClient;

import java.io.File;
import java.net.URL;
import java.util.Optional;
import java.util.ResourceBundle;

public class ClientUI extends DefaultController implements Initializable {
    public TextField ServerPort;
    public TextField ServerAddress;
    public CheckBox TransferProtocolConfig;
    public TextField InputMessage;
    public TextArea ChatMessage;
    public TextArea SystemLog;

    private GUIClient Instance;


    @Override
    public void WriteChatMessage(String msg) {
        Platform.runLater(() -> ChatMessage.appendText(msg+"\n"));
    }

    @Override
    public void WriteSystemLog(String msg) {
        Platform.runLater(() -> SystemLog.appendText(msg+"\n"));
    }
    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        TransferProtocolConfig.setSelected(CodeDynamicConfig.AllowedTransferProtocol);
        ServerPort.setTextFormatter(new TextFormatter<String>(change -> {
            try
            {
                if (!("".equals(change.getText())))
                {
                    int integer = Integer.parseInt(change.getText());
                    if (integer > 65535
                            || integer < 0
                            || (Integer.parseInt(ServerPort.getText() + integer) > 65535)
                            || (Integer.parseInt(ServerPort.getText() + integer) == 0))
                    {
                        return null;
                    }
                }
            } catch (NumberFormatException e)
            {
                return null;
            }
            return change;
        }));
        ChatMessage.textProperty().addListener(
                (observableValue, oldValue, newValue) -> ChatMessage.setScrollTop(Double.MAX_VALUE)
        );
        SystemLog.textProperty().addListener(
                (observableValue, oldValue, newValue) -> SystemLog.setScrollTop(Double.MAX_VALUE)
        );
    }

    public void UpdateConfig(ActionEvent actionEvent) {
        CodeDynamicConfig.AllowedTransferProtocol = TransferProtocolConfig.isSelected();
    }

    public void ConnectorDisconnectToServer(ActionEvent actionEvent) {
        if (ClientMain.getClient() == null) {
            if (new File("./token.txt").exists())
            {
                Instance = new GUIClient(this);
                Instance.writeRequiredInformation("","", false);
                Instance.start(ServerAddress.getText(), Integer.parseInt(ServerPort.getText()));
                return;
            }
            LoginDialog dialog = new LoginDialog(stage);
            Optional<LoginDialog.DialogReturn> UserLoginData = dialog.showAndWait();
            if (UserLoginData.isPresent() && UserLoginData.get().UserName() != null
                    && UserLoginData.get().Password() != null) {
                String UserName = UserLoginData.get().UserName();
                String Password = UserLoginData.get().Password();
                //后续代码等待正式开启ui系统
                Instance = new GUIClient(this);
                Instance.writeRequiredInformation(UserName,Password, UserLoginData.get().isLegacyLogin());
                Instance.start(ServerAddress.getText(), Integer.parseInt(ServerPort.getText()));
            }
            else {
                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.initOwner(stage);
                alert.setTitle("请求已被取消");
                alert.setHeaderText("已取消连接服务器");
                alert.setContentText("因为您已经取消了登录");
                alert.showAndWait();
            }
        }
        else
        {
            Instance = null;
            StopClient();
        }
    }

    public void SendMessage(ActionEvent actionEvent) {
        if (Instance == null)
        {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.initOwner(stage);
            alert.setTitle("无法完成操作");
            alert.setHeaderText("无法发送信息");
            alert.setContentText("客户端未启动");
            alert.showAndWait();
            return;
        }
        Instance.UserInputRequest(InputMessage.getText());
    }

    public void onClientShutdown() {
        Instance = null;
        if (ClientMain.getClient() != null)
        {
            StopClient();
        }
    }
}
