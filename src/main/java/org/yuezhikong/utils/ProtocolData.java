package org.yuezhikong.utils;

/**
 * 接受/发送的json的序列化/反序列化流程
 * 如果修改了protocol，请使用GsonFormat插件直接替换
 */
public class ProtocolData {

    private MessageHead MessageHead;
    private MessageBody MessageBody;

    public MessageHead getMessageHead() {
        return MessageHead;
    }

    public void setMessageHead(MessageHead MessageHead) {
        this.MessageHead = MessageHead;
    }

    public MessageBody getMessageBody() {
        return MessageBody;
    }

    public void setMessageBody(MessageBody MessageBody) {
        this.MessageBody = MessageBody;
    }

    public static class MessageHead {
        /**
         * Version : 0
         * type :
         */

        private int Version;
        private String type;

        public int getVersion() {
            return Version;
        }

        public void setVersion(int Version) {
            this.Version = Version;
        }

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }
    }

    public static class MessageBody {
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
