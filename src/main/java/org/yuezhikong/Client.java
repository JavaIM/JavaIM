package org.yuezhikong;

import java.io.*;
import java.net.Socket;

public class Client {
    public Client(String serverName, int port) throws IOException {
        System.out.println("连接到主机：" + serverName + " ，端口号：" + port);
        Socket client = new Socket(serverName, port);
        System.out.println("远程主机地址：" + client.getRemoteSocketAddress());
        OutputStream outToServer = client.getOutputStream();
        DataOutputStream out = new DataOutputStream(outToServer);
        out.writeUTF("Hello from " + client.getLocalSocketAddress());
        InputStream inFromServer = client.getInputStream();
        DataInputStream in = new DataInputStream(inFromServer);
        System.out.println("服务器响应： " + in.readUTF());

    }
}
