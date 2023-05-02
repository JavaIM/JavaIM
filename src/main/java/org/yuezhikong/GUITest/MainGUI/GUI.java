package org.yuezhikong.GUITest.MainGUI;

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
        Parent root = FXMLLoader.load(Objects.requireNonNull(getClass().getClassLoader().getResource("GUIs/Main.fxml")));
        stage.setTitle("主界面");
        stage.setScene(new Scene(root));
        stage.setMinHeight(437);
        stage.setHeight(437);
        stage.setWidth(654);
        stage.setMinWidth(654);
        stage.show();
        GUI.stage = stage;
    }

    /**
     * 获取Main窗口的Stage
      * @return Stage
     */
    public static Stage getStage() {
        return stage;
    }
}
