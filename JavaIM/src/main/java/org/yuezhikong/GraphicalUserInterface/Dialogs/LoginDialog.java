package org.yuezhikong.GraphicalUserInterface.Dialogs;

import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;

public class LoginDialog extends Dialog<LoginDialog.DialogReturn> {

    public record DialogReturn(String UserName,String Password,boolean isLegacyLogin){}
    private final TextField usernameField;
    private final PasswordField passwordField;
    private final CheckBox useLegacyLogin;

    public LoginDialog(Stage OwnerStage) {
        setTitle("JavaIM --- 服务器登录");
        initOwner(OwnerStage);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);

        Label usernameLabel = new Label("用户名:");
        grid.add(usernameLabel, 0, 0);

        usernameField = new TextField();
        grid.add(usernameField, 1, 0);

        Label passwordLabel = new Label("密码:");
        grid.add(passwordLabel, 0, 1);

        passwordField = new PasswordField();
        grid.add(passwordField, 1, 1);

        useLegacyLogin = new CheckBox("使用旧版登录模式");
        grid.add(useLegacyLogin, 1, 2);

        HBox buttonBox = new HBox();
        buttonBox.setAlignment(Pos.CENTER_RIGHT);
        Button loginButton = new Button("登录");
        Button cancelButton = new Button("取消登录");
        buttonBox.getChildren().addAll(loginButton, cancelButton);
        grid.add(buttonBox, 1, 3, 1, 1);

        getDialogPane().setContent(grid);

        loginButton.setOnAction(e -> handleLogin());
        cancelButton.setOnAction(e -> handleCancel());
    }

    private void handleLogin() {
        String username = usernameField.getText();
        String password = passwordField.getText();
        if (username.equals("") || password.equals(""))
        {
            Alert alert = new Alert(Alert.AlertType.NONE);
            alert.setContentText("用户名或密码不得为空");
            alert.getButtonTypes().add(ButtonType.OK);
            alert.initOwner(this.getDialogPane().getScene().getWindow());
            alert.showAndWait();
            return;
        }
        // 返回用户名与密码
        setResult(new DialogReturn(username,password,useLegacyLogin.isSelected()));
        close();
    }

    private void handleCancel() {
        setResult(new DialogReturn(null,null,false));
        close();
    }
}