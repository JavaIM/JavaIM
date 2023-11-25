package org.yuezhikong.GraphicalUserInterface;

import cn.hutool.crypto.symmetric.AES;
import com.google.gson.Gson;
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
import org.yuezhikong.CodeDynamicConfig;
import org.yuezhikong.CrashReport;
import org.yuezhikong.NetworkManager;
import org.yuezhikong.newClient.ClientMain;
import org.yuezhikong.newClient.GUIClient;
import org.yuezhikong.newServer.GUIServer;
import org.yuezhikong.newServer.ServerMain;
import org.yuezhikong.newServer.UserData.user;
import org.yuezhikong.utils.Protocol.NormalProtocol;

import java.awt.*;
import java.io.IOException;
import java.lang.reflect.Field;
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
        if (ClientMain.getClient() != null)
        {
            if (ClientMain.getClient().getClientStopStatus())
            {

                try {
                    Field instance = ClientMain.class.getDeclaredField("Instance");
                    instance.setAccessible(true);
                    instance.set(null,null);
                    instance.setAccessible(false);
                } catch (NoSuchFieldException | IllegalAccessException e) {
                    CrashReport.failedException(e);
                }
            }
            if (ClientMain.getClient() instanceof GUIClient)
            {
                ((GUIClient) ClientMain.getClient()).StopClient();
            }
            else {
                try {
                    //反射获取private成员变量
                    Field socket = ClientMain.class.getDeclaredField("clientNetworkData");
                    Field ClientThreadGroup = ClientMain.class.getDeclaredField("ClientThreadGroup");
                    Field recvMessageThread = ClientMain.class.getDeclaredField("recvMessageThread");
                    Field aes = ClientMain.class.getDeclaredField("aes");
                    Field instance = ClientMain.class.getDeclaredField("Instance");
                    //绕过java访问检查
                    socket.setAccessible(true);
                    ClientThreadGroup.setAccessible(true);
                    recvMessageThread.setAccessible(true);
                    aes.setAccessible(true);
                    instance.setAccessible(true);
                    //获取客户端实例
                    ClientMain clientMain = ClientMain.getClient();
                    instance.set(null,null);
                    instance.setAccessible(false);
                    //获取AES与Socket
                    AES Aes = (AES) aes.get(clientMain);
                    NetworkManager.NetworkData Socket = (NetworkManager.NetworkData) socket.get(clientMain);
                    //恢复部分成员变量的java访问检查
                    socket.setAccessible(false);
                    aes.setAccessible(false);
                    //执行发送离开服务器消息
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

                    NetworkManager.WriteDataToRemote(Socket,Aes.encryptBase64(gson.toJson(protocol)));
                    //执行强制关闭socket、线程操作
                    ((Thread) recvMessageThread.get(clientMain)).interrupt();
                    ((ThreadGroup) ClientThreadGroup.get(clientMain)).interrupt();
                    Socket.close();
                    //执行设置logger为过期
                    clientMain.getLogger().OutDate();
                    //执行设置为已退出操作
                    Field ClientStatus = ClientMain.class.getDeclaredField("ClientStatus");
                    ClientStatus.setAccessible(true);
                    ClientStatus.setBoolean(clientMain,true);
                    ClientStatus.setAccessible(false);
                    //恢复java访问检查
                    ClientThreadGroup.setAccessible(false);
                    recvMessageThread.setAccessible(false);

                } catch (NoSuchFieldException | IllegalAccessException | IOException e) {
                    CrashReport.failedException(e);
                }
            }
        }
    }
    public static void StopServer()
    {
        if (ServerMain.getServer() != null) {
            ServerMain.getServer().getLogger().info("正在关闭服务器...");
            ServerMain.getServer().getServerAPI().SendMessageToAllClient("服务器已关闭");
            for (user User : ServerMain.getServer().getUsers()) {
                User.UserDisconnect();
            }
            if (!(ServerMain.getServer() instanceof GUIServer)) {
                try {
                    //提示用户服务器已关闭并踢出
                    ServerMain.getServer().getServerAPI().SendMessageToAllClient("服务器已关闭");
                    for (user User : ServerMain.getServer().getUsers())
                    {
                        User.UserDisconnect();
                    }
                    //强制中止authThread
                    Field authThread = ServerMain.class.getDeclaredField("authThread");
                    authThread.setAccessible(true);
                    ((Thread) authThread.get(ServerMain.getServer())).interrupt();
                    authThread.setAccessible(false);
                    //卸载所有插件
                    ServerMain.getServer().getPluginManager().UnLoadAllPlugin();
                    //设置logger为过期
                    ServerMain.getServer().getLogger().OutDate();
                    //设置为未启动服务端
                    Field ServerStarted = ServerMain.class.getDeclaredField("started");
                    ServerStarted.setAccessible(true);
                    ServerStarted.set(ServerMain.getServer(), false);
                    ServerStarted.setAccessible(false);
                    //获取Socket并关闭
                    Field serverSocket = ServerMain.class.getDeclaredField("ServerTCPNetworkData");
                    serverSocket.setAccessible(true);
                    NetworkManager.NetworkData ServerSocket = (NetworkManager.NetworkData) serverSocket.get(ServerMain.getServer());
                    if (ServerSocket != null)
                    {
                        ServerSocket.close();
                    }
                    serverSocket.setAccessible(false);
                    //获取并移除instance
                    ServerMain server = ServerMain.getServer();
                    Field instance = ServerMain.class.getDeclaredField("server");
                    instance.setAccessible(true);
                    instance.set(null, null);
                    instance.setAccessible(false);
                    //终止ServerGroup中的所有线程
                    Field ServerGroup = ServerMain.class.getDeclaredField("ServerGroup");
                    ServerGroup.setAccessible(true);
                    ThreadGroup group = ((ThreadGroup) ServerGroup.get(server));
                    ServerGroup.setAccessible(false);
                    group.interrupt();
                } catch (NoSuchFieldException | IllegalAccessException | IOException e) {
                    CrashReport.failedException(e);
                }
            }
            else
            {
                ((GUIServer) ServerMain.getServer()).StopServer();
            }
        }
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
        if (ServerMain.getServer() != null)
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
        if (ClientMain.getClient() != null)
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
                {
                    return;
                }
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
        throw new RuntimeException("This Controller is Not A Server Or Client Controller!");
    }

    /**
     * 写入系统日志
     * @param msg 写入的日志
     * @apiNote 请通过继承修改此方法，否则默认直接发生异常
     */
    @ApiStatus.OverrideOnly
    public void WriteSystemLog(String msg) {
        throw new RuntimeException("This Controller is Not A Server Or Client Controller!");
    }
}
