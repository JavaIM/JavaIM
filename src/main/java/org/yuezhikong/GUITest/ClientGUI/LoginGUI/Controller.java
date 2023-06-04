package org.yuezhikong.GUITest.ClientGUI.LoginGUI;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Alert;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextFormatter;
import javafx.scene.layout.VBox;
import org.yuezhikong.GUITest.GUIClient;
import org.yuezhikong.utils.CustomVar;

import java.net.URL;
import java.util.ResourceBundle;

public class Controller implements Initializable {
    @FXML
    public VBox root;
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
        root.getScene().getWindow().hide();
    }
}
