package org.yuezhikong.GraphicalUserInterface.Dialogs;

import javafx.collections.FXCollections;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;

import java.util.List;

public class SpinnerDialog extends Dialog<SpinnerDialog.DialogReturn> {
    public record DialogReturn(String SpinnerSelect){}

    /**
     * 初始化Spinner选择对话框
     * @param Select spinner可选择的string列表
     * @param OwnerStage 对话框的父窗口的Stage
     * @param Title 对话框标题
     */
    public SpinnerDialog(List<String> Select, Stage OwnerStage, String Title)
    {
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);

        Label label = new Label("请选择");
        grid.add(label,0,1);

        Spinner<String> userSelect = new Spinner<>();
        userSelect.setEditable(false);
        userSelect.setValueFactory(
                new SpinnerValueFactory.ListSpinnerValueFactory<>(FXCollections.observableArrayList(Select)));
        grid.add(userSelect,0,2,2,1);

        HBox buttonBox = new HBox();
        buttonBox.setAlignment(Pos.CENTER_RIGHT);
        Button submitButton = new Button("提交");
        Button cancelButton = new Button("取消");
        buttonBox.getChildren().addAll(submitButton, cancelButton);
        grid.add(buttonBox, 2, 3, 1, 1);

        getDialogPane().setContent(grid);
        setTitle(Title);
        initOwner(OwnerStage);

        submitButton.setOnAction((actionEvent) -> {
            setResult(new DialogReturn(userSelect.getValue()));
            close();
        });
        cancelButton.setOnAction(t -> {
            setResult(null);
            close();
        });
    }

}
