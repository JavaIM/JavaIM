package org.yuezhikong;

import java.io.IOException;
import java.util.Scanner;

public class Main {
    public static void main(String[] args) throws IOException {
        System.out.print("使用客户端模式请输入1，服务端模式请输入2:");
        Scanner sc = new Scanner(System.in);
        int mode = sc.nextInt();
        if (mode == 1) {
            Scanner sc2 = new Scanner(System.in);
            Scanner sc3 = new Scanner(System.in);
            String serverName = null;
            System.out.print("请输入要连接的主机:");
            serverName = sc2.nextLine();
            System.out.print("请输入端口:");
            int port = Integer.parseInt(sc3.nextLine());
            new Client(serverName,port);
        } else if (mode == 2) {
            Scanner sc4 = new Scanner(System.in);
            System.out.print("请输入监听端口:");
            int port = Integer.parseInt(sc4.nextLine());
            new Server(port);
        }
        else {
            System.out.print("输入值错误，请重新运行程序");
        }
    }
}