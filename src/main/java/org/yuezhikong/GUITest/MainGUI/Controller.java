package org.yuezhikong.GUITest.MainGUI;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.stage.Stage;
import org.yuezhikong.CodeDynamicConfig;
import org.yuezhikong.utils.SaveStackTrace;

public class Controller {
    @FXML
    public Button ServerMode;
    @FXML
    public Button ClientMode;

    @FXML
    public void OnUserClickStartServer(ActionEvent actionEvent) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("作者的话");
        alert.setHeaderText("这个部分正在开发中！");
        if (!CodeDynamicConfig.GetDebugMode()) {
            alert.setContentText("前面的区域以后再来探索吧!");

            alert.showAndWait();
        }
        else
        {
            alert.setContentText("导致存在一些bug");
            alert.showAndWait();

            alert.setAlertType(Alert.AlertType.WARNING);
            alert.setHeaderText("最后一次警告");
            alert.setContentText("此部分仍在开发中，可能存在问题！");
            alert.showAndWait();
            org.yuezhikong.GUITest.ServerGUI.GUI ServerGUI = new org.yuezhikong.GUITest.ServerGUI.GUI();
            Stage StageOfServerGUI=new Stage();
            try {
                ServerGUI.start(StageOfServerGUI);
                GUI.getStage().hide();
                GUI.getStage().close();
            } catch (Exception e) {
                SaveStackTrace.saveStackTrace(e);
            }
        }
    }

    @FXML
    public void OnUserClickStartClient(ActionEvent actionEvent) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("作者的话");
        alert.setHeaderText("这个部分正在开发中！");
        if (!CodeDynamicConfig.GetDebugMode()) {
            alert.setContentText("前面的区域以后再来探索吧!");

            alert.showAndWait();
        }
        else
        {
            alert.setContentText("导致存在一些bug");
            alert.showAndWait();

            alert.setAlertType(Alert.AlertType.WARNING);
            alert.setHeaderText("最后一次警告");
            alert.setContentText("此部分仍在开发中，可能存在问题！");
            alert.showAndWait();
            org.yuezhikong.GUITest.ClientGUI.GUI ClientGUI = new org.yuezhikong.GUITest.ClientGUI.GUI();
            Stage StageOfClientGUI=new Stage();
            try {
                ClientGUI.start(StageOfClientGUI);
                GUI.getStage().hide();
                GUI.getStage().close();
            } catch (Exception e) {
                SaveStackTrace.saveStackTrace(e);
            }
        }
    }

    @FXML
    public void AboutThisProgram(ActionEvent actionEvent) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("关于 JavaIM");
        alert.setHeaderText("JavaIM是主要由QiLechan与AlexLiuDev233开发的一个由java实现的聊天室");
        alert.setContentText("此程序是根据GNU General Public License v3.0开源的自由程序（开源软件）");
        alert.showAndWait();
    }
}
