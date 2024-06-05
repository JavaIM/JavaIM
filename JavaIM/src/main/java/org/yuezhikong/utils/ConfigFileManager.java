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

import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;

import java.io.*;
import java.util.Properties;

@Slf4j
public class ConfigFileManager {
    public void CreateServerprop(){
        Properties sprop = new Properties();
        try {
            sprop.setProperty("EnableLoginSystem", "true");
            sprop.setProperty("Use_SQLITE_Mode", "true");
            sprop.setProperty("MySQLDataBaseHost", "127.0.0.1");
            sprop.setProperty("MySQLDataBasePort", "3306");
            sprop.setProperty("MySQLDataBaseName", "JavaIM");
            sprop.setProperty("MySQLDataBaseUser", "JavaIM");
            sprop.setProperty("MySQLDataBasePasswd", "JavaIM");
            sprop.setProperty("Server-Name", "A JavaIM Server");

            sprop.store(new FileOutputStream("server.properties"), "JavaIM Server Configuration");
        } catch (IOException ex) {
            log.error("出现错误!",ex);
        }
    }
    public static @NotNull Properties LoadServerProperties(){
        Properties prop = new Properties();
        try {
            prop.load(new FileInputStream("server.properties"));
        } catch (IOException ex) {
            log.error("出现错误!",ex);
        }
        return prop;
    }
}