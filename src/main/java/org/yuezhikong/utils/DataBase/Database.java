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
package org.yuezhikong.utils.DataBase;

import org.jetbrains.annotations.NotNull;
import org.yuezhikong.CodeDynamicConfig;
import org.yuezhikong.utils.SaveStackTrace;

import java.sql.*;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class Database {
    private static final Lock lock = new ReentrantLock();
    public static class DatabaseException extends Exception
    {
        public DatabaseException(String Reason,Throwable cause)
        {
            super(Reason,cause);
        }
    }
    private static Connection connection;
    private static void UpdateDatabase(@NotNull Connection DatabaseConnection) throws SQLException {
        boolean exist = false;
        if (CodeDynamicConfig.GetSQLITEMode()) {
            try {
                //查询表信息
                ResultSet rs = DatabaseConnection.createStatement().executeQuery("PRAGMA table_info('UserData')");
                while (rs.next()) {
                    //查看此列名
                    String column = rs.getString("name");
                    if ("token".equals(column)) {
                        exist = true;
                        break;
                    }
                }
            } catch (SQLException e) {
                SaveStackTrace.saveStackTrace(e);
            }
        }
        else
        {
            //查询UserData中是否存在“token”列
            ResultSet rs = DatabaseConnection.createStatement().executeQuery("SHOW COLUMNS FROM UserData LIKE 'token'");
            exist = rs.next();
        }
        if (!exist) {
            //不存在时，添加”token“列
            DatabaseConnection.createStatement().executeUpdate("ALTER TABLE UserData ADD COLUMN token VARCHAR(255) NOT NULL DEFAULT '';");
        }
    }
    /**
     * 初始化数据库连接
     * @param host 如果为MySQL连接，那么为MySQL地址
     * @param port 如果为MySQL连接，那么为MySQL端口
     * @param Database 如果为MySQL连接，那么为MySQL数据库名称
     * @param UserName 如果为MySQL连接，那么为MySQL用户名
     * @param Password 如果为MySQL连接，那么为MySQL密码
     * @throws DatabaseException 数据库连接出错
     * @return 数据库连接
     */
    public static @NotNull Connection Init(String host, String port, String Database, String UserName, String Password) throws DatabaseException {
        lock.lock();
        try {
            if (!CodeDynamicConfig.GetSQLITEMode()) {
                Class.forName("com.mysql.cj.jdbc.Driver");
                connection = DriverManager.getConnection("jdbc:mysql://" + host + ":" + port + "/" + Database + "?autoReconnect=true&failOverReadOnly=false&maxReconnects=1000&serverTimezone=Asia/Shanghai&initialTimeout=1&useSSL=false", UserName, Password);
                connection.createStatement().executeUpdate(
                        "CREATE TABLE if not exists UserData" +
                        " (" +
                        " DatabaseProtocolVersion INT," +//Database Table协议版本
                        " UserMuted INT," +//是否已被禁言
                        " UserMuteTime BIGINT," +//用户禁言时长
                        " Permission INT," +//权限等级，目前只有三个等级，-1级：被封禁用户，0级：普通用户，1级：管理员
                        " UserName varchar(255)," +//用户名
                        " Passwd varchar(255)," +//密码
                        " salt varchar(255)," +//密码加盐加的盐
                        " UserLogged INT," +//用户是否已登录
                        " token varchar(255)" +//Login Token
                        " );");
            } else {
                Class.forName("org.sqlite.JDBC");
                connection = DriverManager.getConnection("jdbc:sqlite:data.db");
                connection.createStatement().executeUpdate(
                        "CREATE TABLE if not exists UserData" +
                        " (" +
                        " DatabaseProtocolVersion INT," +//Database Table协议版本
                        " UserMuted INT," +//是否已被禁言
                        " UserMuteTime BIGINT," +//用户禁言时长
                        " Permission INT," +//权限等级，目前只有三个等级，-1级：被封禁用户，0级：普通用户，1级：管理员
                        " UserName varchar(255)," +//用户名
                        " Passwd varchar(255)," +//密码
                        " salt varchar(255)," +//密码加盐加的盐
                        " UserLogged INT," +//用户是否已登录
                        " token varchar(255)" +//Login Token
                        " );");
            }
            UpdateDatabase(connection);
            return connection;
        }
        catch (Exception e)
        {
            throw new DatabaseException("Database Connection Init Failed",e);
        }
    }
    public static void close()
    {
        try {
            connection.close();
        } catch (SQLException e) {
            SaveStackTrace.saveStackTrace(e);
        } finally {
            lock.unlock();
        }
    }
}
