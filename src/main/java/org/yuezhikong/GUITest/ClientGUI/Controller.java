/*
 * Simplified Chinese (简体中文)
 *
 * 版权所有 (C) 2023 QiLechan <qilechan@outlook.com> 和本程序的贡献者
 *
 * 本程序是自由软件：你可以再分发之和/或依照由自由软件基金会发布的 GNU 通用公共许可证修改之，无论是版本 3 许可证，还是 3 任何以后版都可以。
 * 发布该程序是希望它能有用，但是并无保障;甚至连可销售和符合某个特定的目的都不保证。请参看 GNU 通用公共许可证，了解详情。
 * 你应该随程序获得一份 GNU 通用公共许可证的副本。如果没有，请看 <https://www.gnu.org/licenses/>。
 * English (英语)
 *
 * Copyright (C) 2023 QiLechan <qilechan@outlook.com> and contributors to this program
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or 3 any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program. If not, see <https://www.gnu.org/licenses/>.
 */
package org.yuezhikong.GUITest.ClientGUI;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextFormatter;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.yuezhikong.GUITest.GUIClient;
import org.yuezhikong.utils.SaveStackTrace;

import java.io.IOException;
import java.net.URL;
import java.util.Optional;
import java.util.ResourceBundle;

public class Controller implements Initializable {
    @FXML
    public TextArea Log;
    @FXML
    public TextArea MessageArea;
    @FXML
    public TextArea MessageInput;
    @FXML
    public Text ConnectionStatus;
    @FXML
    public TextArea IPAddress;
    @FXML
    public TextArea Port;
    private Stage LoginStage;
    private boolean Connected = false;
    private GUIClient client;

    /**
     * 将消息打印至日志
     * @param msg 消息
     */
    public void WriteToLog(@NotNull @Nls String msg)
    {
        Platform.runLater(()->
                {
                    Log.appendText(msg+"\n");
                    Log.positionCaret(Log.getText().length());
                }
        );
    }
    /**
     * 将消息打印至聊天区域
     * @param msg 消息
     */
    public void WriteToChatArea(@NotNull @Nls String msg)
    {
        Platform.runLater(()->
                {
                    MessageArea.appendText(msg+"\n");
                    MessageArea.positionCaret(MessageArea.getText().length());
                }
        );
    }
    @FXML
    public void InformationOfThisWindow(ActionEvent actionEvent) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("关于 此窗口");
        alert.setHeaderText("此窗口是 JavaIM 软件的一部分");
        alert.setContentText("它是此软件的“客户端”的控制界面");
        alert.showAndWait();
    }

    @FXML
    public void SendMessage(ActionEvent actionEvent) {
        if (Connected)
        {
            String inputMessage = MessageInput.getText();
            if (inputMessage.isEmpty())
            {
                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setTitle("提示");
                alert.setHeaderText("你不能这样做！");
                alert.setContentText("你不能发送空消息");
                alert.showAndWait();
                return;
            }
            new Thread()
            {
                @Override
                public void run() {
                    this.setName("User Request Process Thread");
                    try {
                        if (client.SendMessageToServer(inputMessage))
                        {
                            client.getLogger().info("再见~");
                            ExitSystem(0);
                        }
                    } catch (IOException e) {
                        if (!"Connection reset by peer".equals(e.getMessage()) && !"Connection reset".equals(e.getMessage())) {
                            client.getLogger().warning("发生I/O错误");
                            SaveStackTrace.saveStackTrace(e);
                        }
                        else
                        {
                            client.getLogger().info("连接早已被关闭...");
                            ExitSystem(0);
                        }
                    }
                }
            }.start();
        }
        else
        {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("提示");
            alert.setHeaderText("你不能这样做！");
            alert.setContentText("目前，客户端没有被启动");
            alert.showAndWait();
        }
    }

    @FXML
    public void Connect(ActionEvent actionEvent) {
        String IP = IPAddress.getText();
        int port;
        try
        {
            port = Integer.parseInt(Port.getText());
        } catch (NumberFormatException e)
        {
            Alert ClientFailedAlert = new Alert(Alert.AlertType.ERROR);
            ClientFailedAlert.setHeaderText("客户端启动失败");
            ClientFailedAlert.setContentText("端口不是一个数字");
            ClientFailedAlert.showAndWait();
            return;
        }
        if (port > 65535 || port < 1)
        {
            Alert ClientFailedAlert = new Alert(Alert.AlertType.ERROR);
            ClientFailedAlert.setHeaderText("客户端启动失败");
            ClientFailedAlert.setContentText("端口不在1-65535范围内");
            ClientFailedAlert.showAndWait();
        }
        else if (Connected)
        {
            Alert ClientFailedAlert = new Alert(Alert.AlertType.ERROR);
            ClientFailedAlert.setHeaderText("客户端启动失败");
            ClientFailedAlert.setContentText("已经启动了一个客户端了");
            ClientFailedAlert.showAndWait();
        }
        else {
            new Thread() {
                @Override
                public void run() {
                    this.setName("Client Thread");
                    GUIClient.SetTempClientGUI(Controller.this);
                    client = new GUIClient(IP, port);
                    Connected = true;
                    Platform.runLater(()-> ConnectionStatus.setText("已连接到服务器\nip为："+IPAddress.getText()+"\n端口为："+ port));
                }
            }.start();
        }
    }

    @FXML
    public void Disconnect(ActionEvent actionEvent)
    {
        if (Connected) {
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.setTitle("警告");
            alert.setHeaderText("此操作将会退出此程序");
            alert.setContentText("您确定要这么做吗？");
            Optional<ButtonType> result = alert.showAndWait();
            if (result.isPresent()) {
                if (result.get().equals(ButtonType.YES)) {
                    new Thread()
                    {
                        @Override
                        public void run() {
                            this.setName("User Request Process Thread");
                            try {
                                if (client.SendMessageToServer(".quit"))
                                {
                                    client.getLogger().info("再见~");
                                    ExitSystem(0);
                                }
                            } catch (IOException e) {
                                if (!"Connection reset by peer".equals(e.getMessage()) && !"Connection reset".equals(e.getMessage())) {
                                    client.getLogger().warning("发生I/O错误");
                                    SaveStackTrace.saveStackTrace(e);
                                }
                                else
                                {
                                    client.getLogger().info("连接早已被关闭...");
                                    ExitSystem(0);
                                }
                            }
                        }
                    }.start();
                }
            }
        }
        else
        {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("提示");
            alert.setHeaderText("你不能这样做！");
            alert.setContentText("目前，客户端没有被启动");
            alert.showAndWait();
        }
    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        MessageInput.setTextFormatter(new TextFormatter<String>(change -> {
            String newText = change.getText().replaceAll("\n","");
            newText = newText.replaceAll("\r","");
            change.setText(newText);
            return change;
        }));
        IPAddress.setTextFormatter(new TextFormatter<String>(change -> {
            String newText = change.getText().replaceAll("\n","");
            newText = newText.replaceAll("\r","");
            change.setText(newText);
            return change;
        }));
        Port.setTextFormatter(new TextFormatter<String>(change -> {
            String newText = change.getText().replaceAll("\n","");
            newText = newText.replaceAll("\r","");
            change.setText(newText);
            return change;
        }));
        GUI.getStage().setOnCloseRequest(windowEvent -> {
            if (Connected)
            {
                client.quit();
            }
            System.exit(0);
        });
        LoginStage = new Stage();
        org.yuezhikong.GUITest.ClientGUI.LoginGUI.GUI LoginGUI = new org.yuezhikong.GUITest.ClientGUI.LoginGUI.GUI();
        try {
            LoginGUI.start(LoginStage);
        } catch (Exception e) {
            SaveStackTrace.saveStackTrace(e);
        }
    }
    public void ClientStartFailedbyServerPublicKeyLack()
    {
        Platform.runLater(()->{
            Connected = false;
            Alert ClientFailedAlert = new Alert(Alert.AlertType.ERROR);
            ClientFailedAlert.setHeaderText("客户端启动失败");
            ClientFailedAlert.setContentText("""
                    在运行目录下未找到ServerPublicKey.txt
                    此文件为服务端公钥文件，用于保证通信安全
                    因为未找到此文件，客户端已经停止工作""");
            ClientFailedAlert.showAndWait();
        });
    }
    public void ExitSystem(int code)
    {
        Platform.runLater(()->{
            Alert ClientFailedAlert = new Alert(Alert.AlertType.INFORMATION);
            ClientFailedAlert.setHeaderText("似乎客户端已经被退出了");
            ClientFailedAlert.setContentText("按下任意按钮退出程序");
            ClientFailedAlert.showAndWait();
            System.exit(code);
        });
    }

    public void GetUserNameAndPassword() {
        LoginStage.showAndWait();
    }
}
