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
package org.yuezhikong.GUITest.MainGUI;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.stage.Stage;
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
        alert.setHeaderText("此功能最新加入，可能存在问题");
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

    @FXML
    public void OnUserClickStartClient(ActionEvent actionEvent) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("作者的话");
        alert.setHeaderText("此功能最新加入，可能存在问题");
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

    @FXML
    public void AboutThisProgram(ActionEvent actionEvent) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("关于 JavaIM");
        alert.setHeaderText("JavaIM是主要由QiLechan与AlexLiuDev233开发的一个由java实现的聊天室");
        alert.setContentText("此程序是根据GNU General Public License v3.0开源的自由程序（开源软件）");
        alert.showAndWait();
    }
}
