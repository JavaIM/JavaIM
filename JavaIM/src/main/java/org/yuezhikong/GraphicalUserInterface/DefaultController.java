package org.yuezhikong.GraphicalUserInterface;

import javafx.event.ActionEvent;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.image.ImageView;
import javafx.stage.Stage;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.yuezhikong.CrashReport;
import org.yuezhikong.newClient.NettyClient;
import org.yuezhikong.newServer.NettyServer;

import java.awt.*;
import java.io.IOException;
import java.util.Objects;
import java.util.Optional;

public class DefaultController {
    //全局共享SystemTray Icon和PopupMenu
    public static TrayIcon SystemTrayIcon;
    protected static PopupMenu SystemTrayMenu;
    //全局共享stage
    protected static Stage stage;

    public static Stage getStage() {
        return stage;
    }

    public void AboutJavaIM() {
        Alert alert = new Alert(Alert.AlertType.NONE);
        alert.initOwner(stage);
        alert.setTitle("关于JavaIM");
        alert.setGraphic(new ImageView(Objects.requireNonNull(
                getClass().getResource("/images/logo.png")).toString()));
        alert.setContentText("""
                    JavaIM是根据GNU General Public License v3.0开源的自由程序（开源软件)
                    主仓库位于：https://github.com/JavaIM/JavaIM
                    主要开发者名单：
                    QiLechan（柒楽)
                    AlexLiuDev233 （阿白)""");
        alert.getButtonTypes().add(ButtonType.OK);
        alert.showAndWait();
    }

    public static void StopClient()
    {
        if (!NettyClient.getInstance().isStopped() && NettyClient.getInstance().isStarted())
            NettyClient.getInstance().stop();
    }
    public static void StopServer()
    {
        if (NettyServer.getNettyNetwork().ServerStartStatus())
            NettyServer.getNettyNetwork().StopNettyChatRoom();
    }
    public void StopAllTaskAndExitProgram(ActionEvent actionEvent) {
        StopClient();
        StopServer();
        System.exit(0);
    }

    /**
     * 切换到指定fxml页
     * @param ResourcePath fxml resource路径
     * @param TitleName 新的标题名称
     * @throws NullPointerException ResourcePath是null
     */
    public void SwitchToPage(@NotNull String ResourcePath, @NotNull String TitleName)
    {
        if (NettyServer.getNettyNetwork().ServerStartStatus())
        {
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.setHeaderText("服务端仍在运行中");
            alert.setContentText("""
                    执行此操作将会导致服务端被退出
                    您仍要继续吗？""");
            Optional<ButtonType> type = alert.showAndWait();
            if (type.isPresent())
            {
                if (!(type.get().equals(ButtonType.OK)))
                {
                    return;
                }
            }
            else
            {
                return;
            }
            StopServer();
        }
        if (NettyClient.getInstance().isStarted() && !NettyClient.getInstance().isStopped())
        {
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.setHeaderText("客户端仍在运行中");
            alert.setContentText("""
                    执行此操作将会导致客户端被退出
                    您仍要继续吗？""");
            Optional<ButtonType> type = alert.showAndWait();
            if (type.isPresent())
            {
                if (!(type.get().equals(ButtonType.OK)))
                    return;
            }
            else
            {
                return;
            }
            StopClient();
        }
        try {
            Parent root = FXMLLoader.load(Objects.requireNonNull(
                    getClass().getResource(ResourcePath)));
            Scene scene = new Scene(root);
            stage.setScene(scene);
            //更新标题
            stage.setTitle(TitleName);
        } catch (IOException e) {
            CrashReport.failedException(e);
        }
    }
    public void UseServerAndKeyManagement(ActionEvent actionEvent) {
        SwitchToPage("/GUI/ServerAndKeyManagement.fxml","JavaIM --- 公钥管理器");
    }

    public void StartByServerMode(ActionEvent actionEvent) {
        SwitchToPage("/GUI/Server.fxml","JavaIM --- 服务端");
    }

    public void StartByClientMode(ActionEvent actionEvent) {
        SwitchToPage("/GUI/Client.fxml","JavaIM --- 客户端");
    }

    /**
     * 写入聊天信息
     * @param msg 写入的消息
     * @apiNote 请通过继承修改此方法，否则默认直接发生异常
     */
    @ApiStatus.OverrideOnly
    public void WriteChatMessage(String msg) {
        throw new UnsupportedOperationException("This controller is not support this method!");
    }

    /**
     * 写入系统日志
     * @param msg 写入的日志
     * @apiNote 请通过继承修改此方法，否则默认直接发生异常
     */
    @ApiStatus.OverrideOnly
    public void WriteSystemLog(String msg) {
        throw new UnsupportedOperationException("This controller is not support this method!");
    }
}
