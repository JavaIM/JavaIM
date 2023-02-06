package org.yuezhikong.Server;

import org.yuezhikong.utils.Logger;
import org.yuezhikong.utils.RSA;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.security.PublicKey;

import static org.yuezhikong.config.GetDebugMode;

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
    public boolean SetUserPublicKey(String UserPublickey)
    {
        if (!PublicKeyChanged) {
            UserPublicKey = UserPublickey;
            PublicKeyChanged = true;
            return true;
        }
        else
        {
            return false;
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
    public PublicKey GetUserPublicKey() throws Exception {
        if (UserPublicKey != null) {
            if (GetDebugMode()) {
                File file = new File("tmp");
                if (!file.exists()) {
                    if (!file.mkdirs()) {
                        return null;
                    }
                }
                file = new File(file.getPath() + "/tmp.key");
                if (!file.exists()) {
                    if (!file.createNewFile()) {
                        if (!new File("tmp").delete()) {
                            System.out.print("\b");
                            Logger.logger_root.fatal("无法删除临时文件夹tmp");
                            Thread.sleep(50);
                            System.out.print(">");
                        }
                        return null;
                    }
                }
                OutputStream os;
                os = new FileOutputStream(file);
                DataOutputStream out = new DataOutputStream(os);
                out.writeUTF(UserPublicKey);
                out.flush();
                out.close();
                os.flush();
                os.close();
                PublicKey publicKey = RSA.loadPublicKeyFromFile(file.getPath());
                boolean a = file.delete();
                boolean b = new File("tmp").delete();
                if (!a) {
                    System.out.print("\b");
                    Logger.logger_root.fatal("无法删除临时tmp.key");
                    Thread.sleep(50);
                    System.out.print(">");
                }
                if (!b) {
                    System.out.print("\b");
                    Logger.logger_root.fatal("无法删除临时文件夹tmp");
                    Thread.sleep(50);
                    System.out.print(">");
                }
                return publicKey;
            }
            else
                return RSA.loadPublicKeyFromString(UserPublicKey);
        }
        else
        {
            return null;
        }
    }
}
