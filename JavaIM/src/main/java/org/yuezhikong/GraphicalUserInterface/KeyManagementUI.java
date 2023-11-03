package org.yuezhikong.GraphicalUserInterface;

import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import org.yuezhikong.utils.SaveStackTrace;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

public class KeyManagementUI extends DefaultController implements Initializable {
    public Spinner<String> SelectServerPublicKey;
    public Spinner<String> SelectEndToEndPublickey;
    public Spinner<String> SelectOperationTypeOfServerPublicKey;
    public Spinner<String> SelectOperationTypeOfEndToEndPublickey;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        //初始化SelectOperationTypeOfEndToEndPublickey
        SelectOperationTypeOfEndToEndPublickey.setEditable(false);
        List<String> EndToEndEncryptionKeyOperationType = new ArrayList<>();
        EndToEndEncryptionKeyOperationType.add("获取详细信息");
        EndToEndEncryptionKeyOperationType.add("删除此公钥");
        SelectOperationTypeOfEndToEndPublickey.setValueFactory
                (new SpinnerValueFactory.ListSpinnerValueFactory<>
                        (FXCollections.observableArrayList(EndToEndEncryptionKeyOperationType)));
        //初始化SelectOperationTypeOfServerPublicKey
        SelectOperationTypeOfServerPublicKey.setEditable(false);
        List<String> ServerPublicKeyOperationType = new ArrayList<>();
        ServerPublicKeyOperationType.add("重命名");
        ServerPublicKeyOperationType.add("设为使用的公钥");
        ServerPublicKeyOperationType.add("删除此公钥");
        SelectOperationTypeOfServerPublicKey.setValueFactory
                (new SpinnerValueFactory.ListSpinnerValueFactory<>
                        (FXCollections.observableArrayList(ServerPublicKeyOperationType)));
        InitPublicKeySpinner();
    }

    public void InitPublicKeySpinner()
    {
        //初始化SelectEndToEndPublickey
        SelectEndToEndPublickey.setEditable(false);
        File Directory = new File("./end-to-end_encryption_saved");
        if (Directory.exists() && Directory.isDirectory())
        {
            if (Directory.list() != null)
            {
                List<String> end_to_end_encryption_key = new ArrayList<>();
                for (String file : Objects.requireNonNull(Directory.list()))
                {
                    if (new File(Directory.getPath()+"/"+file).isFile())
                    {
                        end_to_end_encryption_key.add(file);
                    }
                }
                SelectEndToEndPublickey.setValueFactory
                        (new SpinnerValueFactory.ListSpinnerValueFactory<>
                                (FXCollections.observableArrayList(end_to_end_encryption_key)));
            }
        }
        else if (Directory.isFile())
        {
            Directory.delete();
            InitPublicKeySpinner();
            return;
        }
        else
        {
            Directory.mkdirs();
            InitPublicKeySpinner();
            return;
        }
        //初始化SelectServerPublicKey
        SelectServerPublicKey.setEditable(false);
        Directory = new File("./ClientRSAKey/ServerPublicKeys/");
        if (Directory.exists() && Directory.isDirectory())
        {
            if (Directory.list() != null)
            {
                List<String> ServerPublicKeyList = new ArrayList<>();
                for (String file : Objects.requireNonNull(Directory.list()))
                {
                    if (file.equals("CurrentServerPublicKey.txt"))
                    {
                        continue;
                    }
                    if (new File(Directory.getPath()+"/"+file).isFile())
                    {
                        ServerPublicKeyList.add(file);
                    }
                }
                SelectServerPublicKey.setValueFactory
                        (new SpinnerValueFactory.ListSpinnerValueFactory<>
                                (FXCollections.observableArrayList(ServerPublicKeyList)));
            }
        }
        else if (Directory.isFile())
        {
            Directory.delete();
            InitPublicKeySpinner();
        }
        else
        {
            Directory.mkdirs();
            InitPublicKeySpinner();
        }
    }

    public void SubmitOfServerPublicKey(ActionEvent actionEvent) {
        if (SelectServerPublicKey.getValue() == null)
        {
            Alert alert = new Alert(Alert.AlertType.NONE);
            alert.getButtonTypes().add(ButtonType.OK);
            alert.setContentText("没有可用的公钥");
            alert.initOwner(stage);
            alert.showAndWait();
            return;
        }

        if (SelectOperationTypeOfServerPublicKey.getValue().equals("重命名"))
        {
            TextInputDialog dialog = new TextInputDialog();
            dialog.initOwner(stage);
            dialog.setTitle("请填写必填信息");
            dialog.setHeaderText("请根据提示填写文件名，如不填写或取消，将会取消操作");
            dialog.setContentText("请输入新文件名：");
            Optional<String> UserInputNewFileName = dialog.showAndWait();
            if (UserInputNewFileName.isPresent() && !(UserInputNewFileName.get().isEmpty()))
            {
                if (new File("./ClientRSAKey/ServerPublicKeys/"+UserInputNewFileName).exists())
                {
                    Alert alert = new Alert(Alert.AlertType.NONE);
                    alert.getButtonTypes().add(ButtonType.OK);
                    alert.setContentText("目标文件已经存在了！");
                    alert.initOwner(stage);
                    alert.showAndWait();
                }
                else
                {
                    try {
                        Files.move(Paths.get("./ClientRSAKey/ServerPublicKeys/"+SelectServerPublicKey.getValue())
                                ,Paths.get("./ClientRSAKey/ServerPublicKeys/"+UserInputNewFileName.get()));
                        InitPublicKeySpinner();
                    } catch (IOException e) {
                        SaveStackTrace.saveStackTrace(e);
                        Alert alert = new Alert(Alert.AlertType.ERROR);
                        Label label = new Label("此报错的调用堆栈相关信息:");

                        StringWriter sw = new StringWriter();
                        PrintWriter pw = new PrintWriter(sw);
                        e.printStackTrace(pw);
                        String exceptionText = sw.toString();

                        TextArea textArea = new TextArea(exceptionText);
                        textArea.setEditable(false);
                        textArea.setWrapText(true);

                        textArea.setMaxWidth(Double.MAX_VALUE);
                        textArea.setMaxHeight(Double.MAX_VALUE);
                        GridPane.setVgrow(textArea, Priority.ALWAYS);
                        GridPane.setHgrow(textArea, Priority.ALWAYS);

                        GridPane expContent = new GridPane();
                        expContent.setMaxWidth(Double.MAX_VALUE);
                        expContent.add(label, 0, 0);
                        expContent.add(textArea, 0, 1);

                        alert.getDialogPane().setExpandableContent(expContent);
                        alert.setContentText("""
                        由于出现内部错误，操作无法完成。
                        如需查看错误原因，请展开详情""");
                        alert.initOwner(stage);
                        alert.showAndWait();
                    }
                }
            }
            else
            {
                Alert alert = new Alert(Alert.AlertType.NONE);
                alert.getButtonTypes().add(ButtonType.OK);
                alert.setContentText("已取消操作");
                alert.initOwner(stage);
                alert.showAndWait();
            }
        }
        else if (SelectOperationTypeOfServerPublicKey.getValue().equals("设为使用的公钥"))
        {
            {
                try {
                    Path path = Paths.get("./ClientRSAKey/ServerPublicKeys/CurrentServerPublicKey.txt");
                    if (new File("./ClientRSAKey/ServerPublicKeys/CurrentServerPublicKey.txt").exists())
                    {
                        Files.delete(path);
                    }
                    Files.copy(Paths.get("./ClientRSAKey/ServerPublicKeys/"+SelectServerPublicKey.getValue())
                            , path);
                    InitPublicKeySpinner();
                } catch (IOException e) {
                    SaveStackTrace.saveStackTrace(e);
                    Alert alert = new Alert(Alert.AlertType.ERROR);
                    Label label = new Label("此报错的调用堆栈相关信息:");

                    StringWriter sw = new StringWriter();
                    PrintWriter pw = new PrintWriter(sw);
                    e.printStackTrace(pw);
                    String exceptionText = sw.toString();

                    TextArea textArea = new TextArea(exceptionText);
                    textArea.setEditable(false);
                    textArea.setWrapText(true);

                    textArea.setMaxWidth(Double.MAX_VALUE);
                    textArea.setMaxHeight(Double.MAX_VALUE);
                    GridPane.setVgrow(textArea, Priority.ALWAYS);
                    GridPane.setHgrow(textArea, Priority.ALWAYS);

                    GridPane expContent = new GridPane();
                    expContent.setMaxWidth(Double.MAX_VALUE);
                    expContent.add(label, 0, 0);
                    expContent.add(textArea, 0, 1);

                    alert.getDialogPane().setExpandableContent(expContent);
                    alert.setContentText("""
                        由于出现内部错误，操作无法完成。
                        如需查看错误原因，请展开详情""");
                    alert.initOwner(stage);
                    alert.showAndWait();
                }
            }
        }
        else if (SelectOperationTypeOfServerPublicKey.getValue().equals("删除此公钥"))
        {
            try {
                Files.delete(Paths.get("./ClientRSAKey/ServerPublicKeys/"+SelectServerPublicKey.getValue()));
                InitPublicKeySpinner();
            } catch (IOException e) {
                SaveStackTrace.saveStackTrace(e);
                Alert alert = new Alert(Alert.AlertType.ERROR);
                Label label = new Label("此报错的调用堆栈相关信息:");

                StringWriter sw = new StringWriter();
                PrintWriter pw = new PrintWriter(sw);
                e.printStackTrace(pw);
                String exceptionText = sw.toString();

                TextArea textArea = new TextArea(exceptionText);
                textArea.setEditable(false);
                textArea.setWrapText(true);

                textArea.setMaxWidth(Double.MAX_VALUE);
                textArea.setMaxHeight(Double.MAX_VALUE);
                GridPane.setVgrow(textArea, Priority.ALWAYS);
                GridPane.setHgrow(textArea, Priority.ALWAYS);

                GridPane expContent = new GridPane();
                expContent.setMaxWidth(Double.MAX_VALUE);
                expContent.add(label, 0, 0);
                expContent.add(textArea, 0, 1);

                alert.getDialogPane().setExpandableContent(expContent);
                alert.setContentText("""
                        由于出现内部错误，操作无法完成。
                        如需查看错误原因，请展开详情""");
                alert.initOwner(stage);
                alert.showAndWait();
            }
        }
    }

    public void SubmitOfEndToEndPublickey(ActionEvent actionEvent) {
        if (SelectEndToEndPublickey.getValue() == null)
        {
            Alert alert = new Alert(Alert.AlertType.NONE);
            alert.getButtonTypes().add(ButtonType.OK);
            alert.setContentText("没有可用的公钥");
            alert.initOwner(stage);
            alert.showAndWait();
            return;
        }
        if (SelectOperationTypeOfEndToEndPublickey.getValue().equals("获取详细信息"))
        {
            String[] partOfName = SelectEndToEndPublickey.getValue().split("-");
            Alert alert;
            if (partOfName.length == 3)
            {
                alert = new Alert(Alert.AlertType.NONE);
                alert.getButtonTypes().add(ButtonType.OK);
                alert.initOwner(stage);
                alert.setTitle("密钥文件信息");
                alert.setContentText(
                        "文件类型：客户端加密公钥\n" +
                                "文件来源服务器地址：" + partOfName[1] + "\n" +
                                "用户名：" + partOfName[2] + "\n" +
                                "文件名：" + SelectEndToEndPublickey.getValue() + "\n"
                );
            }
            else
            {
                alert = new Alert(Alert.AlertType.ERROR);
                alert.getButtonTypes().add(ButtonType.OK);
                alert.setHeaderText("无法完成操作");
                alert.setContentText("此文件已被修改文件名，无法获取文件详细信息");
                alert.initOwner(stage);
            }
            alert.showAndWait();
        }
        else if (SelectOperationTypeOfEndToEndPublickey.getValue().equals("删除此公钥"))
        {
            try {
                Files.delete(Paths.get("./end-to-end_encryption_saved/"+SelectEndToEndPublickey.getValue()));
                InitPublicKeySpinner();
            } catch (IOException e) {
                SaveStackTrace.saveStackTrace(e);
                Alert alert = new Alert(Alert.AlertType.ERROR);
                Label label = new Label("此报错的调用堆栈相关信息:");

                StringWriter sw = new StringWriter();
                PrintWriter pw = new PrintWriter(sw);
                e.printStackTrace(pw);
                String exceptionText = sw.toString();

                TextArea textArea = new TextArea(exceptionText);
                textArea.setEditable(false);
                textArea.setWrapText(true);

                textArea.setMaxWidth(Double.MAX_VALUE);
                textArea.setMaxHeight(Double.MAX_VALUE);
                GridPane.setVgrow(textArea, Priority.ALWAYS);
                GridPane.setHgrow(textArea, Priority.ALWAYS);

                GridPane expContent = new GridPane();
                expContent.setMaxWidth(Double.MAX_VALUE);
                expContent.add(label, 0, 0);
                expContent.add(textArea, 0, 1);

                alert.getDialogPane().setExpandableContent(expContent);
                alert.setContentText("""
                        由于出现内部错误，操作无法完成。
                        如需查看错误原因，请展开详情""");
                alert.initOwner(stage);
                alert.showAndWait();
            }
        }
    }
}
