package org.yuezhikong.utils.Protocol;

import lombok.Data;

import java.util.List;

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
