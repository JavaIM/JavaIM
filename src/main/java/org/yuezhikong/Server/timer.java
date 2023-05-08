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
    TimerAllMinute Timer2 = null;
    public void Start() {
        if (Timer1 != null)
        {
            Timer1.cancel();
        }
        if (Timer2 != null)
        {
            Timer2.cancel();
        }
        Timer timer = new Timer(true);
        Timer1 = new TimerAllSecond();
        timer.schedule(Timer1,0,1000);

        Timer timer1 = new Timer(true);
        Timer2 = new TimerAllMinute();
        timer1.schedule(Timer2,60000);

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
    public static class TimerAllMinute extends TimerTask {
        @Override
        public void run()
        {
            //每分钟执行的代码
            Thread thread = new Thread(()->{
                //刷新命令列表
                try {
                    List<CustomVar.CommandInformation> PluginSetCommands = new ArrayList<>();
                    for (Plugin plugin : PluginManager.getInstance("./plugins").getPluginList())
                    {
                        if (plugin.getRegisteredCommand().size() > 0)
                        {
                            PluginSetCommands.addAll(plugin.getRegisteredCommand());
                        }
                    }
                    Server.GetInstance().PluginSetCommands = PluginSetCommands;
                } catch (ModeDisabledException ignore) {
                }
            });
            thread.setName("Flush Command List Thread");
            thread.start();
        }
    }
    public void cancel()
    {
        if (Timer1 != null) {
            Timer1.cancel();
            Timer1 = null;
        }
        if (Timer2 != null) {
            Timer2.cancel();
            Timer2 = null;
        }
    }
}
