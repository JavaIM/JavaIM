package org.yuezhikong.utils.protocol;

import lombok.Data;

/**
 * ProtocolVersion：协议版本，固定CodeDynamicConfig中的ProtocolVersion
 * ProtocolName：协议名，希望对端使用何种协议解析
 * ProtocolData：协议数据
 */
@Data
public class GeneralProtocol {
    private int ProtocolVersion;
    private String ProtocolName;
    private String ProtocolData;
}
