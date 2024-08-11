package org.yuezhikong.utils.Protocol;

import lombok.Data;

/**
 * SourceUserName仅出现在Server -> Client,服务端不会响应客户端的此请求
 */
@Data
public class ChatProtocol {
    private String Message;
    private String SourceUserName;
}
