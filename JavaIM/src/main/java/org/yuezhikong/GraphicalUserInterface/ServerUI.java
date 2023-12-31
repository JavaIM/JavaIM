package org.yuezhikong.GraphicalUserInterface;

import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.*;
import javafx.scene.input.MouseButton;
import org.apache.commons.io.FileUtils;
import org.yuezhikong.GraphicalUserInterface.Dialogs.PortInputDialog;
import org.yuezhikong.newServer.NettyServer;
import org.yuezhikong.newServer.ServerTools;
import org.yuezhikong.utils.Logger;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.ResourceBundle;

public class ServerUI extends DefaultController implements Initializable {


    static class GraphicalUserManagement extends ContextMenu
    {
        private final MenuItem KickUser;
        private final MenuItem TellUser;
        private static GraphicalUserManagement Instance;

        public static GraphicalUserManagement getInstance() {
            if (Instance == null)
            {
                Instance = new GraphicalUserManagement();
            }
            return Instance;
        }
        private GraphicalUserManagement()
        {
            KickUser = new MenuItem("踢出用户");
            TellUser = new MenuItem("私聊用户");
            getItems().addAll(KickUser,TellUser);
        }

        public MenuItem getKickUser() {
            return KickUser;
        }

        public MenuItem getTellUser() {
            return TellUser;
        }
    }

    public TextField CommandInput;
    public TextField MessageInput;
    public TextArea SystemLog;
    public TextArea ChatMessage;
    public ListView<String> UserList;

    private String UISelectUserName;

    private NettyServer ServerInstance;

    @Override
    public void WriteChatMessage(String msg) {
        //如果系统支持SystemTray，则显示信息(Windows7 气泡、Windows 10 通知等）
        if (SystemTrayIcon != null)
        {
            SystemTrayIcon.displayMessage("JavaIM 服务端",msg, TrayIcon.MessageType.INFO);
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
        ChatMessage.textProperty().addListener(
                (observableValue, oldValue, newValue) -> ChatMessage.setScrollTop(Double.MAX_VALUE)
        );
        SystemLog.textProperty().addListener(
                (observableValue, oldValue, newValue) -> SystemLog.setScrollTop(Double.MAX_VALUE)
        );

        GraphicalUserManagement.getInstance().getKickUser().setOnAction(actionEvent -> {
            if (ServerInstance != null)
                ServerInstance.ServerCommandSend("/kick "+UISelectUserName);
        });
        GraphicalUserManagement.getInstance().getTellUser().setOnAction(actionEvent -> {
            if (ServerInstance != null)
            {
                TextInputDialog dialog = new TextInputDialog();
                dialog.initOwner(stage);
                dialog.setTitle("JavaIM --- 发起私聊");
                dialog.setHeaderText("您必须填写此参数，否则无法发送消息");
                dialog.setContentText("请输入发送的聊天信息：");
                Optional<String> InputMessage = dialog.showAndWait();
                if (InputMessage.isEmpty() || InputMessage.get().isEmpty())
                {
                    Alert alert = new Alert(Alert.AlertType.NONE);
                    alert.initOwner(stage);
                    alert.getButtonTypes().add(ButtonType.OK);
                    alert.setTitle("JavaIM --- 发送失败");
                    alert.setTitle("您未填写此消息的必填字段，无法发送消息");
                    alert.showAndWait();
                }
                else
                    ServerInstance.ServerCommandSend("/tell "+UISelectUserName+" "+InputMessage.get());
            }
        });
        UserList.setOnMouseClicked(mouseEvent -> {
            if (mouseEvent.getButton().equals(MouseButton.SECONDARY))
            {
                if (UserList.getSelectionModel().getSelectedItem() == null)
                {
                    return;
                }
                UISelectUserName = UserList.getSelectionModel().getSelectedItem();
                Node node = mouseEvent.getPickResult().getIntersectedNode();
                GraphicalUserManagement.getInstance().show(node, javafx.geometry.Side.BOTTOM, 0, 0);
            }
        });
    }

    public void SendCommand(ActionEvent actionEvent) {
        if (ServerInstance == null)
            return;
        ServerInstance.ServerCommandSend("/"+CommandInput.getText());
        CommandInput.clear();
    }

    public void SendMessage(ActionEvent actionEvent) {
        if (ServerInstance == null)
            return;
        ServerInstance.ServerChatMessageSend(MessageInput.getText());
        MessageInput.clear();
    }

    public void StartorCloseServer(ActionEvent actionEvent) {
        if (ServerTools.getServerInstance() == null)
        {
            int ServerPort;
            PortInputDialog portInputDialog = new PortInputDialog();
            portInputDialog.initOwner(stage);
            portInputDialog.setTitle("JavaIM --- 启动服务器");
            portInputDialog.setHeaderText("如果要启动服务器，请提供以下信息");
            portInputDialog.setContentText("请输入服务器端口：");
            Optional<String> PortOfUserInput = portInputDialog.showAndWait();
            if (PortOfUserInput.isPresent())
            {
                ServerPort = Integer.parseInt(PortOfUserInput.get());
            }
            else
            {
                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setHeaderText("已取消启动服务器");
                alert.setContentText("因为您已经取消了输入必须信息");
                alert.showAndWait();
                return;
            }

            Logger ServerLogger = new Logger(this);
            if (NettyServer.getNettyNetwork().ServerStartStatus())
                throw new IllegalStateException("The Netty Server is always start!");
            NettyServer.getNettyNetwork().RSA_KeyAutogenerate("./ServerRSAKey/Public.txt", "./ServerRSAKey/Private.txt", ServerLogger);
            NettyServer.getNettyNetwork().setLogger(ServerLogger);
            NettyServer.getNettyNetwork().AddLoginRecall((user) -> UpdateUser(true,user.getUserName()));
            NettyServer.getNettyNetwork().AddDisconnectRecall((user) -> {
                if (user.isUserLogined())
                {
                    UpdateUser(false,user.getUserName());
                }
            });
            ServerInstance = NettyServer.getNettyNetwork();
            new Thread(new ThreadGroup(Thread.currentThread().getThreadGroup(), "Server Group"),"Server Thread")
            {
                @Override
                public void run() {
                    try {
                        NettyServer.getNettyNetwork().StartChatRoomServerForNetty(ServerPort, FileUtils.readFileToString(new File("./ServerRSAKey/Private.txt"), StandardCharsets.UTF_8));
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
            }.start();
        }
        else
        {
            NettyServer instance = ServerInstance;
            ServerInstance = null;
            instance.StopNettyChatRoom();
            ObservableList<String> ListOfUser = UserList.getItems();
            ListOfUser.clear();
            UserList.setItems(ListOfUser);
        }
    }

    /**
     * 更新GUI用户表
     * @param isLogin 是否处于登录状态
     * @param userName 用户名
     */
    public void UpdateUser(boolean isLogin, String userName) {
        Platform.runLater(() -> {
            ObservableList<String> ListOfUser = UserList.getItems();
            if (isLogin)
            {
                ListOfUser.add(userName);
            }
            else {
                ListOfUser.remove(userName);
            }
            UserList.setItems(ListOfUser);
        });
    }

    public void onServerShutdown() {
        ServerInstance = null;

        Platform.runLater(() -> {
            ObservableList<String> ListOfUser = UserList.getItems();
            ListOfUser.clear();
            UserList.setItems(ListOfUser);
        });
    }
}
