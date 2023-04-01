package org.yuezhikong.utils;

/**
 * 接受/发送的json的反序列化流程
 * 如果修改了protocol，请使用GsonFormat插件直接替换
 */
public class ProtocolData {
    /**
     * MessageHead : {"Version":0,"type":0}
     * MessageBody : {"Message":"","FileLong":0}
     */

    private MessageHeadBean MessageHead;
    private MessageBodyBean MessageBody;

    public MessageHeadBean getMessageHead() {
        return MessageHead;
    }

    public void setMessageHead(MessageHeadBean MessageHead) {
        this.MessageHead = MessageHead;
    }

    public MessageBodyBean getMessageBody() {
        return MessageBody;
    }

    public void setMessageBody(MessageBodyBean MessageBody) {
        this.MessageBody = MessageBody;
    }

    public static class MessageHeadBean {
        /**
         * Version : 0
         * type : 0
         */

        private int Version;
        private int type;

        public int getVersion() {
            return Version;
        }

        public void setVersion(int Version) {
            this.Version = Version;
        }

        public int getType() {
            return type;
        }

        public void setType(int type) {
            this.type = type;
        }
    }

    public static class MessageBodyBean {
        /**
         * Message :
         * FileLong : 0
         */

        private String Message;
        private int FileLong;

        public String getMessage() {
            return Message;
        }

        public void setMessage(String Message) {
            this.Message = Message;
        }

        public int getFileLong() {
            return FileLong;
        }

        public void setFileLong(int FileLong) {
            this.FileLong = FileLong;
        }
    }
}
