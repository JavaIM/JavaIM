package org.yuezhikong;

import java.io.*;
import java.net.Socket;
import java.util.logging.Logger;

public class Client {
    public Client(String serverName, int port) {
        try {
            System.out.println("连接到主机：" + serverName + " ，端口号：" + port);
            Socket client = new Socket(serverName, port);
            System.out.println("远程主机地址：" + client.getRemoteSocketAddress());
            OutputStream outToServer = client.getOutputStream();
            DataOutputStream out = new DataOutputStream(outToServer);
            out.writeUTF("Hello from " + client.getLocalSocketAddress());
            InputStream inFromServer = client.getInputStream();
            DataInputStream in = new DataInputStream(inFromServer);
            System.out.println("服务器响应： " + in.readUTF());

            BufferedWriter writer;
            BufferedReader consoleReader;

            while (true) {
                writer = new BufferedWriter(new OutputStreamWriter(client.getOutputStream()));
                // 等待用户输入信息
                consoleReader = new BufferedReader(new InputStreamReader(System.in));
                String input = consoleReader.readLine();
                // 检查用户输入是否是quit
                if ("quit".equals(input))
                {
                    Logger.getGlobal().info("正在断开连接");
                    writer.write(input + "\n");
                    client.close();
                    break;
                }
                // 发送消息给服务器
                writer.write(input + "\n");
                writer.flush();
            }
            Logger.getGlobal().info("再见~");
        }
        catch (IOException e)
        {
            Logger.getGlobal().warning("发生I/O错误");
            e.printStackTrace();
        }
    }
}
