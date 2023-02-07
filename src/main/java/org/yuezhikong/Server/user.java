package org.yuezhikong.Server;

import java.net.Socket;
import static org.yuezhikong.config.GetRSA_Mode;

public class user {
    private Socket UserSocket;
    private final int ClientID;
    private String UserPublicKey;
    private boolean PublicKeyChanged = false;
    public user(Socket socket,int clientid)
    {
        UserSocket = socket;
        ClientID = clientid;
    }
    public void SetUserPublicKey(String UserPublickey)
    {
        if (!GetRSA_Mode())
        {
            throw new RuntimeException("RSA Mode Has Disabled!");
        }
        if (!PublicKeyChanged) {
            UserPublicKey = UserPublickey;
            PublicKeyChanged = true;
        }
    }
    public void UserDisconnect()
    {
        UserSocket = null;
        UserPublicKey = null;
    }
    public Socket GetUserSocket()
    {
        return UserSocket;
    }
    public int GetUserClientID()
    {
        return ClientID;
    }
    public String GetUserPublicKey() throws Exception {
        if (!GetRSA_Mode())
        {
            throw new Exception("RSA Mode Has Disabled!");
        }
        if (UserPublicKey != null) {
            return UserPublicKey;
        }
        else
        {
            return null;
        }
    }
}
