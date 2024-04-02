package org.yuezhikong.utils.Protocol;

public class GeneralProtocol {

    /**
     * ProtocolVersion : 0
     * ProtocolName : NormalProtocol
     * ProtocolData : Data
     */

    private int ProtocolVersion;
    private String ProtocolName;
    private String ProtocolData;

    public int getProtocolVersion() {
        return ProtocolVersion;
    }

    public void setProtocolVersion(int ProtocolVersion) {
        this.ProtocolVersion = ProtocolVersion;
    }

    public String getProtocolName() {
        return ProtocolName;
    }

    public void setProtocolName(String ProtocolName) {
        this.ProtocolName = ProtocolName;
    }

    public String getProtocolData() {
        return ProtocolData;
    }

    public void setProtocolData(String ProtocolData) {
        this.ProtocolData = ProtocolData;
    }
}
