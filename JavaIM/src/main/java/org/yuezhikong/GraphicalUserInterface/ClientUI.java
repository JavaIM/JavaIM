package org.yuezhikong.GraphicalUserInterface;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.*;
import javafx.stage.FileChooser;
import org.apache.commons.io.FileUtils;
import org.yuezhikong.CodeDynamicConfig;
import org.yuezhikong.GraphicalUserInterface.Dialogs.PortInputDialog;
import org.yuezhikong.GraphicalUserInterface.Dialogs.SpinnerDialog;
import org.yuezhikong.GraphicalUserInterface.ServerAndKeyManagement.SavedServerFileLayout;
import org.yuezhikong.GraphicalUserInterface.ServerAndKeyManagement.ServerAndKeyManagementUI;
import org.yuezhikong.newClient.NettyClient;
import org.yuezhikong.utils.Protocol.NormalProtocol;
import org.yuezhikong.utils.SaveStackTrace;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.concurrent.ScheduledExecutorService;

public class ClientUI extends DefaultController implements Initializable {
    public CheckBox TransferProtocolConfig;
    public TextField InputMessage;
    public TextArea ChatMessage;
    public TextArea SystemLog;
    public Button DirectConnectToServerButton;
    public Button DisconnectServerButton;
    public Button ConnectToSavedServerButton;

    private NettyClientGUIDependsData NettyClientDependsData;
    public static ScheduledExecutorService TimerThreadPool;


    @Override
    public void WriteChatMessage(String msg) {
        //如果系统支持SystemTray，则显示信息(Windows7 气泡、Windows 10 通知等）
        if (SystemTrayIcon != null)
        {
            SystemTrayIcon.displayMessage("JavaIM 客户端",msg, TrayIcon.MessageType.INFO);
        }

        //显示在GUI
        Platform.runLater(() -> ChatMessage.appendText(msg+"\n"));
    }

    @Override
    public void WriteSystemLog(String msg) {
        Platform.runLater(() -> SystemLog.appendText(msg+"\n"));
    }
    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
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
    private void StartClient(String ServerAddress, int ServerPort, String ServerPublicKey)
    {
        NettyClientDependsData = new NettyClientGUIDependsData(this);
        StartClient(NettyClientDependsData,ServerAddress, ServerPort, ServerPublicKey, null);
    }
    /**
     * 启动一个GUI客户端
     * @param data netty客户端依赖数据
     * @param ServerAddress 服务器地址
     * @param ServerPort 服务器端口
     * @param ServerPublicKey 服务端公钥，null为默认
     * @param informationBean 服务器信息，null为空
     */
    private void StartClient(NettyClientGUIDependsData data,String ServerAddress, int ServerPort, String ServerPublicKey, SavedServerFileLayout.ServerInformationBean informationBean)
    {
        data.setServerInformation(informationBean);
        NettyClient.getInstance().writeDependsData(data);
        new Thread(new ThreadGroup(Thread.currentThread().getThreadGroup(),"Client Thread Group"),
                () ->
                        NettyClient.getInstance().start(ServerAddress, ServerPort,ServerPublicKey)
                ,"Client Thread").start();
    }
    public void DirectConnectToServer(ActionEvent actionEvent) {
        //IP
        TextInputDialog textInputDialog = new TextInputDialog();
        textInputDialog.initOwner(stage);
        textInputDialog.setTitle("JavaIM --- 启动客户端");
        textInputDialog.setHeaderText("如果要启动客户端，请提供以下信息");
        textInputDialog.setContentText("请输入服务器IP地址：");
        ((Button) (textInputDialog.getDialogPane().lookupButton(ButtonType.OK))).setOnAction((actionEvent1) -> textInputDialog.getEditor().clear());
        Optional<String> ServerAddressOfUserInput = textInputDialog.showAndWait();
        if (ServerAddressOfUserInput.isEmpty() || ServerAddressOfUserInput.get().isEmpty()) {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setHeaderText("已取消启动客户端");
            alert.setContentText("因为您已经取消了输入必须信息");
            alert.showAndWait();
            return;
        }

        //端口
        PortInputDialog portInputDialog = new PortInputDialog();
        portInputDialog.initOwner(stage);
        portInputDialog.setTitle("JavaIM --- 启动客户端");
        portInputDialog.setHeaderText("如果要启动客户端，请提供以下信息");
        portInputDialog.setContentText("请输入服务器端口：");
        Optional<String> ServerPortOfUserInput = portInputDialog.showAndWait();
        if (ServerPortOfUserInput.isEmpty() || ServerPortOfUserInput.get().isEmpty()) {
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
        if (select.isEmpty() || !(select.get().equals(ButtonType.OK))) {
            return;
        }
        FileChooser chooser = new FileChooser();
        chooser.setTitle("打开 服务端公钥文件");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("服务端公钥文件 (*.txt)", "*.txt"));
        File ServerPublicKeyFile = chooser.showOpenDialog(stage);
        if (ServerPublicKeyFile == null) {
            alert.setAlertType(Alert.AlertType.INFORMATION);
            alert.setTitle("JavaIM --- 提示");
            alert.setHeaderText("已取消启动客户端");
            alert.setContentText("因为您已经取消了选择服务端公钥文件");
            alert.showAndWait();
            return;
        }
        try {
            StartClient(ServerAddressOfUserInput.get(), Integer.parseInt(ServerPortOfUserInput.get()), FileUtils.readFileToString(ServerPublicKeyFile, StandardCharsets.UTF_8));
            DisconnectServerButton.setDisable(false);
            DirectConnectToServerButton.setDisable(true);
            ConnectToSavedServerButton.setDisable(true);
        } catch (IOException e) {
            SaveStackTrace.saveStackTrace(e);
        }
    }
    public void DisconnectServer()
    {
        try
        {
            StopClient();
        } finally {
            DisconnectServerButton.setDisable(true);
            DirectConnectToServerButton.setDisable(false);
            ConnectToSavedServerButton.setDisable(false);
        }
    }

    public void ConnectToSavedServer()
    {
        File SavedServerFile = new File("./SavedServers.json");
        if (!SavedServerFile.exists() || SavedServerFile.isDirectory() || SavedServerFile.length() == 0)
        {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.initOwner(stage);
            alert.setTitle("无法完成操作");
            alert.setHeaderText("文件系统出错");
            alert.setContentText("保存的服务器文件不存在或为一个目录或为空文件，请通过“保存的服务器管理器”重新设置保存的服务器");
            alert.showAndWait();
            return;
        }
        SavedServerFileLayout layout;
        Gson gson = new Gson();
        try {
            try {
                layout = gson.fromJson(FileUtils.readFileToString(SavedServerFile, StandardCharsets.UTF_8)
                        , SavedServerFileLayout.class);
                ServerAndKeyManagementUI.tryUpdateSavedServerLayout(layout);
                FileUtils.writeStringToFile(SavedServerFile,gson.toJson(layout), StandardCharsets.UTF_8);
            } catch (JsonSyntaxException e) {
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.initOwner(stage);
                alert.setTitle("JavaIM --- 出现错误");
                alert.setContentText("无法解析保存的服务器文件，请检查文件内容");
                alert.showAndWait();
                return;
            }

            var canSelectHashMap = ServerAndKeyManagementUI.
                    getServerManagementCanSelectHashMap(layout.getServerInformation());
            if (canSelectHashMap.isEmpty())
            {
                Alert alert = new Alert(Alert.AlertType.NONE);
                alert.setTitle("JavaIM --- 提示");
                alert.setContentText("当前没有添加服务器，去添加一个吧!");
                alert.getButtonTypes().add(ButtonType.OK);
                alert.initOwner(stage);
                alert.showAndWait();
                return;
            }
            List<String> canSelectList = new ArrayList<>();
            canSelectHashMap.forEach((string, serverPortAndServerAddress) -> canSelectList.add(string));
            SpinnerDialog dialog = new SpinnerDialog(canSelectList,stage,"JavaIM --- 选择操作的服务器");
            Optional<SpinnerDialog.DialogReturn> returnOptional = dialog.showAndWait();
            if (returnOptional.isEmpty())
                return;
            var SelectServerData = canSelectHashMap.get(returnOptional.get().SpinnerSelect());
            var serverInformation = layout.getServerInformation();

            for (var bean : serverInformation)
            {
                if (SelectServerData.ServerPort() == bean.getServerPort()
                        && SelectServerData.ServerAddress().equals(bean.getServerAddress()))
                {
                    NettyClientDependsData = new NettyClientGUIDependsData(this);
                    NettyClientDependsData.setServerInformation(bean);
                    StartClient(NettyClientDependsData,bean.getServerAddress(),
                            bean.getServerPort(),bean.getServerPublicKey(),bean);
                    DisconnectServerButton.setDisable(false);
                    DirectConnectToServerButton.setDisable(true);
                    ConnectToSavedServerButton.setDisable(true);
                    return;
                }
            }
        } catch (IOException e)
        {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.initOwner(stage);
            alert.setTitle("JavaIM --- 出现错误");
            alert.setContentText("""
                    由于出现文件系统错误，无法读取保存的服务器
                    请您检查本程序是否可以在当前目录下读写文件
                    程序由于此问题，启动客户端已被取消!""");
            alert.showAndWait();
            SaveStackTrace.saveStackTrace(e);
        }
    }

    public void SendMessage(ActionEvent actionEvent) {
        if (NettyClient.getInstance().isStopped() || !NettyClient.getInstance().isStarted())
        {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.initOwner(stage);
            alert.setTitle("无法完成操作");
            alert.setHeaderText("无法发送信息");
            alert.setContentText("客户端未启动");
            alert.showAndWait();
            return;
        }
        String UserInput = InputMessage.getText();
        NettyClient clientInstance = NettyClient.getInstance();
        try {
            if (clientInstance.CommandRequest(UserInput,clientInstance.getStatus(),clientInstance.getChannel()))
                return;
        } catch (IOException e) {
            SaveStackTrace.saveStackTrace(e);
            StopClient();
        } catch (NettyClient.QuitException e) {
            StopClient();
        }

        NormalProtocol protocol = new NormalProtocol();//开始构造协议
        NormalProtocol.MessageHead head = new NormalProtocol.MessageHead();
        NormalProtocol.MessageBody body = new NormalProtocol.MessageBody();
        head.setVersion(CodeDynamicConfig.getProtocolVersion());
        head.setType("Chat");//类型是聊天数据包
        protocol.setMessageHead(head);
        body.setMessage(UserInput);
        protocol.setMessageBody(body);
        clientInstance.SendData(new Gson().toJson(protocol),clientInstance.getStatus(),clientInstance.getChannel());
    }

    public void onClientShutdown() {
        NettyClientDependsData = null;
        StopClient();
    }

    public String RequestUserToken() {
        return (NettyClientDependsData.getServerInformation() != null)
                ? NettyClientDependsData.getServerInformation().getServerToken()
                : "";
    }

    public void writeUserToken(String userToken) {
        if (NettyClientDependsData.getServerInformation() != null)
        {
            File SavedServerFile = ServerAndKeyManagementUI.getSavedServerFile();
            if (!SavedServerFile.exists() || !SavedServerFile.isFile()
                    || !SavedServerFile.canRead() || SavedServerFile.length() == 0)
            {
                Platform.runLater(() -> {
                    Alert alert = new Alert(Alert.AlertType.NONE);
                    alert.setHeaderText("token写入失败!");
                    alert.setContentText("此问题是由于文件系统异常，请勿在客户端启动后手动操作SavedServers.json!");
                    alert.getButtonTypes().add(ButtonType.OK);
                    alert.initOwner(stage);
                    alert.show();
                });
                return;
            }
            SavedServerFileLayout layout;
            try {
                layout = new Gson().fromJson(
                        FileUtils.readFileToString(SavedServerFile,StandardCharsets.UTF_8),
                        SavedServerFileLayout.class
                        );
                if (layout.getServerInformation() == null ||
                        layout.getVersion() != CodeDynamicConfig.SavedServerFileVersion)
                {
                    Platform.runLater(() -> {
                        Alert alert = new Alert(Alert.AlertType.NONE);
                        alert.setHeaderText("token写入失败!");
                        alert.setContentText("此问题是由于文件系统异常，请勿在客户端启动后手动操作SavedServers.json!");
                        alert.getButtonTypes().add(ButtonType.OK);
                        alert.initOwner(stage);
                        alert.show();
                    });
                    return;
                }
                for (var bean : layout.getServerInformation())
                {
                    if (bean.getServerAddress().equals(NettyClientDependsData.getServerInformation().getServerAddress()) &&
                            NettyClientDependsData.getServerInformation().getServerPort() == bean.getServerPort())
                    {
                        bean.setServerToken(userToken);
                        FileUtils.writeStringToFile(SavedServerFile,new Gson().toJson(layout),
                                StandardCharsets.UTF_8);
                    }
                }
            }
            catch (IOException | JsonSyntaxException e) {
                Platform.runLater(() -> {
                    Alert alert = new Alert(Alert.AlertType.NONE);
                    alert.setHeaderText("token写入失败!");
                    alert.setContentText("此问题是由于文件系统异常，请勿在客户端启动后手动操作SavedServers.json!");
                    alert.getButtonTypes().add(ButtonType.OK);
                    alert.initOwner(stage);
                    alert.show();
                });
            }
        }
    }
}
