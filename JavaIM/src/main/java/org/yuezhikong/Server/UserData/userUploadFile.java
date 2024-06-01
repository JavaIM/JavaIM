package org.yuezhikong.Server.UserData;

public class userUploadFile {
    private String userId;
    private String ownFile;
    private String origFileName;

    public userUploadFile(String userId, String ownFile, String origFileName){
        this.userId = userId;
        this.ownFile = ownFile;
        this.origFileName = origFileName;
    }

    public userUploadFile() {}

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getOwnFile() {
        return ownFile;
    }

    public void setOwnFile(String ownFile) {
        this.ownFile = ownFile;
    }

    public String getOrigFileName() {
        return origFileName;
    }

    public void setOrigFileName(String origFileName) {
        this.origFileName = origFileName;
    }
}
