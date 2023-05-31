package org.yuezhikong.GUITest.ServerGUI;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Alert;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextFormatter;
import javafx.scene.layout.VBox;
import org.yuezhikong.GUITest.GUIServer;
import org.yuezhikong.Server.api.ServerAPI;
import org.yuezhikong.utils.CustomVar;
import org.yuezhikong.utils.Logger;

import java.net.URL;
import java.util.ResourceBundle;

import static org.yuezhikong.Server.Commands.RequestCommand.CommandRequest;

public class Controller implements Initializable {
    private boolean StartedServer = false;
    private Logger logger;
    private GUIServer Server;
    @FXML
    public VBox root;
    @FXML
    public TextArea ChatLog;
    @FXML
    public TextArea MessageInput;
    @FXML
    public TextArea Log;
    @FXML
    public TextArea Command;
    @FXML
    public TextArea PortInput;

    /**
     * 将消息打印到服务端日志
     * @param msg 消息
     */
    public void WriteToServerLog(String msg)
    {
        Platform.runLater(()->
                {
                    Log.appendText(msg+"\n");
                    Log.positionCaret(Log.getText().length());
                }
        );
    }

    /**
     * 将消息打印到聊天信息
     * @param msg 消息
     */
    public void WriteToChatLog(String msg)
    {
        Platform.runLater(()->
                {
                    ChatLog.appendText(msg+"\n");
                    ChatLog.positionCaret(ChatLog.getText().length());
                }
        );
    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        logger = new Logger(true,false,Controller.this,null);
        MessageInput.setTextFormatter(new TextFormatter<String>(change -> {
            String newText = change.getText().replaceAll("\n","");
            newText = newText.replaceAll("\r","");
            change.setText(newText);
            return change;
        }));
        Command.setTextFormatter(new TextFormatter<String>(change -> {
            String newText = change.getText().replaceAll("\n","");
            newText = newText.replaceAll("\r","");
            change.setText(newText);
            return change;
        }));
        PortInput.setTextFormatter(new TextFormatter<String>(change -> {
            String newText = change.getText().replaceAll("\n","");
            newText = newText.replaceAll("\r","");
            change.setText(newText);
            return change;
        }));
        root.getScene().getWindow().setOnCloseRequest(windowEvent -> {
            if (StartedServer) {
                CloseServer();
            }
            System.exit(0);
        });
    }

    public void InformationOfThisWindow() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("关于 此窗口");
        alert.setHeaderText("此窗口是 JavaIM 软件的一部分");
        alert.setContentText("它是此软件的“服务端”的控制界面");
        alert.showAndWait();
    }

    public void SendMessage() {
        if (StartedServer)
        {
            if (MessageInput.getText().isEmpty())
            {
                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setTitle("提示");
                alert.setHeaderText("你不能这样做！");
                alert.setContentText("你不能发送空消息");
                alert.showAndWait();
                return;
            }
            String msg = "<Server> " + MessageInput.getText();
            new Thread()
            {
                @Override
                public void run() {
                    this.setName("User Request Process Thread");
                    ServerAPI.SendMessageToAllClient(msg,Server);
                    logger.ChatMsg(msg);
                }
            }.start();
        }
        else
        {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("错误");
            alert.setHeaderText("无法发出信息");
            alert.setContentText("没有服务端被启动，请先启动一个服务端");
            alert.showAndWait();
        }
    }

    public void SendCommand() {
        if (StartedServer) {
            String Command = this.Command.getText();
            if (Command.isEmpty())
            {
                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setTitle("提示");
                alert.setHeaderText("你不能这样做！");
                alert.setContentText("你不能执行空命令");
                alert.showAndWait();
                return;
            }
            new Thread() {
                @Override
                public void run() {
                    this.setName("User Request Process Thread");
                    CustomVar.Command CommandRequestReturn = ServerAPI.CommandFormat(Command);
                    String command = CommandRequestReturn.Command();
                    logger.info("控制台 执行了命令 /" + command);
                    CommandRequest("/" + command, CommandRequestReturn.argv(), null);
                }
            }.start();
        } else
        {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("错误");
            alert.setHeaderText("无法发出命令");
            alert.setContentText("没有服务端被启动，请先启动一个服务端");
            alert.showAndWait();
        }
    }

    public void StartServer() {
        int port;
        try
        {
            port = Integer.parseInt(PortInput.getText());
        } catch (NumberFormatException e)
        {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("错误");
            alert.setHeaderText("格式不正确错误");
            alert.setContentText("端口必须是一个数字哦");
            alert.showAndWait();
            return;
        }
        if (port > 65535 || port < 1)
        {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("错误");
            alert.setHeaderText("格式不正确错误");
            alert.setContentText("端口必须在1-65535范围内");
            alert.showAndWait();
        }
        else if (StartedServer)
        {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("错误");
            alert.setHeaderText("无法启动服务端");
            alert.setContentText("已经启动了一个服务端！不可再次启动服务端");
            alert.showAndWait();
        }
        else
        {
            new Thread() {
                @Override
                public void run() {
                    this.setName("Server Thread");
                    GUIServer.SetTempServerGUI(Controller.this);
                    Server = new GUIServer(port);
                    StartedServer = true;
                }
            }.start();
        }
    }

    public void CloseServer() {
        if (!StartedServer)
        {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("警告");
            alert.setHeaderText("没有启动的服务端");
            alert.setContentText("没有启动的服务端，无法关闭服务端");
            alert.showAndWait();
        }
        else {
            CustomVar.Command command = ServerAPI.CommandFormat("/quit");
            CommandRequest(command.Command(), command.argv(), null);
            StartedServer = false;
        }
    }
}
