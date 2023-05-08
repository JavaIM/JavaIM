package org.yuezhikong.Server;

import org.yuezhikong.CodeDynamicConfig;
import org.yuezhikong.Server.UserData.user;
import org.yuezhikong.Server.plugin.Plugin;
import org.yuezhikong.Server.plugin.load.PluginManager;
import org.yuezhikong.utils.CustomExceptions.ModeDisabledException;
import org.yuezhikong.utils.CustomVar;
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
            int i = 0;
            int tmpClientIDAll = Server.GetInstance().getClientIDAll();
            tmpClientIDAll = tmpClientIDAll - 1;
            while (true) {
                if (i > tmpClientIDAll) {
                    break;
                }
                user ProcessUser = Server.GetInstance().getUsers().get(i);
                if (ProcessUser.GetUserSocket() == null) {
                    i = i + 1;
                    continue;
                }
                if (ProcessUser.GetUserLogined()) {
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
                                } catch (ClassNotFoundException | SQLException e) {
                                    SaveStackTrace.saveStackTrace(e);
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
                if (i == tmpClientIDAll) {
                    break;
                }
                i = i + 1;
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
