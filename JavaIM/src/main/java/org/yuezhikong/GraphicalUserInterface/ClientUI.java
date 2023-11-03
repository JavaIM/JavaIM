package org.yuezhikong.GraphicalUserInterface;

import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.*;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.shape.Polygon;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.stage.FileChooser;
import org.apache.commons.io.FileUtils;
import org.jetbrains.annotations.NotNull;
import org.yuezhikong.CodeDynamicConfig;
import org.yuezhikong.GraphicalUserInterface.Dialogs.LoginDialog;
import org.yuezhikong.GraphicalUserInterface.Dialogs.PortInputDialog;
import org.yuezhikong.newClient.ClientMain;
import org.yuezhikong.newClient.GUIClient;
import org.yuezhikong.utils.SaveStackTrace;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

public class ClientUI extends DefaultController implements Initializable {
    public CheckBox TransferProtocolConfig;
    public TextField InputMessage;
    public TextArea ChatMessage;
    public TextArea SystemLog;
    public FlowPane messages;
    public ScrollPane scrollpane;

    private GUIClient Instance;
    public static ScheduledExecutorService TimerThreadPool;


    @Override
    public void WriteChatMessage(String msg) {
        //如果系统支持SystemTray，则显示信息(Windows7 气泡、Windows 10 通知等）
        if (SystemTrayIcon != null)
        {
            SystemTrayIcon.displayMessage("JavaIM 客户端",msg, TrayIcon.MessageType.INFO);
        }
        
        //测试版GUI
        TestDisplayGUI(msg);
        //显示在GUI
        Platform.runLater(() -> ChatMessage.appendText(msg+"\n"));
    }

    private void TestDisplayGUI(String msg) {
        TestDisplayGUI(msg,"未知",false);
    }

    private boolean last = false;
    private void TestDisplayGUI(String msg,String name,boolean isMine)
    {
        if (!Platform.isFxApplicationThread())
        {
            Platform.runLater(() -> TestDisplayGUI(msg,name,isMine));
            return;
        }
        Label head = new Label(name);
        head.setPrefSize(40,40);
        head.setMinSize(40,40);
        head.setMaxSize(40,40);
        head.setCenterShape(true);
        head.setStyle("-fx-background-color: rgb(179,231,244);");

        Label messageBubble = new Label(msg);
        messageBubble.setWrapText(true);
        messageBubble.setMaxWidth(220);
        messageBubble.setStyle("-fx-background-color: rgb(179,231,244); -fx-background-radius: 8px;");
        messageBubble.setPadding(new Insets(6));
        messageBubble.setFont(new Font(14));
        HBox.setMargin(messageBubble,new Insets(8,0,0,0));

        double[] points;
        if (isMine)
        {
            points = new double[] {
                    0.0, 0.0,
                    0.0,10.0,
                    10.0,5.0
            };
        }
        else
        {
            points = new double[] {
                    0.0, 5.0,
                    10.0,0.0,
                    10.0,10.0
            };
        }
        Polygon triangle = new Polygon(points);
        triangle.setFill(Color.rgb(179,231,244));
        HBox messageBox = new HBox();
        messageBox.setPrefWidth(366);
        messageBox.setPadding(new Insets(10,5,10,5));
        if (isMine)
        {
            HBox.setMargin(triangle,new Insets(15,10,0,0));
            messageBox.getChildren().addAll(messageBubble,triangle,head);
            messageBox.setAlignment(Pos.TOP_RIGHT);
        }
        else
        {
            HBox.setMargin(triangle,new Insets(15,0,0,10));
            messageBox.getChildren().addAll(head,triangle,messageBubble);
        }
        last = scrollpane.getVvalue() == 1.0;
        messages.getChildren().add(messageBox);
    }

    @Override
    public void WriteSystemLog(String msg) {
        Platform.runLater(() -> SystemLog.appendText(msg+"\n"));
    }
    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        scrollpane.vvalueProperty().addListener((observableValue, number, t1) -> {
            if (last)
            {
                scrollpane.setVvalue(1.0);
                last = false;
            }
        });
        TestDisplayGUI("测试1——他人","测试",false);
        TestDisplayGUI("测试2——自己","测试",true);
        TransferProtocolConfig.setSelected(CodeDynamicConfig.AllowedTransferProtocol);
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

    /**
     * 启动一个GUI客户端
     * @param ServerAddress 服务器地址
     * @param ServerPort 服务器端口
     * @param ServerPublicKey 服务端公钥，null为默认
     */
    private void StartClient(String ServerAddress,String ServerPort,File ServerPublicKey)
    {
        if (new File("./token.txt").exists())
        {
            Instance = new GUIClient(this);
            Instance.writeRequiredInformation("","", false);
            Instance.start(ServerAddress, Integer.parseInt(ServerPort));
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
            //为null时使用默认路径
            if (ServerPublicKey != null)
            {
                Instance.setServerPublicKeyFile(ServerPublicKey);
            }
            if (TimerThreadPool == null)
                TimerThreadPool = Executors.newScheduledThreadPool(1, new ThreadFactory() {
                    private final AtomicInteger threadNumber = new AtomicInteger(1);

                    @Override
                    public Thread newThread(@NotNull Runnable r) {
                        return new Thread(r, "Timer Thread #" + threadNumber.getAndIncrement());
                    }
                });
            Instance.setTimerThreadPool(TimerThreadPool,false);
            Instance.start(ServerAddress, Integer.parseInt(ServerPort));
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
    public void DirectConnectOrDisconnectToServer(ActionEvent actionEvent) {
        if (ClientMain.getClient() == null) {

            //IP
            TextInputDialog textInputDialog = new TextInputDialog();
            textInputDialog.initOwner(stage);
            textInputDialog.setTitle("JavaIM --- 启动客户端");
            textInputDialog.setHeaderText("如果要启动客户端，请提供以下信息");
            textInputDialog.setContentText("请输入服务器IP地址：");
            ((Button) (textInputDialog.getDialogPane().lookupButton(ButtonType.OK))).setOnAction((actionEvent1) -> textInputDialog.getEditor().clear());
            Optional<String> ServerAddressOfUserInput = textInputDialog.showAndWait();
            if (ServerAddressOfUserInput.isEmpty() || ServerAddressOfUserInput.get().equals(""))
            {
                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setHeaderText("已取消启动客户端");
                alert.setContentText("因为您已经取消了输入必须信息");
                alert.showAndWait();
                return;
            }

            //端口
            PortInputDialog portInputDialog = new PortInputDialog();
            portInputDialog.initOwner(stage);
            textInputDialog.setTitle("JavaIM --- 启动客户端");
            textInputDialog.setHeaderText("如果要启动客户端，请提供以下信息");
            textInputDialog.setContentText("请输入服务器端口：");
            Optional<String> ServerPortOfUserInput = textInputDialog.showAndWait();
            if (ServerPortOfUserInput.isEmpty() || ServerPortOfUserInput.get().equals(""))
            {
                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setTitle("JavaIM --- 提示");
                alert.setHeaderText("已取消启动客户端");
                alert.setContentText("因为您已经取消了输入必须信息");
                alert.showAndWait();
                return;
            }

            //请求服务端公钥文件
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.initOwner(stage);
            alert.setTitle("JavaIM --- 提示");
            alert.setHeaderText("如果要连接服务端，需要服务端公钥");
            alert.setContentText("是否继续连接服务端?");
            Optional<ButtonType> select = alert.showAndWait();
            if (select.isEmpty() || !(select.get().equals(ButtonType.OK)))
            {
                return;
            }
            FileChooser chooser = new FileChooser();
            chooser.setTitle("打开 服务端公钥文件");
            chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("服务端公钥文件 (*.txt)","*.txt"));
            File ServerPublicKeyFile = chooser.showOpenDialog(stage);
            if (ServerPublicKeyFile == null)
            {
                alert.setAlertType(Alert.AlertType.INFORMATION);
                alert.setTitle("JavaIM --- 提示");
                alert.setHeaderText("已取消启动客户端");
                alert.setContentText("因为您已经取消了选择服务端公钥文件");
                alert.showAndWait();
                return;
            }
            try {
                File tempServerPublicKeyFile = File.createTempFile("ServerPublicKey",".txt");
                FileUtils.copyFile(ServerPublicKeyFile,tempServerPublicKeyFile);
                tempServerPublicKeyFile.deleteOnExit();
                StartClient(ServerAddressOfUserInput.get(),ServerPortOfUserInput.get(),tempServerPublicKeyFile);
            } catch (IOException e) {
                SaveStackTrace.saveStackTrace(e);
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
        try {
            if (ClientMain.getClient() != null) {
                StopClient();
            }
        } catch (Throwable throwable)
        {
            SaveStackTrace.saveStackTrace(throwable);
        }
        finally {
            if (Instance.getTimerThreadPool() != null)
                Instance.getTimerThreadPool().shutdownNow();
            if (Instance.getClientThreadGroup() != null)
                Instance.getClientThreadGroup().interrupt();
            if (Instance.getSocket() != null && !Instance.getSocket().isClosed()) {
                try {
                    Instance.getSocket().close();
                } catch (IOException ignored) {}
            }
            Instance = null;
        }
    }
}
