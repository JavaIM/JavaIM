package org.yuezhikong;

import org.jetbrains.annotations.NotNull;
import org.yuezhikong.newClient.ClientMain;
import org.yuezhikong.newServer.ServerMain;
import org.yuezhikong.utils.Logger;
import org.yuezhikong.utils.SaveStackTrace;

import java.util.Scanner;

public class ExpVersionCode {
    public void run(@NotNull Logger logger)
    {
        try {
            logger.info("请输入想选择的模式");
            logger.info("1:服务端");
            logger.info("2:客户端");
            Scanner scanner = new Scanner(System.in);
            int UserInput = scanner.nextInt();
            if (UserInput == 1)
            {
                logger.info("请输入绑定的端口");
                new ServerMain().start(scanner.nextInt());
                System.exit(0);
            }
            else if (UserInput == 2)
            {
                logger.info("请输入ip");
                scanner.nextLine();
                String Address = scanner.nextLine();
                logger.info("请输入端口");
                new ClientMain().start(Address,scanner.nextInt());
                System.exit(0);
            }
        } catch (Throwable throwable)
        {
            SaveStackTrace.saveStackTrace(throwable);
        }
    }
}
