/*
 * Simplified Chinese (简体中文)
 *
 * 版权所有 (C) 2023 QiLechan <qilechan@outlook.com> 和本程序的贡献者
 *
 * 本程序是自由软件：你可以再分发之和/或依照由自由软件基金会发布的 GNU 通用公共许可证修改之，无论是版本 3 许可证，还是 3 任何以后版都可以。
 * 发布该程序是希望它能有用，但是并无保障;甚至连可销售和符合某个特定的目的都不保证。请参看 GNU 通用公共许可证，了解详情。
 * 你应该随程序获得一份 GNU 通用公共许可证的副本。如果没有，请看 <https://www.gnu.org/licenses/>。
 * English (英语)
 *
 * Copyright (C) 2023 QiLechan <qilechan@outlook.com> and contributors to this program
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or 3 any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program. If not, see <https://www.gnu.org/licenses/>.
 */
package org.yuezhikong.GUITest;

import org.jetbrains.annotations.NotNull;
import org.yuezhikong.Client;
import org.yuezhikong.GUITest.ClientGUI.Controller;
import org.yuezhikong.utils.CustomVar;
import org.yuezhikong.utils.Logger;
import org.yuezhikong.utils.SaveStackTrace;

import java.io.IOException;

public class GUIClient extends Client {
    private static Controller ClientGUI;
    private static GUIClient Instance;

    @Override
    protected void PublicKeyLack() {
        super.PublicKeyLack();
        ClientGUI.ClientStartFailedbyServerPublicKeyLack();
    }

    @Override
    protected void LoggerInit() {
        super.logger = new Logger(false,true,null,ClientGUI);
    }

    @Override
    protected void GetUserNameAndUserPassword() {
        ClientGUI.GetUserNameAndPassword();
    }

    @Override
    public boolean SendMessageToServer(@NotNull String input) throws IOException {
        return super.SendMessageToServer(input);
    }

    public void UserNameAndPasswordReCall(CustomVar.UserAndPassword userAndPassword)
    {
        super.LoginCallback(userAndPassword);
    }

    /**
     * 获取客户端logger
     * @return logger
     */
    public Logger getLogger()
    {
        return super.logger;
    }

    public static GUIClient getInstance() {
        return Instance;
    }
    @Override
    protected void ExitSystem(int code) {
        try {
            super.client.close();
        } catch (IOException e) {
            SaveStackTrace.saveStackTrace(e);
        }
        ClientGUI.ExitSystem(code);
    }

    public void quit()
    {
        Thread thread = new Thread(() -> {
            try {
                GUIClient.super.client.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
        thread.setName("Request User Thread");
        thread.start();
        try {
            thread.join();
        } catch (InterruptedException ignored) {
        }
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
        Instance = this;
    }

    public static void SetTempClientGUI(@NotNull Controller ClientGUI)
    {
        GUIClient.ClientGUI = ClientGUI;
    }
}
