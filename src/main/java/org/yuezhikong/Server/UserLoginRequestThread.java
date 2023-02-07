package org.yuezhikong.Server;

import cn.hutool.crypto.SecureUtil;
import org.apache.maven.artifact.DependencyResolutionRequiredException;
import org.yuezhikong.config;
import org.yuezhikong.utils.DataBase.MySQL;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import static org.yuezhikong.Server.newServer.logger_log4j;

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
            Connection mySQLConnection = MySQL.GetMySQLConnection(config.GetMySQLDataBaseHost(), config.GetMySQLDataBasePort(), config.GetMySQLDataBaseName(), config.GetMySQLDataBaseUser(), config.GetMySQLDataBasePasswd());
            String sql = "select * from UserData where UserName = ?";
            PreparedStatement ps = mySQLConnection.prepareStatement(sql);
            ps.setString(1,Username);
            ResultSet rs = ps.executeQuery();
            String sait = "Not";
            String sha256 = Passwd;
            if (rs.next()) {
                sait = rs.getString("sait");
                sha256 = SecureUtil.sha256(Passwd + sait);
            }
            if (sait.equals("Not"))
            {
                RequestReturn = false;
                return;
            }
            if (sha256.equals(Passwd))
            {
                RequestReturn = false;
                return;
            }
            sql = "select * from UserData where UserName = ? and Passwd = ? and sait = ?";
            ps = mySQLConnection.prepareStatement(sql);
            ps.setString(1,Username);
            ps.setString(2,sha256);
            ps.setString(3,sait);
            rs = ps.executeQuery();
            if (rs.next())
            {
                RequestReturn = true;
                Username = rs.getString("UserName");
                RequestUser.UserLogin(Username);
            }
        }
        catch (ClassNotFoundException e)
        {
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            e.printStackTrace(pw);
            pw.flush();
            sw.flush();
            logger_log4j.debug(sw.toString());
            pw.close();
            try {
                sw.close();
            }
            catch (IOException ex)
            {
                e.printStackTrace();
            }
            logger_log4j.fatal("ClassNotFoundException，无法找到MySQL驱动");
            logger_log4j.fatal("程序已崩溃");
            System.exit(-2);
            RequestReturn = false;
        }
        catch (SQLException e)
        {
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            e.printStackTrace(pw);
            pw.flush();
            sw.flush();
            logger_log4j.debug(sw.toString());
            pw.close();
            try {
                sw.close();
            }
            catch (IOException ex)
            {
                e.printStackTrace();
            }
            RequestReturn = false;
        }
    }

    public boolean GetReturn() {
        return RequestReturn;
    }
}