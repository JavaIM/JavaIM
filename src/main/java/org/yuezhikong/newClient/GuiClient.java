package org.yuezhikong.newClient;

import cn.hutool.crypto.symmetric.AES;
import org.yuezhikong.utils.CustomVar;
import org.yuezhikong.utils.Logger;

import java.net.Socket;

public class GuiClient extends ClientMain{
    private CustomVar.KeyData keyData;
    private static ClientMain Instance;
    private String Address;
    @Override
    public void start(String ServerAddress,int ServerPort){
        super.start(ServerAddress,ServerPort);
    }
    public void SendMessage(Logger logger, Socket socket, AES aes) {
        new Thread()
        {
            @Override
            public void run()
            {
                GuiClient.super.SendMessage(logger, socket, aes);
            }
        };
    }
}
