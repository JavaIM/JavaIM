package org.yuezhikong.Server.UserData;

public class userInformation {
    private int Permission;
    private String UserName;
    private String Passwd;
    private String salt;
    private String token;

    public userInformation() {}
    public userInformation(int Permission, String UserName, String Passwd, String salt, String token)
    {
        this.Permission = Permission;
        this.UserName = UserName;
        this.Passwd = Passwd;
        this.salt = salt;
        this.token = token;
    }

    public int getPermission() {
        return Permission;
    }

    public String getUserName() {
        return UserName;
    }

    public String getPasswd() {
        return Passwd;
    }

    public String getSalt() {
        return salt;
    }

    public String getToken() {
        return token;
    }

    public void setPasswd(String passwd) {
        Passwd = passwd;
    }

    public void setPermission(int permission) {
        Permission = permission;
    }

    public void setSalt(String salt) {
        this.salt = salt;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public void setUserName(String userName) {
        UserName = userName;
    }
}
