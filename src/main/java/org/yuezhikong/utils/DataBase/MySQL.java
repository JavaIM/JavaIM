package org.yuezhikong.utils.DataBase;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class MySQL {
    private static Connection MySQLConnection = null;
    /**
     * 初始化MySQL连接/获取MySQL连接
     * @param host MySQL地址
     * @param port MySQL端口
     * @param Database MySQL数据库名称
     * @param UserName MySQL用户名
     * @param Password MySQL密码
     * @throws ClassNotFoundException 找不到MySQL驱动
     * @throws SQLException 获取MySQL连接时连接失败
     * @return MySQL连接
     * @apiNote 返回的MySQL连接也会在Class中存一份，如果已经有一个MySQL连接了，会直接返回，而不去创建新的Connection
     */
    public static Connection GetMySQLConnection(String host,String port,String Database,String UserName,String Password) throws ClassNotFoundException, SQLException {
        if (MySQLConnection == null) {
            Class.forName("com.mysql.jdbc.Driver");
            MySQLConnection = DriverManager.getConnection("jdbc:mysql://"+host+":"+port+"/"+Database+"?user="+UserName+"&password="+Password+"autoReconnect=true&failOverReadOnly=false&maxReconnects=1000&useUnicode=true&serverTimezone=Asia/Shanghai&characterEncoding=utf8&initialTimeout=1&useSSL=false");
            String sql = "CREATE TABLE UserData" +
                    " (" +
                    " UserName varchar(255)," +
                    " Passwd varchar(255)," +
                    " sait varchar(255)" +
                    " );";
            PreparedStatement ps = MySQLConnection.prepareStatement(sql);
            ps.executeUpdate();
        }
        return MySQLConnection;
    }
}
