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
