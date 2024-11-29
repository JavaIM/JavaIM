package org.yuezhikong.Server.protocolHandler.handlers;

import org.apache.commons.io.FileUtils;
import org.jetbrains.annotations.NotNull;
import org.yuezhikong.Server.IServer;
import org.yuezhikong.Server.protocolHandler.ProtocolHandler;
import org.yuezhikong.Server.userData.Permission;
import org.yuezhikong.Server.userData.user;
import org.yuezhikong.Server.userData.userInformation;
import org.yuezhikong.Server.userData.userUploadFile;
import org.yuezhikong.utils.Protocol.SystemProtocol;
import org.yuezhikong.utils.Protocol.TransferProtocol;
import org.yuezhikong.utils.database.dao.userInformationDao;
import org.yuezhikong.utils.database.dao.userUploadFileDao;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Objects;

public class SystemProtocolHandler implements ProtocolHandler {
    @Override
    public void handleProtocol(@NotNull IServer server, @NotNull String protocolData, user user) {
        SystemProtocol protocol = server.getGson().fromJson(protocolData, SystemProtocol.class);// 反序列化 json 到 object
        switch (protocol.getType()) { // 判断模式
            case "ChangePassword" -> responseChangePassReq(server, user, protocol.getMessage());
            case "TOTP" -> responseTOTPReq(server, user,protocol.getMessage());
            case "DownloadOwnFileByFileName" -> responseDownloadOwnFileByFileNameReq(server, user,protocol.getMessage());
            case "DownloadFileByFileId" -> responseDownloadFileByFileIdReq(server, user,protocol.getMessage());
            case "DeleteUploadFileByFileId" -> responseDeleteUploadFileByFileIdReq(server, user,protocol.getMessage());
            case "GetFileIdByFileName" -> responseGetFileIdByFileNameReq(server, user,protocol.getMessage());
            case "GetFileNameByFileId" -> responseGetFileNameByFileIdReq(server, user,protocol.getMessage());
            case "GetUploadFileList" -> responseGetUploadFileListReq(server, user);
            case "SetAvatarId" -> responseSetAvatarIdReq(server, user,protocol.getMessage());
            case "GetAvatarIdByUserName" -> responseGetAvatarIdByUserNameReq(server, user,protocol.getMessage());
            case "Login", "Error", "DisplayMessage" -> {
                SystemProtocol systemProtocol = new SystemProtocol();
                systemProtocol.setType("Error");
                systemProtocol.setMessage("Invalid Protocol Type, Please Check it");
                server.getServerAPI().sendJsonToClient(user, server.getGson().toJson(systemProtocol), "SystemProtocol");
            }
            default -> server.getServerAPI().sendMessageToUser(user, "暂不支持此模式");
        }
    }

    /**
     * 处理获取用户头像Id的请求
     * @param server    服务器实例
     * @param user      用户
     * @param userName  用户名
     */
    private void responseGetAvatarIdByUserNameReq(IServer server, user  user, String userName) {
        if (!checkLoginStatus(server, user)) // 检查登录状态
            return;
        userInformation information = server.getSqlSession().getMapper(userInformationDao.class).getUser(null, userName, null, null);
        if (information == null) {
            SystemProtocol systemProtocol = new SystemProtocol();
            systemProtocol.setType("Error");
            systemProtocol.setMessage("User Not Found");
            server.getServerAPI().sendJsonToClient(user, server.getGson().toJson(systemProtocol), "SystemProtocol");
            return;
        }

        SystemProtocol systemProtocol = new SystemProtocol();
        systemProtocol.setType("GetAvatarIdByUserNameResult");
        systemProtocol.setMessage(information.getAvatar());
        server.getServerAPI().sendJsonToClient(user, server.getGson().toJson(systemProtocol), "SystemProtocol");
    }

    /**
     * 处理设置头像文件Id的请求
     * @param server    服务器实例
     * @param user      用户
     * @param fileId    文件Id
     */
    private void responseSetAvatarIdReq(IServer server, user  user, String fileId) {
        if (!checkLoginStatus(server, user)) // 检查登录状态
            return;
        // 检测是否存在
        userUploadFile uploadFile = server.getSqlSession().getMapper(userUploadFileDao.class).getUploadFileByFileId(fileId);
        if (uploadFile == null) {
            SystemProtocol systemProtocol = new SystemProtocol();
            systemProtocol.setType("Error");
            systemProtocol.setMessage("File Not Found");
            server.getServerAPI().sendJsonToClient(user, server.getGson().toJson(systemProtocol), "SystemProtocol");
            return;
        }
        // 如果存在，设置
        user.getUserInformation().setAvatar(fileId);
    }

    /**
     * 处理获取上传文件列表的请求
     * @param server    服务器实例
     * @param user      用户
     */
    private void responseGetUploadFileListReq(IServer server, user user) {
        if (!checkLoginStatus(server, user)) // 检查登录状态
            return;
        List<userUploadFile> uploadFiles = server.getSqlSession().getMapper(userUploadFileDao.class).getUploadFilesByUserId(user.getUserInformation().getUserId());// 获取文件
        if (uploadFiles == null) {// 如果没找到
            SystemProtocol systemProtocol = new SystemProtocol();
            systemProtocol.setType("Error");
            systemProtocol.setMessage("File Not Found");
            server.getServerAPI().sendJsonToClient(user, server.getGson().toJson(systemProtocol), "SystemProtocol");
            return;
        }
        TransferProtocol transferProtocol = new TransferProtocol();// 封装数据包
        transferProtocol.setTransferProtocolHead(new TransferProtocol.TransferProtocolHeadBean());
        transferProtocol.getTransferProtocolHead().setType("fileList");
        transferProtocol.setTransferProtocolBody(new ArrayList<>());

        uploadFiles.forEach((file -> {
            TransferProtocol.TransferProtocolBodyBean bodyBean = new TransferProtocol.TransferProtocolBodyBean();
            bodyBean.setData(file.getOrigFileName());
            transferProtocol.getTransferProtocolBody().add(bodyBean);
        }));
        server.getServerAPI().sendJsonToClient(user, server.getGson().toJson(transferProtocol), "TransferProtocol");// 发送数据包
    }

    /**
     * 处理获取文件名请求
     * @param server    服务器实例
     * @param user      用户
     * @param fileId    文件Id
     */
    private void responseGetFileNameByFileIdReq(IServer server, user  user, String fileId) {
        if (!checkLoginStatus(server, user)) // 检查登录状态
            return;
        userUploadFile uploadFile = server.getSqlSession().getMapper(userUploadFileDao.class).getUploadFileByFileId(fileId);// 获取文件
        if (uploadFile == null) {// 如果没找到
            SystemProtocol systemProtocol = new SystemProtocol();
            systemProtocol.setType("Error");
            systemProtocol.setMessage("File Not Found");
            server.getServerAPI().sendJsonToClient(user, server.getGson().toJson(systemProtocol), "SystemProtocol");
        }

    }

    /**
     * 处理获取文件Id请求
     * @param server    服务器实例
     * @param user      用户
     * @param fileName  文件名
     */
    private void responseGetFileIdByFileNameReq(IServer server, user  user, String fileName) {
        if (!checkLoginStatus(server, user)) // 检查登录状态
            return;
        List<userUploadFile> uploadFiles = server.getSqlSession().getMapper(userUploadFileDao.class)
                .getUploadFilesByUserId(user.getUserInformation().getUserId());// 获取该用户上传文件列表
        if (uploadFiles == null) {// 如果没上传任何文件
            SystemProtocol systemProtocol = new SystemProtocol();
            systemProtocol.setType("Error");
            systemProtocol.setMessage("File Not Found");
            server.getServerAPI().sendJsonToClient(user, server.getGson().toJson(systemProtocol), "SystemProtocol");
            return;
        }

        String FileId = null;
        for (userUploadFile uploadFile : uploadFiles) {// 获取FileId
            if (uploadFile.getOrigFileName().equals(fileName)) {
                FileId = uploadFile.getOwnFile();
                break;
            }
        }
        if (FileId == null) {// 如果没找到
            SystemProtocol systemProtocol = new SystemProtocol();
            systemProtocol.setType("Error");
            systemProtocol.setMessage("File Not Found");
            server.getServerAPI().sendJsonToClient(user, server.getGson().toJson(systemProtocol), "SystemProtocol");
            return;
        }
        SystemProtocol systemProtocol = new SystemProtocol();
        systemProtocol.setType("GetFileIdByFileNameResult");
        systemProtocol.setMessage(FileId);
        server.getServerAPI().sendJsonToClient(user, server.getGson().toJson(systemProtocol), "SystemProtocol");
    }

    /**
     * 处理删除文件请求
     * @param server    服务器实例
     * @param user      用户
     * @param fileId    文件Id
     */
    private void responseDeleteUploadFileByFileIdReq(IServer server, user  user, String fileId) {
        if (!checkLoginStatus(server, user)) // 检查登录状态
            return;
        userUploadFileDao mapper = server.getSqlSession().getMapper(userUploadFileDao.class);
        userUploadFile uploadFile = mapper.getUploadFileByFileId(fileId);// 获取文件
        if (uploadFile == null) { // 如果没找到
            SystemProtocol systemProtocol = new SystemProtocol();
            systemProtocol.setType("Error");
            systemProtocol.setMessage("File Not Found");
            server.getServerAPI().sendJsonToClient(user, server.getGson().toJson(systemProtocol), "SystemProtocol");
            return;
        }
        // 如果操作者不是文件拥有者，且操作者不是管理员，则禁止操作
        if (!user.getUserInformation().getUserId().equals(uploadFile.getUserId()) && !Permission.ADMIN.equals(user.getUserPermission())) {
            SystemProtocol systemProtocol = new SystemProtocol();
            systemProtocol.setType("Error");
            systemProtocol.setMessage("Permission denied");
            server.getServerAPI().sendJsonToClient(user, server.getGson().toJson(systemProtocol), "SystemProtocol");
            return;
        }
        // 开始删除
        File file = new File("./uploadFiles", uploadFile.getOwnFile());
        if (!file.delete()) {
            SystemProtocol systemProtocol = new SystemProtocol();
            systemProtocol.setType("Error");
            systemProtocol.setMessage("Permission denied by platform");
            server.getServerAPI().sendJsonToClient(user, server.getGson().toJson(systemProtocol), "SystemProtocol");
            return;
        }

        if (!mapper.deleteFile(uploadFile)) {
            SystemProtocol systemProtocol = new SystemProtocol();
            systemProtocol.setType("Error");
            systemProtocol.setMessage("Permission denied by platform");
            server.getServerAPI().sendJsonToClient(user, server.getGson().toJson(systemProtocol), "SystemProtocol");
            return;
        }
        server.getServerAPI().sendMessageToUser(user, "操作成功完成。");
    }

    /**
     * 处理根据文件id下载文件请求
     * @param server    服务器实例
     * @param user      用户
     * @param fileId    文件Id
     */
    private void responseDownloadFileByFileIdReq(IServer server, user  user, String fileId) {
        if (!checkLoginStatus(server, user)) // 检查登录状态
            return;
        userUploadFile uploadFile = server.getSqlSession().getMapper(userUploadFileDao.class).getUploadFileByFileId(fileId);// 获取文件
        if (uploadFile == null) {// 如果没找到
            SystemProtocol systemProtocol = new SystemProtocol();
            systemProtocol.setType("Error");
            systemProtocol.setMessage("File Not Found");
            server.getServerAPI().sendJsonToClient(user, server.getGson().toJson(systemProtocol), "SystemProtocol");
            return;
        }
        sendFile(fileId, server, user, uploadFile.getOrigFileName());// 发送文件
    }


    /**
     * 处理下载自己文件请求
     * @param server    服务器实例
     * @param user      用户
     * @param fileName  文件名
     */
    private void responseDownloadOwnFileByFileNameReq(IServer server, user  user, String fileName) {
        if (!checkLoginStatus(server, user)) // 检查登录状态
            return;
        List<userUploadFile> uploadFiles = server.getSqlSession().getMapper(userUploadFileDao.class)
                .getUploadFilesByUserId(user.getUserInformation().getUserId());// 获取该用户上传文件列表
        if (uploadFiles == null) {// 如果没上传任何文件
            SystemProtocol systemProtocol = new SystemProtocol();
            systemProtocol.setType("Error");
            systemProtocol.setMessage("File Not Found");
            server.getServerAPI().sendJsonToClient(user, server.getGson().toJson(systemProtocol), "SystemProtocol");
            return;
        }

        String FileId = null;
        for (userUploadFile uploadFile : uploadFiles) {// 获取FileId
            if (uploadFile.getOrigFileName().equals(fileName)) {
                FileId = uploadFile.getOwnFile();
                break;
            }
        }
        if (FileId == null) {// 如果没找到
            SystemProtocol systemProtocol = new SystemProtocol();
            systemProtocol.setType("Error");
            systemProtocol.setMessage("File Not Found");
            server.getServerAPI().sendJsonToClient(user, server.getGson().toJson(systemProtocol), "SystemProtocol");
            return;
        }
        sendFile(FileId, server, user, fileName);// 发送文件
    }

    /**
     * 处理TOTP请求
     * @param server  服务器实例
     * @param user    用户
     * @param code    TOTP验证码
     */
    private void responseTOTPReq(IServer server, user  user, String code) {
        if (!checkLoginStatus(server, user)) // 检查登录状态
            return;
        try {
            Objects.requireNonNull(user.getUserAuthentication()).extendSecurity(code);// 校验TOTP
        } catch (IllegalStateException e) {
            server.getServerAPI().sendMessageToUser(user, "没有请求增强安全性");
        }
    }

    /**
     * 处理修改密码请求
     * @param server    服务器实例
     * @param user      用户
     * @param newPass   新密码
     */
    private void responseChangePassReq(IServer server, user  user, String newPass) {
        if (!checkLoginStatus(server, user)) // 检查登录状态
            return;
        server.getServerAPI().changeUserPassword(user, newPass);//修改密码
    }

    /**
     * 检查登录状态
     * @param server 服务器实例
     * @param user 用户
     * @return       是否登录
     */
    private boolean checkLoginStatus(IServer server, user  user) {
        if (!user.isUserLogged()) {
            server.getServerAPI().sendMessageToUser(user, "请先登录");
            return false;
        }
        return true;
    }

    /**
     * 发送文件
     * @param fileId    文件Id
     * @param server 服务器实例
     * @param user 用户
     * @param fileName  文件名
     */
    public void sendFile(String fileId, IServer server, user  user, String fileName) {
        File file = new File("./uploadFiles", fileId);// 读取文件
        String content;
        try {
            content = Base64.getEncoder().encodeToString(FileUtils.readFileToByteArray(file));
        } catch (IOException e) {
            SystemProtocol systemProtocol = new SystemProtocol();
            systemProtocol.setType("Error");
            systemProtocol.setMessage("File Not Found");
            server.getServerAPI().sendJsonToClient(user, server.getGson().toJson(systemProtocol), "SystemProtocol");
            return;
        }

        TransferProtocol transferProtocol = new TransferProtocol();// 封装数据包
        transferProtocol.setTransferProtocolHead(new TransferProtocol.TransferProtocolHeadBean());
        transferProtocol.getTransferProtocolHead().setType("download");
        transferProtocol.setTransferProtocolBody(new ArrayList<>());

        TransferProtocol.TransferProtocolBodyBean fileNameBean = new TransferProtocol.TransferProtocolBodyBean();
        fileNameBean.setData(fileName);
        TransferProtocol.TransferProtocolBodyBean fileContentBean = new TransferProtocol.TransferProtocolBodyBean();
        fileContentBean.setData(content);

        transferProtocol.getTransferProtocolBody().add(fileNameBean);
        transferProtocol.getTransferProtocolBody().add(fileContentBean);

        server.getServerAPI().sendJsonToClient(user, server.getGson().toJson(transferProtocol), "TransferProtocol");// 发送数据包
    }
}
