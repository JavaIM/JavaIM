package org.yuezhikong.Server;

import cn.hutool.crypto.SecureUtil;
import org.yuezhikong.config;
import org.yuezhikong.utils.DataBase.MySQL;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;

public class UserRegisterRequestThread extends Thread{
    private boolean RequestReturn;
    private final user user;
    private String Username;
    private final String Passwd;
    public boolean GetReturn() {
        return RequestReturn;
    }

    public UserRegisterRequestThread(user RequestUser,String username,String Password)
    {
        user = RequestUser;
        Username = username;
        Passwd = Password;
    }
    @Override
    public void run() {
        super.run();
        String salt = UUID.randomUUID().toString();
        String sha256 = SecureUtil.sha256(Passwd + salt);
        try {
            Connection mySQLConnection = MySQL.GetMySQLConnection(config.GetMySQLDataBaseHost(), config.GetMySQLDataBasePort(), config.GetMySQLDataBaseName(), config.GetMySQLDataBaseUser(), config.GetMySQLDataBasePasswd());
            String sql = "select * from UserData where UserName = ?";
            PreparedStatement ps = mySQLConnection.prepareStatement(sql);
            ps.setString(1,Username);
            ResultSet rs  = ps.executeQuery();
            if (rs.next())
            {
                RequestReturn = false;
                return;
            }
            sql = "INSERT INTO `UserData` (`UserName`, `Passwd`,`salt`) VALUES (?, ?, ?);";
            ps = mySQLConnection.prepareStatement(sql);
            ps.setString(1,Username);
            ps.setString(2,sha256);
            ps.setString(3,salt);
            ps.executeUpdate();
            RequestReturn = true;
            Username = rs.getString("UserName");
            user.UserLogin(Username);
        } catch (ClassNotFoundException | SQLException e) {
            e.printStackTrace();
        }

    }
}
