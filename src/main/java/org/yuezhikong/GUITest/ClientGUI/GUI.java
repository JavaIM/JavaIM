package org.yuezhikong.GUITest.ClientGUI;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.util.Objects;

public class GUI extends Application {
    private static Stage stage;
    @Override
    public void start(Stage stage) throws Exception {
        GUI.stage = stage;
        Parent root = FXMLLoader.load(Objects.requireNonNull(getClass().getClassLoader().getResource("GUIs/Client.fxml")));
        stage.setTitle("客户端界面");
        stage.setScene(new Scene(root));
        stage.show();
    }

    public static Stage getStage() {
        return stage;
    }
}
