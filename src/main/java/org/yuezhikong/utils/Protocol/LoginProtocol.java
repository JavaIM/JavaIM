package org.yuezhikong.utils.Protocol;
/**
 * 接受/发送的json的序列化/反序列化流程
 * <p>如果修改了protocol，请使用GsonFormat插件直接替换</p>
 */
public class LoginProtocol {

    /**
     * LoginPacketHead : {"type":""}
     * LoginPacketBody : {"ReLogin":{"Token":""},"NormalLogin":{"UserName":"","Passwd":""}}
     */

    private LoginPacketHeadBean LoginPacketHead;
    private LoginPacketBodyBean LoginPacketBody;

    public LoginPacketHeadBean getLoginPacketHead() {
        return LoginPacketHead;
    }

    public void setLoginPacketHead(LoginPacketHeadBean LoginPacketHead) {
        this.LoginPacketHead = LoginPacketHead;
    }

    public LoginPacketBodyBean getLoginPacketBody() {
        return LoginPacketBody;
    }

    public void setLoginPacketBody(LoginPacketBodyBean LoginPacketBody) {
        this.LoginPacketBody = LoginPacketBody;
    }

    public static class LoginPacketHeadBean {
        /**
         * type :
         */

        private String type;

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }
    }

    public static class LoginPacketBodyBean {
        /**
         * ReLogin : {"Token":""}
         * NormalLogin : {"UserName":"","Passwd":""}
         */

        private ReLoginBean ReLogin;
        private NormalLoginBean NormalLogin;

        public ReLoginBean getReLogin() {
            return ReLogin;
        }

        public void setReLogin(ReLoginBean ReLogin) {
            this.ReLogin = ReLogin;
        }

        public NormalLoginBean getNormalLogin() {
            return NormalLogin;
        }

        public void setNormalLogin(NormalLoginBean NormalLogin) {
            this.NormalLogin = NormalLogin;
        }

        public static class ReLoginBean {
            /**
             * Token :
             */

            private String Token;

            public String getToken() {
                return Token;
            }

            public void setToken(String Token) {
                this.Token = Token;
            }
        }

        public static class NormalLoginBean {
            /**
             * UserName :
             * Passwd :
             */

            private String UserName;
            private String Passwd;

            public String getUserName() {
                return UserName;
            }

            public void setUserName(String UserName) {
                this.UserName = UserName;
            }

            public String getPasswd() {
                return Passwd;
            }

            public void setPasswd(String Passwd) {
                this.Passwd = Passwd;
            }
        }
    }
}
