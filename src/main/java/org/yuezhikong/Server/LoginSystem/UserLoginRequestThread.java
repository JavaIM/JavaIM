package org.yuezhikong.Server.LoginSystem;

import cn.hutool.crypto.SecureUtil;
import org.yuezhikong.CodeDynamicConfig;
import org.yuezhikong.Server.UserData.user;
import org.yuezhikong.utils.DataBase.Database;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import static org.yuezhikong.Server.api.ServerAPI.SendMessageToUser;
import static org.yuezhikong.Server.Server.DEBUG;

public class UserLoginRequestThread extends Thread{
    private boolean RequestReturn = false;
    private final user RequestUser;
    private String Username;
    private final String Passwd;
    public UserLoginRequestThread(user LoginUser,String UserName,String Password)
    {
        RequestUser = LoginUser;
        Username = UserName;
        Passwd = Password;
    }
    @Override
    public void run() {
        super.run();
        try {
            Connection DatabaseConection = Database.Init(CodeDynamicConfig.GetMySQLDataBaseHost(), CodeDynamicConfig.GetMySQLDataBasePort(), CodeDynamicConfig.GetMySQLDataBaseName(), CodeDynamicConfig.GetMySQLDataBaseUser(), CodeDynamicConfig.GetMySQLDataBasePasswd());
            String sql = "select * from UserData where UserName = ?";
            PreparedStatement ps = DatabaseConection.prepareStatement(sql);
            ps.setString(1,Username);
            ResultSet rs = ps.executeQuery();
            String salt;
            String sha256;
            while (rs.next())
            {
                if (rs.getInt("UserLogged") == 1)
                {
                    break;
                }
                salt = rs.getString("salt");
                sha256 = SecureUtil.sha256(Passwd + salt);
                if (rs.getString("Passwd").equals(sha256))
                {
                    Username = rs.getString("UserName");
                    int PermissionLevel = rs.getInt("Permission");
                    if (PermissionLevel != 0)
                    {
                        if (PermissionLevel != 1)
                        {
                            if (PermissionLevel != -1)
                            {
                                PermissionLevel = 0;
                            }
                            else
                            {
                                SendMessageToUser(RequestUser,"您的账户已被永久封禁！");
                                RequestReturn = false;
                                DatabaseConection.close();
                                return;
                            }
                        }
                    }
                    long muted = rs.getLong("UserMuted");
                    long MuteTime = rs.getLong("UserMuteTime");
                    if (muted == 1)
                    {
                        RequestUser.setMuteTime(MuteTime);
                        RequestUser.setMuted(true);
                    }
                    RequestReturn = true;
                    RequestUser.SetUserPermission(PermissionLevel,true);
                    RequestUser.UserLogin(Username);
                    sql = "UPDATE UserData SET UserLogged = 1 where UserName = ?;";
                    ps = DatabaseConection.prepareStatement(sql);
                    ps.setString(1,Username);
                    ps.executeUpdate();
                    DatabaseConection.close();
                    return;
                }
            }
            DatabaseConection.close();
            RequestReturn = false;
        }
        catch (ClassNotFoundException e)
        {
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            e.printStackTrace(pw);
            pw.flush();
            sw.flush();
            DEBUG.debug(sw.toString());
            pw.close();
            try {
                sw.close();
            }
            catch (IOException ex)
            {
                ex.printStackTrace();
            }
            DEBUG.fatal("ClassNotFoundException，无法找到MySQL驱动");
            DEBUG.fatal("程序已崩溃");
            System.exit(-2);
            RequestReturn = false;
        }
        catch (SQLException e)
        {
            org.yuezhikong.utils.SaveStackTrace.saveStackTrace(e);
            RequestReturn = false;
        }
    }

    public boolean GetReturn() {
        return RequestReturn;
    }
}
