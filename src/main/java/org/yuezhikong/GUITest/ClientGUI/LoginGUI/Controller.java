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
package org.yuezhikong.GUITest.ClientGUI.LoginGUI;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Alert;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextFormatter;
import org.yuezhikong.GUITest.GUIClient;
import org.yuezhikong.utils.CustomVar;

import java.net.URL;
import java.util.ResourceBundle;

public class Controller implements Initializable {
    @FXML
    public TextArea Password;
    @FXML
    public TextArea UserName;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        Password.setTextFormatter(new TextFormatter<String>(change -> {
            String newText = change.getText().replaceAll("\n", "");
            newText = newText.replaceAll("\r", "");
            change.setText(newText);
            return change;
        }));
        UserName.setTextFormatter(new TextFormatter<String>(change -> {
            String newText = change.getText().replaceAll("\n", "");
            newText = newText.replaceAll("\r", "");
            change.setText(newText);
            return change;
        }));
    }
    @FXML
    public void submit(ActionEvent actionEvent) {
        if (UserName.getText().isEmpty() || Password.getText().isEmpty())
        {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setHeaderText("用户名和密码和不得为空");
            alert.setContentText("请先填写信息");
            alert.showAndWait();
            return;
        }
        new Thread() {
            @Override
            public void run() {
                this.setName("User Request Process Thread");
                GUIClient.getInstance().UserNameAndPasswordReCall(new CustomVar.UserAndPassword(UserName.getText(),Password.getText()));
            }
        }.start();
        Password.setText("");
        UserName.setText("");
        GUI.getStage().hide();
    }
}
