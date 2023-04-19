package org.yuezhikong.GUITest;

import javafx.application.Application;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.layout.FlowPane;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

public class TestMainGUI extends Application {
    @Override
    public void start(Stage stage) throws Exception {
        stage.setTitle("测试主界面");

        Text Tips = new Text("您可选择使用服务端模式还是客户端模式");
        Tips.setX(30);
        Tips.setY(30);

        Button StartServer = new Button("启动服务端");
        StartServer.setTranslateX(20);
        StartServer.setTranslateY(30);
        StartServer.setOnAction(actionEvent -> {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("作者的话");
            alert.setHeaderText("这个部分正在开发中!");
            alert.setContentText("前面的区域以后再来探索吧!");

            alert.showAndWait();
        });

        TextFlow textFlow = new TextFlow();
        textFlow.getChildren().add(Tips);

        FlowPane pane = new FlowPane();
        pane.getChildren().add(StartServer);

        Group group = new Group(textFlow,pane);
        Scene scene = new Scene(group); //场景
        stage.setScene(scene);
        stage.setMinHeight(300);
        stage.setHeight(300);
        stage.setMinWidth(300);
        stage.setWidth(300);
        stage.initStyle(StageStyle.DECORATED);
        stage.show();
    }
}
