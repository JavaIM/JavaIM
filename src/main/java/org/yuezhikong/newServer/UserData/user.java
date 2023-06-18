package org.yuezhikong.newServer.UserData;

import cn.hutool.crypto.symmetric.AES;
import org.yuezhikong.newServer.ServerMain.RecvMessageThread;

import java.net.Socket;
@SuppressWarnings("unused")
public class user {
    private String UserName;
    private String PublicKey;
    private final int ClientID;
    private Socket UserSocket;
    private boolean UserLogined;
    private RecvMessageThread recvMessageThread;
    private AES UserAES;
    public user(Socket socket, int ClientID)
    {
        UserSocket = socket;
        this.ClientID = ClientID;
        UserLogined = false;
    }
    public void setRecvMessageThread(RecvMessageThread thread)
    {
        recvMessageThread = thread;
    }

    public RecvMessageThread getRecvMessageThread() {
        return recvMessageThread;
    }

    public Socket getUserSocket() {
        return UserSocket;
    }

    public void setPublicKey(String publicKey) {
        PublicKey = publicKey;
    }

    public String getPublicKey() {
        return PublicKey;
    }

    public void setUserAES(AES userAES) {
        UserAES = userAES;
    }

    public AES getUserAES() {
        return UserAES;
    }

    public int getClientID() {
        return ClientID;
    }

    public String getUserName() {
        return UserName;
    }
    public void UserLogin(String UserName)
    {
        this.UserName = UserName;
        UserLogined = true;
    }

    public boolean isUserLogined() {
        return UserLogined;
    }

    public void UserDisconnect() {
        recvMessageThread.interrupt();
        UserSocket = null;
        UserName = null;
        PublicKey = null;
        UserLogined = false;
        UserAES = null;
    }

    //被暂缓的服务端管理功能
    public void setMuteTime(long muteTime) {
    }

    public void setMuted(boolean Muted) {
    }

    public void SetUserPermission(int permissionLevel, boolean FlashPermission) {
    }
}
