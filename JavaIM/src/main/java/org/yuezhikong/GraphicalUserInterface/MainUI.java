package org.yuezhikong.GraphicalUserInterface;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import org.yuezhikong.utils.Logger;

import java.awt.*;
import java.net.URL;
import java.util.Objects;
import java.util.ResourceBundle;

public class MainUI extends DefaultController implements Initializable {
    /**
     * JavaFX FXML被加载时执行的代码
     */
    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {

    }

    public static class UIInit extends Application
    {
        private void GUIShutdownJavaIM()
        {
            DefaultController.StopClient();
            DefaultController.StopServer();
            if (ClientUI.TimerThreadPool != null)
            {
                if (!ClientUI.TimerThreadPool.isShutdown())
                    ClientUI.TimerThreadPool.shutdownNow();
                ClientUI.TimerThreadPool = null;
            }

            //关闭系统托盘
            if (!SystemTray.isSupported())
            {
                Logger.logger_root.info("当前程序所在的操作系统不支持托盘图标，跳过System Tray Clean-up");
            }
            else
            {
                SystemTray tray = SystemTray.getSystemTray();
                for (TrayIcon icon : tray.getTrayIcons())
                {
                    tray.remove(icon);
                }
            }
        }
        @Override
        public void start(Stage stage) throws Exception {
            //获取程序图标
            URL ProgramIcon = Objects.requireNonNull(
                    getClass().getResource("/images/logo.png"));
            //主窗口FXML加载
            Parent root = FXMLLoader.load(Objects.requireNonNull(
                    getClass().getResource("/GUI/Main.fxml")));
            Scene scene = new Scene(root);
            DefaultController.stage = stage;
            stage.setScene(scene);
            //按下“X”的处理
            stage.setOnCloseRequest(windowEvent -> GUIShutdownJavaIM());
            //托盘图标
            if (!SystemTray.isSupported())
            {
                Logger.logger_root.info("当前程序所在的操作系统不支持托盘图标，已停用此功能");
            }
            else
            {
                //初始化托盘图标、右键菜单
                SystemTray tray = SystemTray.getSystemTray();
                DefaultController.SystemTrayMenu = new PopupMenu();
                DefaultController.SystemTrayIcon = new TrayIcon(Toolkit.getDefaultToolkit().getImage(ProgramIcon),"JavaIM",SystemTrayMenu);
                SystemTrayIcon.setImageAutoSize(true);
                SystemTrayIcon.addActionListener((actionEvent) -> Platform.runLater(() -> {
                    if(stage.isIconified())
                        stage.setIconified(false);
                    if(!stage.isShowing())
                        stage.show();
                    stage.toFront();
                }));
                tray.add(SystemTrayIcon);
                //设置右键菜单
                MenuItem ExitProgram = new MenuItem("退出 JavaIM");
                ExitProgram.addActionListener((actionEvent) -> {
                    Platform.runLater(stage::close);//JavaFX Application Thread关闭窗口，AWT Queue不能操作JavaFX窗口
                    GUIShutdownJavaIM();
                });

                DefaultController.SystemTrayMenu.add(ExitProgram);
            }
            //设定最小尺寸
            stage.setMinHeight(437);
            stage.setMinWidth(614);
            //设置图标等
            stage.getIcons().clear();
            stage.getIcons().add(new Image(ProgramIcon.toString()));
            stage.setTitle("JavaIM --- 主界面");
            stage.show();
        }
    }

}
