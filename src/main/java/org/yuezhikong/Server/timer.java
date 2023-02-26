package org.yuezhikong.Server;

import org.apache.logging.log4j.LogManager;
import org.yuezhikong.Server.UserData.user;
import org.yuezhikong.config;
import org.yuezhikong.utils.DataBase.Database;
import org.yuezhikong.utils.Logger;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

public class timer extends TimerTask
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
                                StringWriter sw = new StringWriter();
                                PrintWriter pw = new PrintWriter(sw);
                                e.printStackTrace(pw);
                                pw.flush();
                                sw.flush();
                                org.apache.logging.log4j.Logger logger_log4j = LogManager.getLogger("Debug");
                                logger_log4j.debug(sw.toString());
                                pw.close();
                                try {
                                    sw.close();
                                }
                                catch (IOException ex)
                                {
                                    ex.printStackTrace();
                                }
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
