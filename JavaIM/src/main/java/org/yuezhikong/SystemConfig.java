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

import lombok.Getter;

import static org.yuezhikong.utils.ConfigFileManager.getServerConfig;


public final class SystemConfig {
    //协议版本
    @Getter
    private static final int ProtocolVersion = 12;
    //本程序版本：
    @Getter
    private static final String Version = "InDev";
    // 多线程下载分片数量
    @Getter
    private final static int DownloadParts = Runtime.getRuntime().availableProcessors();

    //服务器名
    @Getter
    private final static String ServerName = getServerConfig("serverName", "A JavaIM Server");
    //是否使用SQLITE
    @Getter
    private final static boolean Use_SQLITE_Mode = Boolean.parseBoolean(getServerConfig("sqlite"));
    //MySQL数据库地址
    @Getter
    private final static String MySQLDataBaseHost = getServerConfig("mysqlHost");
    //MySQL数据库端口
    @Getter
    private final static String MySQLDataBasePort = getServerConfig("mysqlPort");
    //MySQL数据库名称
    @Getter
    private final static String MySQLDataBaseName = getServerConfig("mysqlDBName");
    //MySQL数据库用户
    @Getter
    private final static String MySQLDataBaseUser = getServerConfig("mysqlUser");
    //MySQL数据库密码
    @Getter
    private final static String MySQLDataBasePasswd = getServerConfig("mysqlPasswd");
    //Maven中心仓库地址(此项配置用来设置镜像地址)
    @Getter
    private final static String MavenCenterRepository = getServerConfig("mavenCenterRepo");

    /**
     * Static Config不得被实例化
     */
    private SystemConfig() {
    }
}
