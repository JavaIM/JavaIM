/*
 * Simplified Chinese (简体中文)
 *
 * 版权所有 (C) 2023 QiLechan <qilechan@outlook.com> 和本程序的贡献者
 *
 * 本程序是自由软件：你可以再分发之和/或依照由自由软件基金会发布的 GNU 通用公共许可证修改之，无论是版本 3 许可证，还是 3 任何以后版都可以。
 * 发布该程序是希望它能有用，但是并无保障;甚至连可销售和符合某个特定的目的都不保证。请参看 GNU 通用公共许可证，了解详情。
 * 你应该随程序获得一份 GNU 通用公共许可证的副本。如果没有，请看 <https://www.gnu.org/licenses/>。
 * English (英语)
 *
 * Copyright (C) 2023 QiLechan <qilechan@outlook.com> and contributors to this program
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or 3 any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program. If not, see <https://www.gnu.org/licenses/>.
 */
package org.yuezhikong.utils;

import org.apache.ibatis.io.Resources;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.yuezhikong.CodeDynamicConfig;

import java.io.IOException;
import java.sql.*;
import java.util.Objects;
import java.util.Properties;

public class DatabaseHelper {

    /**
     * 初始化数据库
     * @return JDBCUrl
     */
    public static String InitDataBase() {
        Connection connection = null;
        String JDBCUrl;
        try {
            if (!CodeDynamicConfig.GetSQLITEMode()) {
                JDBCUrl = "jdbc:mysql://" + CodeDynamicConfig.GetMySQLDataBaseHost()
                        + ":" + CodeDynamicConfig.GetMySQLDataBasePort() + "/" + CodeDynamicConfig.GetMySQLDataBaseName()
                        + "?autoReconnect=true&failOverReadOnly=false&maxReconnects=1000&serverTimezone=Asia/Shanghai&initialTimeout=1&useSSL=false";
                Class.forName("com.mysql.cj.jdbc.Driver");
                connection = DriverManager.getConnection(JDBCUrl, CodeDynamicConfig.GetMySQLDataBaseUser(), CodeDynamicConfig.GetMySQLDataBasePasswd());
            } else {
                Class.forName("org.sqlite.JDBC");
                JDBCUrl = "jdbc:sqlite:data.db";
                connection = DriverManager.getConnection(JDBCUrl);
            }
            // 创建数据库表
            connection.createStatement().executeUpdate(
                    "CREATE TABLE if not exists UserData" +
                            " (" +
                            " DatabaseProtocolVersion INT," +//Database Table协议版本
//                            " UserMuted INT," +//是否已被禁言
//                            " UserMuteTime BIGINT," +//用户禁言时长
                            " Permission INT," +//权限等级，目前只有三个等级，-1级：被封禁用户，0级：普通用户，1级：管理员
                            " UserName varchar(255)," +//用户名
                            " Passwd varchar(255)," +//密码
                            " salt varchar(255)," +//密码加盐加的盐
                            //" UserLogged INT," +//用户是否已登录
                            " token varchar(255)" +//Login Token
                            " );");
            //更新数据库表
            UpdateDatabase(connection);
        }
        catch (Exception e) {
            throw new RuntimeException("Database Connection Init Failed",e);
        }
        finally {
            try {
                Objects.requireNonNull(connection).close();
            } catch (SQLException | NullPointerException e) {
                SaveStackTrace.saveStackTrace(e);
            }
        }
        return JDBCUrl;
    }

    /**
     * 初始化Mybatis
     * @param JDBCUrl JDBCUrl
     * @return SQL会话
     */
    public static SqlSession InitMybatis(String JDBCUrl)
    {
        checks.checkArgument(JDBCUrl == null, "JDBC Url can not be null!");
        Properties MybatisConfig = new Properties();
        MybatisConfig.setProperty("jdbc.url",JDBCUrl);
        if (CodeDynamicConfig.GetSQLITEMode())
        {
            MybatisConfig.setProperty("jdbc.driver", "org.sqlite.JDBC");
            MybatisConfig.setProperty("jdbc.username", "");
            MybatisConfig.setProperty("jdbc.password", "");
        }
        else {
            MybatisConfig.setProperty("jdbc.driver", "com.mysql.cj.jdbc.Driver");
            MybatisConfig.setProperty("jdbc.username", CodeDynamicConfig.GetMySQLDataBaseUser());
            MybatisConfig.setProperty("jdbc.password", CodeDynamicConfig.GetMySQLDataBasePasswd());
        }
        try {
            return new SqlSessionFactoryBuilder().build(Resources.getResourceAsStream("mybatis-config.xml"), MybatisConfig).openSession(true);
        } catch (IOException e) {
            throw new RuntimeException("Mybatis Open Failed",e);
        }
    }

    /**
     * 检查某个列是否存在
     * @param Columns 列名
     * @param TableName 表名
     * @param DatabaseConnection 连接
     * @return true为存在，false为不存在
     * @throws SQLException SQL出现错误
     * @apiNote 注意，此函数可能导致SQL注入，请勿将用户输入载入到此函数
     */
    @ApiStatus.Internal
    private static boolean CheckColumnsExist(String Columns,String TableName,@NotNull Connection DatabaseConnection) throws SQLException {
        if (CodeDynamicConfig.GetSQLITEMode())
        {
            ResultSet rs = DatabaseConnection.createStatement().executeQuery("PRAGMA table_info ("+TableName+")");
            while (rs.next()) {
                //查看此列名
                String column = rs.getString("name");
                if (Columns.equals(column)) {
                    return true;
                }
            }
        }
        else
        {
            //查询UserData中是否存在列
            PreparedStatement ps = DatabaseConnection.prepareStatement("SHOW COLUMNS FROM ? LIKE ?");
            ps.setString(1,TableName);
            ps.setString(2,Columns);
            return ps.executeQuery().next();
        }
        return false;
    }

    /**
     * 更新数据库
     * @param DatabaseConnection 数据库连接
     * @throws SQLException SQL出错
     */
    public static void UpdateDatabase(@NotNull Connection DatabaseConnection) throws SQLException {
        if (!CheckColumnsExist("token","UserData",DatabaseConnection)) {
            //不存在时，添加”token“列
            DatabaseConnection.createStatement().executeUpdate("ALTER TABLE UserData ADD COLUMN token VARCHAR(255) NOT NULL DEFAULT '';");
        }
    }
}
