package org.yuezhikong.GraphicalUserInterface.ServerAndKeyManagement;

import java.util.List;

public class SavedServerFileLayout {

    /**
     * Version : 1
     * ServerInformation : [{"ServerRemark":"","ServerAddress":"","ServerPort":1,"ServerPublicKey":""}]
     */

    private int Version;
    private List<ServerInformationBean> ServerInformation;

    public int getVersion() {
        return Version;
    }

    public void setVersion(int Version) {
        this.Version = Version;
    }

    public List<ServerInformationBean> getServerInformation() {
        return ServerInformation;
    }

    public void setServerInformation(List<ServerInformationBean> ServerInformation) {
        this.ServerInformation = ServerInformation;
    }

    public static class ServerInformationBean {
        /**
         * ServerRemark :
         * ServerAddress :
         * ServerPort : 1
         * ServerPublicKey :
         */

        private String ServerRemark;
        private String ServerAddress;
        private int ServerPort;
        private String ServerPublicKey;

        public String getServerRemark() {
            return ServerRemark;
        }

        public void setServerRemark(String ServerRemark) {
            this.ServerRemark = ServerRemark;
        }

        public String getServerAddress() {
            return ServerAddress;
        }

        public void setServerAddress(String ServerAddress) {
            this.ServerAddress = ServerAddress;
        }

        public int getServerPort() {
            return ServerPort;
        }

        public void setServerPort(int ServerPort) {
            this.ServerPort = ServerPort;
        }

        public String getServerPublicKey() {
            return ServerPublicKey;
        }

        public void setServerPublicKey(String ServerPublicKey) {
            this.ServerPublicKey = ServerPublicKey;
        }
    }
}
