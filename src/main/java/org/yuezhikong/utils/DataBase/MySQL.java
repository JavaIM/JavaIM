package org.yuezhikong.utils.DataBase;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class MySQL {
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
     */
    public static Connection GetMySQLConnection(String host,String port,String Database,String UserName,String Password) throws ClassNotFoundException, SQLException {
        Class.forName("com.mysql.cj.jdbc.Driver");
        Connection mySQLConnection = DriverManager.getConnection("jdbc:mysql://" + host + ":" + port + "/" + Database + "?autoReconnect=true&failOverReadOnly=false&maxReconnects=1000&serverTimezone=Asia/Shanghai&initialTimeout=1&useSSL=false", UserName, Password);
        String sql = "CREATE TABLE if not exists UserData" +
                " (" +
                " UserName varchar(255)," +
                " Passwd varchar(255)," +
                " salt varchar(255)" +
                " );";
        PreparedStatement ps = mySQLConnection.prepareStatement(sql);
        ps.executeUpdate();
        return mySQLConnection;
    }
}
