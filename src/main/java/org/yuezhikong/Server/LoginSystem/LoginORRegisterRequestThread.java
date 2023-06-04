package org.yuezhikong.Server.LoginSystem;

import cn.hutool.crypto.SecureUtil;
import org.jetbrains.annotations.NotNull;
import org.yuezhikong.CodeDynamicConfig;
import org.yuezhikong.Server.UserData.user;
import org.yuezhikong.Server.api.ServerAPI;
import org.yuezhikong.utils.DataBase.Database;
import org.yuezhikong.utils.SaveStackTrace;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;

import static org.yuezhikong.Server.Server.DEBUG;
import static org.yuezhikong.Server.api.ServerAPI.SendMessageToUser;

public class LoginORRegisterRequestThread extends Thread{
    private final String UserName;
    private final String Passwd;
    private boolean Success;
    private final user RequestUser;
    public LoginORRegisterRequestThread(@NotNull String UserName, @NotNull String Passwd, @NotNull user RequestUser)
    {
        this.UserName = UserName;
        this.Passwd = Passwd;
        this.Success = false;
        this.RequestUser = RequestUser;
    }
    @Override
    public void run() {
        this.setUncaughtExceptionHandler((t, e) -> SaveStackTrace.saveStackTrace(e));
        this.setName("SQL Request Thread");
        if ("Server".equals(UserName))
        {
            Success = false;
            ServerAPI.SendMessageToUser(RequestUser,"不得使用被禁止的用户名：Server");

            return;
        }
        try {
            Connection DatabaseConnection = Database.Init(CodeDynamicConfig.GetMySQLDataBaseHost(), CodeDynamicConfig.GetMySQLDataBasePort(), CodeDynamicConfig.GetMySQLDataBaseName(), CodeDynamicConfig.GetMySQLDataBaseUser(), CodeDynamicConfig.GetMySQLDataBasePasswd());
            String sql = "select * from UserData where UserName = ?";
            PreparedStatement ps = DatabaseConnection.prepareStatement(sql);
            ps.setString(1,UserName);
            ResultSet rs = ps.executeQuery();
            if (rs.next())
            {
                String salt;
                String sha256;
                if (rs.getInt("UserLogged") == 1)
                {
                    Success = false;
                    DatabaseConnection.close();
                    return;
                }
                salt = rs.getString("salt");
                sha256 = SecureUtil.sha256(Passwd + salt);
                if (rs.getString("Passwd").equals(sha256))
                {
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
                                Success = false;
                                DatabaseConnection.close();
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
                    Success = true;
                    RequestUser.SetUserPermission(PermissionLevel,true);
                    RequestUser.UserLogin(UserName);
                    sql = "UPDATE UserData SET UserLogged = 1 where UserName = ?;";
                    ps = DatabaseConnection.prepareStatement(sql);
                    ps.setString(1,UserName);
                    ps.executeUpdate();
                    DatabaseConnection.close();
                }
            }
            else
            {
                String salt;
                do {
                    salt = UUID.randomUUID().toString();
                    sql = "select * from UserData where salt = ?";
                    ps = DatabaseConnection.prepareStatement(sql);
                    ps.setString(1, sql);
                    rs = ps.executeQuery();
                } while (!rs.next());
                String sha256 = SecureUtil.sha256(Passwd + salt);
                sql = "INSERT INTO `UserData` (`Permission`,`UserName`, `Passwd`,`salt`) VALUES (0,?, ?, ?);";
                ps = DatabaseConnection.prepareStatement(sql);
                ps.setString(1,UserName);
                ps.setString(2,sha256);
                ps.setString(3,salt);
                ps.executeUpdate();
                DatabaseConnection.close();
                Success = true;
                RequestUser.UserLogin(UserName);
            }
        } catch (ClassNotFoundException e)
        {
            RequestUser.UserDisconnect();
            SaveStackTrace.saveStackTrace(e);
            DEBUG.fatal("ClassNotFoundException，无法找到MySQL驱动");
            DEBUG.fatal("程序已崩溃");
            System.exit(-2);
            Success = false;
        }
        catch (SQLException e)
        {
            RequestUser.UserDisconnect();
            SaveStackTrace.saveStackTrace(e);
            Success = false;
        }
    }

    public boolean isSuccess() {
        return Success;
    }
}
