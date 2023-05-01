package org.yuezhikong.GUITest.ServerGUI;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.util.Objects;

public class GUI extends Application {
    @Override
    public void start(Stage stage) throws Exception {
        Parent root = FXMLLoader.load(Objects.requireNonNull(getClass().getClassLoader().getResource("GUIs/Server.fxml")));
        stage.setTitle("服务端界面");
        stage.setScene(new Scene(root));
        stage.setMinWidth(726);
        stage.setWidth(726);
        stage.setMinHeight(492);
        stage.setHeight(492);
        stage.show();
    }
}
