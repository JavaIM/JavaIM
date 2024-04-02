package org.yuezhikong.utils.Protocol;

public class TransferProtocol {

    /**
     * TransferProtocolHead : {"TargetUserName":"","Type":""}
     * TransferProtocolBody : {"Data":""}
     */

    private TransferProtocolHeadBean TransferProtocolHead;
    private TransferProtocolBodyBean TransferProtocolBody;

    public TransferProtocolHeadBean getTransferProtocolHead() {
        return TransferProtocolHead;
    }

    public void setTransferProtocolHead(TransferProtocolHeadBean TransferProtocolHead) {
        this.TransferProtocolHead = TransferProtocolHead;
    }

    public TransferProtocolBodyBean getTransferProtocolBody() {
        return TransferProtocolBody;
    }

    public void setTransferProtocolBody(TransferProtocolBodyBean TransferProtocolBody) {
        this.TransferProtocolBody = TransferProtocolBody;
    }

    public static class TransferProtocolHeadBean {
        /**
         * TargetUserName :
         * Type :
         */

        private String TargetUserName;
        private String Type;

        public String getTargetUserName() {
            return TargetUserName;
        }

        public void setTargetUserName(String TargetUserName) {
            this.TargetUserName = TargetUserName;
        }

        public String getType() {
            return Type;
        }

        public void setType(String Type) {
            this.Type = Type;
        }
    }

    public static class TransferProtocolBodyBean {
        /**
         * Data :
         */

        private String Data;

        public String getData() {
            return Data;
        }

        public void setData(String Data) {
            this.Data = Data;
        }
    }
}
