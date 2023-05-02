package org.yuezhikong.GUITest;

import org.yuezhikong.Client;
import org.yuezhikong.GUITest.ClientGUI.Controller;
import org.yuezhikong.utils.Logger;
import org.yuezhikong.utils.SaveStackTrace;

import java.io.IOException;

public class GUIClient extends Client {
    private static Controller ClientGUI;

    @Override
    protected void PublicKeyLack() {
        Logger.logger_root.fatal("在运行目录下未找到ServerPublicKey.key");
        Logger.logger_root.fatal("此文件为服务端公钥文件，用于保证通信安全");
        Logger.logger_root.fatal("由于此文件缺失，客户端即将停止运行");
        ClientGUI.ClientStartFailedbyServerPublicKeyLack();
    }

    @Override
    protected void LoggerInit() {
        super.logger = new Logger(false,true,null,ClientGUI);
    }

    /**
     * 获取客户端logger
     * @return logger
     */
    public Logger getLogger()
    {
        return super.logger;
    }

    @Override
    protected void ExitSystem(int code) {
        ClientGUI.ExitSystem(code);
    }

    @Override
    public boolean SendMessageToServer(String input) throws IOException {
        return super.SendMessageToServer(input);
    }

    @Override
    protected void SendMessage() {
        new Thread()
        {
            @Override
            public void run() {
                this.setName("Client Thread");
                try {
                    GUIClient.super.SendMessage();
                } catch (IOException e) {
                    if (!"Connection reset by peer".equals(e.getMessage()) && !"Connection reset".equals(e.getMessage())) {
                        logger.warning("发生I/O错误");
                        SaveStackTrace.saveStackTrace(e);
                    }
                    else
                    {
                        logger.info("连接早已被关闭...");
                    }
                }
            }
        }.start();
    }

    public GUIClient(String serverName, int port) {
        super(serverName, port);
    }

    public static void SetTempClientGUI(Controller ClientGUI)
    {
        GUIClient.ClientGUI = ClientGUI;
    }
}
