package org.yuezhikong.GraphicalUserInterface.ServerAndKeyManagement;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.stage.FileChooser;
import org.apache.commons.io.FileUtils;
import org.yuezhikong.CodeDynamicConfig;
import org.yuezhikong.GraphicalUserInterface.DefaultController;
import org.yuezhikong.GraphicalUserInterface.Dialogs.PortInputDialog;
import org.yuezhikong.GraphicalUserInterface.Dialogs.SpinnerDialog;
import org.yuezhikong.utils.SaveStackTrace;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

public class ServerAndKeyManagementUI extends DefaultController implements Initializable {
    public Spinner<String> SelectEndToEndPublickey;
    public Spinner<String> SelectOperationTypeOfServer;
    public Spinner<String> SelectOperationTypeOfEndToEndPublickey;

    public static void tryUpdateSavedServerLayout(SavedServerFileLayout layout) {
        if (layout.getVersion() == 1) {//自动更新判断，后期大于3个版本后，使用switch
            layout.setVersion(2);
            for (SavedServerFileLayout.ServerInformationBean bean : layout.getServerInformation())
            {
                bean.setServerToken("");
            }
        }
    }

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
        //初始化SelectOperationTypeOfServer
        List<String> ServerPublicKeyOperationType = new ArrayList<>();
        ServerPublicKeyOperationType.add("添加服务器");
        ServerPublicKeyOperationType.add("删除服务器");
        ServerPublicKeyOperationType.add("设置备注");
        ServerPublicKeyOperationType.add("查看服务器信息");
        SelectOperationTypeOfServer.setValueFactory
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
        }
        else
        {
            Directory.mkdirs();
            InitPublicKeySpinner();
        }
    }

    public record ServerPortAndServerAddress(String ServerAddress,int ServerPort) {}
    public static HashMap<String, ServerPortAndServerAddress> getServerManagementCanSelectHashMap(List<SavedServerFileLayout.ServerInformationBean>
                                                                  informationList)
    {
        HashMap<String,ServerPortAndServerAddress> CanSelectHashMap = new HashMap<>();
        for (SavedServerFileLayout.ServerInformationBean informationBean : informationList)
        {
            CanSelectHashMap.put(!informationBean.getServerRemark().isEmpty() ?
                    "ip:"+informationBean.getServerAddress()+"端口:"+informationBean.getServerPort()+
                            "备注为:"+informationBean.getServerRemark() :
                    "ip:"+informationBean.getServerAddress()+"端口:"+informationBean.getServerPort(),
                    new ServerPortAndServerAddress(informationBean.getServerAddress(),informationBean.getServerPort()));
            //Java三目运算符，如果informationBean.getServerRemark() != null成立，显示ip、端口、备注
            //否则仅显示ip、端口
        }
        return CanSelectHashMap;
    }
    public static File getSavedServerFile()
    {
        return new File("./SavedServers.json");
    }
    public void SubmitOfServerManagement(ActionEvent actionEvent) {
        SavedServerFileLayout layout = null;
        Gson gson = new Gson();
        File SavedServerFile = getSavedServerFile();
        try {
            if ((!SavedServerFile.exists() && !SavedServerFile.createNewFile()) ||
                    (SavedServerFile.isDirectory()
                            && !(SavedServerFile.delete() || SavedServerFile.createNewFile())))
                // 如果文件夹不存在，且创建失败，执行此分支
                // 如果对应文件夹是一个文件，无法被删除或删除后无法被成功创建文件夹，执行此分支
            {
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.initOwner(stage);
                alert.setTitle("JavaIM --- 出现错误");
                alert.setContentText("""
                    由于出现文件系统错误，无法管理保存的服务器
                    请您检查本程序是否可以在当前目录下读写文件
                    程序由于此问题，服务器管理已被取消!""");
                alert.showAndWait();
                return;
            }
            if (SavedServerFile.length() == 0) {
                layout = new SavedServerFileLayout();
                layout.setVersion(CodeDynamicConfig.SavedServerFileVersion);
                layout.setServerInformation(new ArrayList<>());
                FileUtils.writeStringToFile(SavedServerFile,gson.toJson(layout), StandardCharsets.UTF_8);
            }
            else {
                try {
                    layout = gson.fromJson(FileUtils.readFileToString(SavedServerFile, StandardCharsets.UTF_8)
                            , SavedServerFileLayout.class);
                } catch (JsonSyntaxException e) {
                    Alert alert = new Alert(Alert.AlertType.ERROR);
                    alert.initOwner(stage);
                    alert.setTitle("JavaIM --- 出现错误");
                    alert.setContentText("无法解析保存的服务器文件，请检查文件内容");
                    alert.showAndWait();
                    return;
                }
                if (layout.getVersion() != CodeDynamicConfig.SavedServerFileVersion &&
                    layout.getVersion() != 1//版本1已经兼容自动升级，因此可以继续
                )
                {
                    Alert alert = new Alert(Alert.AlertType.ERROR);
                    alert.initOwner(stage);
                    alert.setTitle("JavaIM --- 出现错误");
                    alert.setContentText("""
                    由于保存的服务器文件版本与程序版本不一致
                    为防止覆盖您以前保存的服务器
                    因此，服务器管理已被取消!
                    建议检查保存的服务器文件中的数据!""");
                    alert.showAndWait();
                    return;
                }
                tryUpdateSavedServerLayout(layout);
                FileUtils.writeStringToFile(SavedServerFile,gson.toJson(layout), StandardCharsets.UTF_8);
            }
            switch (SelectOperationTypeOfServer.getValue())
            {
                case "添加服务器" -> {
                    //服务器IP地址
                    TextInputDialog dialog = new TextInputDialog();
                    dialog.setTitle("JavaIM --- 添加服务器");
                    dialog.setContentText("请输入服务器IP地址：");
                    Optional<String> ServerAddressOfUserInput = dialog.showAndWait();
                    if (ServerAddressOfUserInput.isEmpty() || ServerAddressOfUserInput.get().isEmpty())
                        return;
                    //端口
                    PortInputDialog portInputDialog = new PortInputDialog();
                    portInputDialog.initOwner(stage);
                    portInputDialog.setTitle("JavaIM --- 添加服务器");
                    portInputDialog.setContentText("请输入服务器端口：");
                    Optional<String> ServerPortOfUserInput = portInputDialog.showAndWait();
                    if (ServerPortOfUserInput.isEmpty() || ServerPortOfUserInput.get().isEmpty())
                        return;

                    //请求服务端公钥文件
                    Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
                    alert.initOwner(stage);
                    alert.setTitle("JavaIM --- 提示");
                    alert.setHeaderText("如果要继续添加服务器，需要服务端公钥");
                    alert.setContentText("是否继续添加服务器?");
                    Optional<ButtonType> select = alert.showAndWait();
                    if (select.isEmpty() || !(select.get().equals(ButtonType.OK)))
                        return;
                    FileChooser chooser = new FileChooser();
                    chooser.setTitle("打开 服务端公钥文件");
                    chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("服务端公钥文件 (*.txt)","*.txt"));
                    File ServerPublicKeyFile = chooser.showOpenDialog(stage);
                    if (ServerPublicKeyFile == null)
                        return;

                    //检查是否已经存在
                    for (var bean : layout.getServerInformation())
                    {
                        if (bean.getServerAddress().equals(ServerAddressOfUserInput.get()) &&
                                Integer.parseInt(ServerPortOfUserInput.get()) == bean.getServerPort())
                        {
                            alert.setAlertType(Alert.AlertType.NONE);
                            alert.setTitle("JavaIM --- 提示");
                            alert.setContentText("此服务器已经存在了!");
                            alert.getButtonTypes().add(ButtonType.OK);
                            alert.initOwner(stage);
                            alert.showAndWait();
                            return;
                        }
                    }

                    SavedServerFileLayout.ServerInformationBean information = new SavedServerFileLayout.ServerInformationBean();
                    information.setServerRemark("");
                    information.setServerToken("");
                    information.setServerAddress(ServerAddressOfUserInput.get());
                    information.setServerPort(Integer.parseInt(ServerPortOfUserInput.get()));
                    information.setServerPublicKey(FileUtils.readFileToString(ServerPublicKeyFile,StandardCharsets.UTF_8));

                    layout.getServerInformation().add(information);

                    alert.setAlertType(Alert.AlertType.NONE);
                    alert.setHeaderText("");
                    alert.setTitle("JavaIM --- 提示");
                    alert.setContentText("服务器添加成功!");
                    alert.getButtonTypes().add(ButtonType.OK);
                    alert.showAndWait();
                }
                case "删除服务器" -> {
                    var canSelectHashMap = getServerManagementCanSelectHashMap(layout.getServerInformation());
                    if (canSelectHashMap.isEmpty())
                    {
                        Alert alert = new Alert(Alert.AlertType.NONE);
                        alert.setTitle("JavaIM --- 提示");
                        alert.setContentText("当前没有添加服务器，去添加一个吧!");
                        alert.getButtonTypes().add(ButtonType.OK);
                        alert.initOwner(stage);
                        alert.showAndWait();
                        return;
                    }
                    List<String> canSelectList = new ArrayList<>();
                    canSelectHashMap.forEach((string, serverPortAndServerAddress) -> {
                        canSelectList.add(string);
                    });
                    SpinnerDialog dialog = new SpinnerDialog(canSelectList,stage,"JavaIM --- 选择操作的服务器");
                    Optional<SpinnerDialog.DialogReturn> returnOptional = dialog.showAndWait();
                    if (returnOptional.isEmpty())
                        return;
                    var SelectServerData = canSelectHashMap.get(returnOptional.get().SpinnerSelect());
                    var serverInformation = layout.getServerInformation();

                    serverInformation.removeIf(informationBean ->
                            SelectServerData.ServerAddress.equals(informationBean.getServerAddress())
                            && SelectServerData.ServerPort == informationBean.getServerPort());
                    Alert alert = new Alert(Alert.AlertType.NONE);
                    alert.setTitle("JavaIM --- 提示");
                    alert.setContentText("操作成功完成!");
                    alert.getButtonTypes().add(ButtonType.OK);
                    alert.initOwner(stage);
                    alert.showAndWait();
                }
                case "设置备注" -> {
                    TextInputDialog textInputDialog = new TextInputDialog();
                    textInputDialog.setTitle("JavaIM --- 设置备注");
                    textInputDialog.setContentText("请输入新的备注");
                    textInputDialog.initOwner(stage);
                    Optional<String> input = textInputDialog.showAndWait();
                    if (input.isEmpty())
                        return;

                    var canSelectHashMap = getServerManagementCanSelectHashMap(layout.getServerInformation());
                    if (canSelectHashMap.isEmpty())
                    {
                        Alert alert = new Alert(Alert.AlertType.NONE);
                        alert.setTitle("JavaIM --- 提示");
                        alert.setContentText("当前没有添加服务器，去添加一个吧!");
                        alert.getButtonTypes().add(ButtonType.OK);
                        alert.initOwner(stage);
                        alert.showAndWait();
                        return;
                    }
                    List<String> canSelectList = new ArrayList<>();
                    canSelectHashMap.forEach((string, serverPortAndServerAddress) -> canSelectList.add(string));
                    SpinnerDialog dialog = new SpinnerDialog(canSelectList,stage,"JavaIM --- 选择操作的服务器");
                    Optional<SpinnerDialog.DialogReturn> returnOptional = dialog.showAndWait();
                    if (returnOptional.isEmpty())
                        return;
                    var SelectServerData = canSelectHashMap.get(returnOptional.get().SpinnerSelect());
                    var serverInformation = layout.getServerInformation();

                    for (var bean : serverInformation)
                    {
                        if (SelectServerData.ServerPort == bean.getServerPort()
                                && SelectServerData.ServerAddress.equals(bean.getServerAddress()))
                        {
                            bean.setServerRemark(input.get());
                            Alert alert = new Alert(Alert.AlertType.NONE);
                            alert.setTitle("JavaIM --- 提示");
                            alert.setContentText("操作成功完成!");
                            alert.getButtonTypes().add(ButtonType.OK);
                            alert.initOwner(stage);
                            alert.showAndWait();
                            return;
                        }
                    }
                }
                case "查看服务器信息" -> {
                    var canSelectHashMap = getServerManagementCanSelectHashMap(layout.getServerInformation());
                    if (canSelectHashMap.isEmpty())
                    {
                        Alert alert = new Alert(Alert.AlertType.NONE);
                        alert.setTitle("JavaIM --- 提示");
                        alert.setContentText("当前没有添加服务器，去添加一个吧!");
                        alert.getButtonTypes().add(ButtonType.OK);
                        alert.initOwner(stage);
                        alert.showAndWait();
                        return;
                    }
                    List<String> canSelectList = new ArrayList<>();
                    canSelectHashMap.forEach((string, serverPortAndServerAddress) -> canSelectList.add(string));
                    SpinnerDialog dialog = new SpinnerDialog(canSelectList,stage,"JavaIM --- 选择操作的服务器");
                    Optional<SpinnerDialog.DialogReturn> returnOptional = dialog.showAndWait();
                    if (returnOptional.isEmpty())
                        return;
                    var SelectServerData = canSelectHashMap.get(returnOptional.get().SpinnerSelect());
                    var serverInformation = layout.getServerInformation();

                    for (var bean : serverInformation)
                    {
                        if (SelectServerData.ServerPort == bean.getServerPort()
                                && SelectServerData.ServerAddress.equals(bean.getServerAddress()))
                        {
                            Alert alert = new Alert(Alert.AlertType.NONE);
                            alert.setTitle("JavaIM --- 提示");
                            if (bean.getServerRemark().isEmpty()) {
                                alert.setContentText(
                                        "服务器ip:" + SelectServerData.ServerAddress + "\n" +
                                        "服务器端口:" + SelectServerData.ServerPort + "\n"
                                );
                            }
                            else {
                                alert.setContentText(
                                        "服务器ip:" + SelectServerData.ServerAddress + "\n" +
                                                "服务器端口:" + SelectServerData.ServerPort + "\n" +
                                                "备注:" + bean.getServerRemark() + "\n"
                                );
                            }
                            alert.getButtonTypes().add(ButtonType.OK);
                            alert.initOwner(stage);
                            alert.showAndWait();
                            return;
                        }
                    }
                }
            }
        }
        catch (IOException e) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.initOwner(stage);
            alert.setTitle("JavaIM --- 出现错误");
            alert.setContentText("""
                    由于出现文件系统错误，无法管理保存的服务器
                    请您检查本程序是否可以在当前目录下读写文件
                    程序由于此问题，服务器管理已被取消!""");
            alert.showAndWait();
            SaveStackTrace.saveStackTrace(e);
        }
        finally {
            if (layout != null)
            {
                try {
                    FileUtils.writeStringToFile(SavedServerFile,gson.toJson(layout), StandardCharsets.UTF_8);
                } catch (IOException e) {
                    Alert alert = new Alert(Alert.AlertType.ERROR);
                    alert.initOwner(stage);
                    alert.setTitle("JavaIM --- 出现错误");
                    alert.setContentText("""
                    由于出现文件系统错误，无法保存配置项
                    请您检查本程序是否可以在当前目录下读写文件
                    程序由于此问题，服务器管理已被取消!""");
                    alert.showAndWait();
                    SaveStackTrace.saveStackTrace(e);
                }
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
        switch (SelectOperationTypeOfEndToEndPublickey.getValue())
        {
            case "获取详细信息" -> {
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
            case "删除此公钥" -> {
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
}
