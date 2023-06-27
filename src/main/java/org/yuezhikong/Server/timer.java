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
package org.yuezhikong.Server;

import org.yuezhikong.CodeDynamicConfig;
import org.yuezhikong.Server.UserData.user;
import org.yuezhikong.Server.api.ServerAPI;
import org.yuezhikong.utils.DataBase.Database;
import org.yuezhikong.utils.Logger;
import org.yuezhikong.utils.SaveStackTrace;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.*;

public class timer
{
    TimerAllSecond Timer1 = null;
    public void Start() {
        if (Timer1 != null)
        {
            Timer1.cancel();
        }
        Timer timer = new Timer(true);
        Timer1 = new TimerAllSecond();
        timer.schedule(Timer1,0,1000);

    }
    public static class TimerAllSecond extends TimerTask{
        @Override
        public void run() {
            //从这里开始写时间事件
            for (user ProcessUser : ServerAPI.GetValidClientList(Server.GetInstance(),true))
            {
                if (ProcessUser.isMuted()) {
                    Date date = new Date();
                    long Time = date.getTime();//获取当前时间毫秒数
                    if (ProcessUser.getMuteTime() <= Time) {
                        ProcessUser.setMuteTime(0);
                        ProcessUser.setMuted(false);
                        Runnable SQLUpdateThread = () -> {
                            try {
                                Connection DatabaseConnection = Database.Init(CodeDynamicConfig.GetMySQLDataBaseHost(), CodeDynamicConfig.GetMySQLDataBasePort(), CodeDynamicConfig.GetMySQLDataBaseName(), CodeDynamicConfig.GetMySQLDataBaseUser(), CodeDynamicConfig.GetMySQLDataBasePasswd());
                                String sql = "UPDATE UserData SET UserMuted = 0 and UserMuteTime = 0 where UserName = ?";
                                PreparedStatement ps = DatabaseConnection.prepareStatement(sql);
                                ps.setString(1, ProcessUser.GetUserName());
                                ps.executeUpdate();
                                DatabaseConnection.close();
                            } catch (Database.DatabaseException | SQLException e) {
                                SaveStackTrace.saveStackTrace(e);
                            } finally {
                                Database.close();
                            }
                        };
                        Thread UpdateThread = new Thread(SQLUpdateThread);
                        UpdateThread.start();
                        UpdateThread.setName("SQL Update Thread");
                        try {
                            UpdateThread.join();
                        } catch (InterruptedException e) {
                            Logger logger = Server.GetInstance().logger;
                            logger.error("发生异常InterruptedException");
                            SaveStackTrace.saveStackTrace(e);
                        }
                    }
                }
            }
        }
    }
    public void cancel()
    {
        if (Timer1 != null) {
            Timer1.cancel();
            Timer1 = null;
        }
    }
}
