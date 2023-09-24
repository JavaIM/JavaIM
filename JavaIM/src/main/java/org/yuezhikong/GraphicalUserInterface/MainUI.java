package org.yuezhikong.GraphicalUserInterface;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;

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
        @Override
        public void start(Stage stage) throws Exception {
            Parent root = FXMLLoader.load(Objects.requireNonNull(
                    getClass().getResource("/GUI/Main.fxml")));
            Scene scene = new Scene(root);
            MainUI.stage = stage;
            stage.setScene(scene);
            stage.getIcons().clear();
            stage.getIcons().add(new Image(Objects.requireNonNull(
                    getClass().getResource("/images/logo.png")).toString()));
            stage.setTitle("JavaIM --- 主界面");
            stage.show();
        }
    }

}
