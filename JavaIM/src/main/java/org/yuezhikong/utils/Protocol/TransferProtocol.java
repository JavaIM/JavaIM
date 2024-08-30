package org.yuezhikong.utils.Protocol;

import lombok.Data;

import java.util.List;

/**
 * 一个用于转发文件/消息的协议
 * type:transfer
 * 转发到用户（尚未完成）
 * 此模式下，body约定List中的第一个会把发送到目标用户\，其他全部忽略
 * type:upload或download
 * 上传或下载文件文件
 * upload只会出现在客户端->服务端，download只会出现在服务端->客户端
 * 此模式下，body约定List中的第一个是文件名，第二个为文件内容，其他全部忽略
 * type:fileList
 * 文件列表
 * 此模式下，body约定List中每一个元素都是上传的文件的文件名
 * type:QRCode
 * 二维码
 * 此模式下，body约定List中每一个元素都是二维码的base64编码
 */
@Data
public class TransferProtocol {
    private TransferProtocolHeadBean TransferProtocolHead;
    private List<TransferProtocolBodyBean> TransferProtocolBody;

    @Data
    public static class TransferProtocolHeadBean {
        private String TargetUserName;
        private String Type;
    }

    @Data
    public static class TransferProtocolBodyBean {
        private String Data;
    }
}
