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

        BufferedWriter writer = null;
        BufferedReader reader = null;
        BufferedReader consoleReader = null;

        while (true) {
            reader = new BufferedReader(new InputStreamReader(client.getInputStream()));
            writer = new BufferedWriter(new OutputStreamWriter(client.getOutputStream()));
            // 等待用户输入信息
            consoleReader = new BufferedReader(new InputStreamReader(System.in));
            String input = consoleReader.readLine();
            // 发送消息给服务器
            writer.write(input + "\n");
            writer.flush();
            // 读取服务器返回的消息
            String msg = reader.readLine();
            System.out.println(msg);
        }
    }
}
