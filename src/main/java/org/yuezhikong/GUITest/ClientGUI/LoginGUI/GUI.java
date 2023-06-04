package org.yuezhikong.GUITest.ClientGUI.LoginGUI;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.util.Objects;

public class GUI extends Application {
    @Override
    public void start(Stage stage) throws Exception {
        Parent root = FXMLLoader.load(Objects.requireNonNull(getClass().getClassLoader().getResource("GUIs/LoginSystem/Login.fxml")));
        stage.setTitle("登录界面");
        stage.setScene(new Scene(root));
    }
}
