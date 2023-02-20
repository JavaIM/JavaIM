package org.yuezhikong.Server.utils;

import org.yuezhikong.Server.newServer;
import org.yuezhikong.Server.user;
import org.yuezhikong.config;
import org.yuezhikong.utils.DataBase.Database;
import org.yuezhikong.utils.Logger;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

public class time_event extends TimerTask
{
    @Override
    public boolean cancel() {
        return super.cancel();
    }

    public void Start() {
        Timer timer = new Timer(true);
        timer.schedule(this,0,1000);
    }
    @Override
    public void run() {
        //从这里开始写时间事件
        int i = 0;
        int tmpClientIDAll = newServer.GetInstance().getClientIDAll();
        tmpClientIDAll = tmpClientIDAll - 1;
        while (true) {
            if (i > tmpClientIDAll) {
                break;
            }
            user ProcessUser = newServer.GetInstance().getUsers().get(i);
            if (ProcessUser.GetUserSocket() == null) {
                i = i + 1;
                continue;
            }
            if (ProcessUser.GetUserLogined())
            {
                if (ProcessUser.isMuted())
                {
                    Date date = new Date();
                    long Time = date.getTime();//获取当前时间毫秒数
                    if (ProcessUser.getMuteTime() <= Time)
                    {
                        ProcessUser.setMuteTime(0);
                        ProcessUser.setMuted(false);
                        Runnable SQLUpdateThread = () -> {
                            try {
                                Connection mySQLConnection = Database.Init(config.GetMySQLDataBaseHost(), config.GetMySQLDataBasePort(), config.GetMySQLDataBaseName(), config.GetMySQLDataBaseUser(), config.GetMySQLDataBasePasswd());
                                String sql = "UPDATE UserData SET UserMuted = 0 and UserMuteTime = 0 where UserName = ?";
                                PreparedStatement ps = mySQLConnection.prepareStatement(sql);
                                ps.setString(1,ProcessUser.GetUserName());
                                ps.executeUpdate();
                                mySQLConnection.close();
                            } catch (ClassNotFoundException | SQLException e) {
                                e.printStackTrace();
                            }
                        };
                        Thread UpdateThread = new Thread(SQLUpdateThread);
                        UpdateThread.start();
                        UpdateThread.setName("SQL Update Thread");
                        try {
                            UpdateThread.join();
                        } catch (InterruptedException e) {
                            Logger logger = new Logger();
                            logger.error("发生异常InterruptedException");
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
