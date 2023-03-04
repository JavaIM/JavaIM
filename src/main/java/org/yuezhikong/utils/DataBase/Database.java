package org.yuezhikong.utils.DataBase;

import org.yuezhikong.config;

import java.sql.*;

public class Database {
    /**
     * 初始化数据库连接
     * @param host 如果为MySQL连接，那么为MySQL地址
     * @param port 如果为MySQL连接，那么为MySQL端口
     * @param Database 如果为MySQL连接，那么为MySQL数据库名称
     * @param UserName 如果为MySQL连接，那么为MySQL用户名
     * @param Password 如果为MySQL连接，那么为MySQL密码
     * @throws ClassNotFoundException 找不到数据库驱动
     * @throws SQLException 获取数据库连接时连接失败
     * @return 数据库连接
     */
    public static Connection Init(String host, String port, String Database, String UserName, String Password) throws ClassNotFoundException, SQLException {
        if (!config.GetSQLITEMode()) {
            Class.forName("com.mysql.cj.jdbc.Driver");
            Connection DatabaseConnection = DriverManager.getConnection("jdbc:mysql://" + host + ":" + port + "/" + Database + "?autoReconnect=true&failOverReadOnly=false&maxReconnects=1000&serverTimezone=Asia/Shanghai&initialTimeout=1&useSSL=false", UserName, Password);
            String sql =
                    "CREATE TABLE if not exists UserData" +
                            " (" +
                            " DatabaseProtocolVersion INT,"+//Database Table协议版本
                            " UserMuted INT," +//是否已被禁言
                            " UserMuteTime BIGINT," +//用户禁言时长
                            " Permission INT," +//权限等级，目前只有三个等级，-1级：被封禁用户，0级：普通用户，1级：管理员
                            " UserName varchar(255)," +//用户名
                            " Passwd varchar(255)," +//密码
                            " salt varchar(255)," +//密码加盐加的盐
                            " UserLogged INT"+//用户是否已登录
                            " );";
            PreparedStatement ps = DatabaseConnection.prepareStatement(sql);
            ps.executeUpdate();
            sql = "select * from UserData where DatabaseProtocolVersion = 1";
            ps = DatabaseConnection.prepareStatement(sql);
            ResultSet rs = ps.executeQuery();
            while (rs.next())
            {
                sql = "INSERT  INTO `UserData` (" +
                        "`DatabaseProtocolVersion`" +
                        ",`UserMuted`," +
                        " `UserMuteTime`," +
                        "`Permission`," +
                        " `UserName`," +
                        "`Passwd`," +
                        "`salt`," +
                        "`UserLogged`" +
                        ") VALUES (?,?, ?, ?,?,?, ?,?);";
                ps = DatabaseConnection.prepareStatement(sql);
                ps.setInt(1,config.GetDatabaseProtocolVersion());
                ps.setLong(2,rs.getLong("UserMuted"));
                ps.setLong(3,rs.getLong("UserMuteTime"));
                ps.setLong(4,rs.getLong("Permission"));
                ps.setString(5,rs.getString("UserName"));
                ps.setString(6,rs.getString("Passwd"));
                ps.setString(7,rs.getString("salt"));
                ps.setInt(8,0);
                ps.executeUpdate();
            }
            return DatabaseConnection;
        }
        else {
            Class.forName("org.sqlite.JDBC");
            Connection DatabaseConnection = DriverManager.getConnection("jdbc:sqlite:data.db");
            String sql =
                    "CREATE TABLE if not exists UserData" +
                            " (" +
                            " DatabaseProtocolVersion INT,"+//Database Table协议版本
                            " UserMuted INT," +//是否已被禁言
                            " UserMuteTime BIGINT," +//用户禁言时长
                            " Permission INT," +//权限等级，目前只有三个等级，-1级：被封禁用户，0级：普通用户，1级：管理员
                            " UserName varchar(255)," +//用户名
                            " Passwd varchar(255)," +//密码
                            " salt varchar(255)," +//密码加盐加的盐
                            " UserLogged INT"+//用户是否已登录
                            " );";
            PreparedStatement ps = DatabaseConnection.prepareStatement(sql);
            ps.executeUpdate();
            sql = "select * from UserData where DatabaseProtocolVersion = 1";
            ps = DatabaseConnection.prepareStatement(sql);
            ResultSet rs = ps.executeQuery();
            while (rs.next())
            {
                sql = "INSERT  INTO `UserData` (" +
                        "`DatabaseProtocolVersion`" +
                        ",`UserMuted`," +
                        " `UserMuteTime`," +
                        "`Permission`," +
                        " `UserName`," +
                        "`Passwd`," +
                        "`salt`," +
                        "`UserLogged`" +
                        ") VALUES (?,?, ?, ?,?,?, ?,?);";
                ps = DatabaseConnection.prepareStatement(sql);
                ps.setInt(1,config.GetDatabaseProtocolVersion());
                ps.setLong(2,rs.getLong("UserMuted"));
                ps.setLong(3,rs.getLong("UserMuteTime"));
                ps.setLong(4,rs.getLong("Permission"));
                ps.setString(5,rs.getString("UserName"));
                ps.setString(6,rs.getString("Passwd"));
                ps.setString(7,rs.getString("salt"));
                ps.setInt(8,0);
                ps.executeUpdate();
            }
            return DatabaseConnection;
        }
    }
}
