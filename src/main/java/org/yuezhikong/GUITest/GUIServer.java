package org.yuezhikong.GUITest;

import org.yuezhikong.GUITest.ServerGUI.Controller;
import org.yuezhikong.Server.Server;
import org.yuezhikong.utils.Logger;

public class GUIServer extends Server {
    private static Controller GUIForServer;

    @Override
    protected void SetupLoggerSystem() {
        super.logger = new Logger(true,false,GUIForServer);
    }

    @Override
    protected void StartCommandSystem() {
        new Thread()
        {
            @Override
            public void run() {
                this.setName("Server Thread");
                GUIServer.super.StartCommandSystem();
            }
        }.start();
    }

    /**
     * 初始化GUI服务端
     * @param port 要开始监听的端口
     */
    public GUIServer(int port) {
        super(port);
    }

    /**
     * 请在初始化前调用
     * @param GUIForServer 服务端GUI实例
     */
    public static void SetTempServerGUI(Controller GUIForServer) {
        GUIServer.GUIForServer = GUIForServer;
    }
}
