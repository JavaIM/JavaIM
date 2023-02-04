package org.yuezhikong;

import java.net.*;
import java.io.*;

public class Server {
    public Server(int port) {
        ServerSocket serverSocket = null;
        try {
            serverSocket = new ServerSocket(port);
            /* serverSocket.setSoTimeout(10000); */
        } catch (IOException e) {
            e.printStackTrace();
        }
        while (true) {
            try {
                System.out.println("等待远程连接，端口号为：" + serverSocket.getLocalPort() + "...");
                Socket server = serverSocket.accept();
                System.out.println("远程主机地址：" + server.getRemoteSocketAddress());
                DataInputStream in = new DataInputStream(server.getInputStream());
                System.out.println(in.readUTF());
                DataOutputStream out = new DataOutputStream(server.getOutputStream());
                out.writeUTF("服务器连接成功：" + server.getLocalSocketAddress());
                BufferedReader reader = null;
                BufferedWriter writer = null;
                reader = new BufferedReader(new InputStreamReader(server.getInputStream()));
                writer = new BufferedWriter(new OutputStreamWriter(server.getOutputStream()));
                String msg = null;
                while ((msg = reader.readLine()) != null) {
                    // 读取客户端发送的消息
                    System.out.println("客户端[" + server.getInetAddress() + ":" + server.getPort() + "]: " + msg);
                }
            } catch (SocketTimeoutException s) {
                System.out.println("Socket timed out!");
                break;
            } catch (IOException e) {
                e.printStackTrace();
                break;
            }
        }
    }
}