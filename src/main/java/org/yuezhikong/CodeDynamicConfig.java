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
package org.yuezhikong;

import static org.yuezhikong.utils.ConfigFileManager.LoadClientProperties;
import static org.yuezhikong.utils.ConfigFileManager.LoadServerProperties;

public final class CodeDynamicConfig {
    //静态区，不允许修改
    //协议版本
    private static final int ProtocolVersion = 4;
    //本程序版本：
    private static final String Version = "InDev";
    //目前最新的Database协议版本
    private static final int TheLatestDatabaseProtocolVersion = 2;
    //调试模式
    private static final boolean Debug_Mode = false;
    //About System
    public static final boolean About_System = true;
    //RSA加密功能
    private static final boolean RSA_Mode = true;
    //插件系统
    private static final boolean PluginSystem = true;
    //本版本为实验性版本
    private static final boolean ThisVersionIsExpVersion = true;
    //如果为测试性版本，则信息为：
    private static final String ExpVersionText = "此版本包含新C/S机制";
    //AES加密系统（依赖于RSA加密系统）
    private static final boolean AES_Mode = true;

    //动态区，可动态通过配置文件修改
    //是否是GUI模式
    public static boolean GUIMode = Boolean.parseBoolean(LoadClientProperties().getProperty("GUIMode"));

    //仅服务端
    //最大客户端数量，输入-1代表禁用
    public static int MAX_CLIENT = Integer.parseInt(LoadServerProperties().getProperty("MAX_CLIENT"));
    //是否启用登录系统
    public static boolean EnableLoginSystem = Boolean.parseBoolean(LoadServerProperties().getProperty("EnableLoginSystem"));
    //是否使用SQLITE
    public static boolean Use_SQLITE_Mode = Boolean.parseBoolean(LoadServerProperties().getProperty("Use_SQLITE_Mode"));
    //MySQL数据库地址
    public static String MySQLDataBaseHost = LoadServerProperties().getProperty("MySQLDataBaseHost");
    //MySQL数据库端口
    public static String MySQLDataBasePort = LoadServerProperties().getProperty("MySQLDataBasePort");
    //MySQL数据库名称
    public static String MySQLDataBaseName = LoadServerProperties().getProperty("MySQLDataBaseName");
    //MySQL数据库用户
    public static String MySQLDataBaseUser = LoadServerProperties().getProperty("MySQLDataBaseUser");
    //MySQL数据库密码
    public static String MySQLDataBasePasswd = LoadServerProperties().getProperty("MySQLDataBasePasswd");
    //socket最大空闲时间，单位为：毫秒
    //如果这么长的时间无任何数据发来，则认为客户端已断开
    public static int SocketTimeout = 120000;

    //仅客户端
    //心跳包时间间隔，单位为：毫秒
    public static int HeartbeatInterval = 30000;

    public static boolean GetPluginSystemMode() { return PluginSystem; }
    public static boolean GetSQLITEMode() { return Use_SQLITE_Mode; }
    public static int GetDatabaseProtocolVersion() { return TheLatestDatabaseProtocolVersion; }
    public static boolean GetEnableLoginSystem() { return EnableLoginSystem; }
    public static String GetMySQLDataBaseHost() { return MySQLDataBaseHost; }
    public static String GetMySQLDataBasePort() { return MySQLDataBasePort; }
    public static String GetMySQLDataBaseName() { return MySQLDataBaseName; }
    public static String GetMySQLDataBaseUser() { return MySQLDataBaseUser; }
    public static String GetMySQLDataBasePasswd() { return MySQLDataBasePasswd; }
    public static boolean GetRSA_Mode (){ return RSA_Mode; }
    public static int getMaxClient() { return MAX_CLIENT; }
    public static boolean GetDebugMode() { return Debug_Mode; }
    public static boolean isThisVersionIsExpVersion() {
        return ThisVersionIsExpVersion;
    }

    public static String getVersion() {
        return Version;
    }

    public static int getProtocolVersion() {
        return ProtocolVersion;
    }

    public static boolean isAES_Mode() {
        return AES_Mode;
    }

    public static String getExpVersionText() {
        return ExpVersionText;
    }

    public static boolean isGUIMode() {
        return GUIMode;
    }
}
