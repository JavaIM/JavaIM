package org.yuezhikong.Server.protocolHandler.handlers;

import org.apache.commons.io.FileUtils;
import org.jetbrains.annotations.NotNull;
import org.yuezhikong.Server.IServer;
import org.yuezhikong.Server.protocolHandler.ProtocolHandler;
import org.yuezhikong.Server.userData.user;
import org.yuezhikong.Server.userData.userUploadFile;
import org.yuezhikong.utils.Protocol.SystemProtocol;
import org.yuezhikong.utils.Protocol.TransferProtocol;
import org.yuezhikong.utils.database.dao.userUploadFileDao;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

public class TransferProtocolHandler implements ProtocolHandler {
    @Override
    public void handleProtocol(@NotNull IServer server, @NotNull String protocolData, user user) {
        if (!user.isUserLogged()) { // 检查登录状态
            server.getServerAPI().sendMessageToUser(user, "请先登录");
            return;
        }
        TransferProtocol protocol = server.getGson().fromJson(protocolData, TransferProtocol.class);// 反序列化 json 到 object
        if (protocol.getTransferProtocolHead().getType().equals("upload")) // 判断模式
            handleUpload(server, protocol, user);
        else
            server.getServerAPI().sendMessageToUser(user, "暂不支持此模式");
    }

    /**
     * 处理上传文件请求
     * @param server    服务器实例
     * @param protocol  协议 json 数据
     * @param user      用户
     */
    private void handleUpload(IServer server, TransferProtocol protocol, user user) {
        List<TransferProtocol.TransferProtocolBodyBean> data = protocol.getTransferProtocolBody();// 读取列表
        if (data.size() != 2 || data.get(0).getData() == null || data.get(1).getData() == null) {// 不符合规定返回无效数据包错误给客户端
            SystemProtocol systemProtocol = new SystemProtocol();
            systemProtocol.setType("Error");
            systemProtocol.setMessage("Invalid Packet");
            server.getServerAPI().sendJsonToClient(user, server.getGson().toJson(systemProtocol), "SystemProtocol");
            return;
        }
        String fileName = data.get(0).getData();// 读取出文件名

        userUploadFileDao uploadFileDao = server.getSqlSession().getMapper(userUploadFileDao.class);// 读取数据库，查询同一用户下是否有重名文件
        List<userUploadFile> uploadFiles = uploadFileDao.getUploadFilesByUserId(user.getUserInformation().getUserId());
        if (uploadFiles != null)
            for (userUploadFile uploadFile : uploadFiles) {
                if (uploadFile.getOrigFileName().equals(fileName)) {
                    SystemProtocol systemProtocol = new SystemProtocol();
                    systemProtocol.setType("Error");
                    systemProtocol.setMessage("File Already Exists");
                    server.getServerAPI().sendJsonToClient(user, server.getGson().toJson(systemProtocol), "SystemProtocol");
                    return;
                }
            }

        byte[] fileContent;// 读取消息内容
        try {
            fileContent = Base64.getDecoder().decode(data.get(1).getData().getBytes(StandardCharsets.UTF_8));
        } catch (IllegalArgumentException e) {
            SystemProtocol systemProtocol = new SystemProtocol();
            systemProtocol.setType("Error");
            systemProtocol.setMessage("Invalid Packet");
            server.getServerAPI().sendJsonToClient(user, server.getGson().toJson(systemProtocol), "SystemProtocol");
            return;
        }

        String fileId;// 分配文件UUID
        do {
            fileId = UUID.randomUUID().toString();
            userUploadFile uploadFile = uploadFileDao.getUploadFileByFileId(fileId);
            if (uploadFile == null) {
                break;
            }
        } while (true);
        uploadFileDao.addFile(new userUploadFile(user.getUserInformation().getUserId(), fileId, fileName));// 写入文件

        File uploadFileDirectory = new File("./uploadFiles");
        File uploadFile = new File(uploadFileDirectory, fileId);
        try {
            try {
                Files.createDirectory(uploadFileDirectory.toPath());
            } catch (FileAlreadyExistsException ignored) {
            }
            Files.createFile(uploadFile.toPath());

            FileUtils.writeByteArrayToFile(uploadFile, fileContent);
        } catch (IOException e) {
            throw new RuntimeException("write File Failed", e);
        }
        server.getServerAPI().sendMessageToUser(user, "操作成功完成。");// 提示用户
    }
}
