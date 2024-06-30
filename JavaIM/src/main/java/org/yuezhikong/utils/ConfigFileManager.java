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
import org.apache.commons.io.FileUtils;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.Nullable;

import java.io.*;
import java.util.Properties;

@Slf4j
public class ConfigFileManager {
    private static final Properties prop = new Properties();
    private ConfigFileManager() {}

    /**
     * 创建服务端配置文件
     */
    public static void createServerConfig(){
        try (OutputStream fos = new FileOutputStream("server.properties")) {
            prop.setProperty("serverName", "A JavaIM Server");
            prop.setProperty("sqlite","true");
            prop.setProperty("mysqlHost","127.0.0.1");
            prop.setProperty("mysqlPort","3306");
            prop.setProperty("mysqlDBName","JavaIM");
            prop.setProperty("mysqlUser","JavaIM");
            prop.setProperty("mysqlPasswd","JavaIM");
            prop.store(fos,"JavaIM Configuration");
        } catch (IOException ex) {
            log.error("出现错误!",ex);
        }
    }

    /**
     * 获取服务端配置信息
     * @param key 键
     * @param defaultValue 默认值
     * @return 配置信息
     */
    @Contract(pure = true)
    public static String getServerConfig(String key, String defaultValue){
        return prop.getProperty(key, defaultValue);
    }

    /**
     * 获取服务端配置信息
     * @param key 键
     * @return 配置信息
     */
    @Contract(pure = true)
    public static @Nullable String getServerConfig(String key) {
        return getServerConfig(key, null);
    }

    /**
     * 重载服务端配置文件
     * @apiNote 不会修改动态配置! CodeDynamicConfig 不会受到影响!
     */
    public static void reloadServerConfig(){
        try (InputStream fis = new FileInputStream("server.properties")){
            prop.load(fis);
        } catch (IOException ex) {
            log.error("刷新服务端配置文件时出现错误!",ex);
        }
    }

    /**
     * 设置服务端配置信息
     * @param key 键
     * @param value 值
     * @apiNote 不会修改动态配置! CodeDynamicConfig 不会受到影响!
     */
    public static void setServerConfig(String key, String value) {
        prop.setProperty(key, value);
    }

    /**
     * 覆写服务端配置信息(保存更改)
     */
    public static void rewriteServerConfig() {
        try (OutputStream fos = new FileOutputStream(FileUtils.delete(new File("server.properties")))){
            prop.store(fos,"JavaIM Configuration");
        } catch (IOException ex) {
            log.error("覆写服务端配置文件时出现错误!",ex);
        }
    }
}