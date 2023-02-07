package org.yuezhikong.Server;

import org.yuezhikong.utils.CustomExceptions.ModeDisabledException;

import java.io.IOException;
import java.net.Socket;
import static org.yuezhikong.config.GetRSA_Mode;

public class user {
    private String UserName = "";
    private boolean UserLogined;
    private Socket UserSocket;
    private final int ClientID;
    private String UserPublicKey;
    private boolean PublicKeyChanged = false;
    public user(Socket socket,int clientid)
    {
        UserSocket = socket;
        ClientID = clientid;
        UserLogined = false;
    }
    public String GetUserName()
    {
        return UserName;
    }
    public boolean GetUserLogined()
    {
        return UserLogined;
    }
    public void UserLogin(String Username)
    {
        UserName = Username;
        UserLogined = true;
    }

    /**
     * 设置用户的公钥
     * @param UserPublickey 用户的公钥
     * @throws ModeDisabledException RSA功能被禁用时抛出此异常
     */
    public void SetUserPublicKey(String UserPublickey) throws ModeDisabledException {
        if (!GetRSA_Mode())
        {
            throw new ModeDisabledException("RSA Mode Has Disabled!");
        }
        if (!PublicKeyChanged) {
            UserPublicKey = UserPublickey;
            PublicKeyChanged = true;
        }
    }
    public void UserDisconnect()
    {
        try {
            UserSocket.close();
        } catch (IOException e)
        {
            e.printStackTrace();
        }
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
