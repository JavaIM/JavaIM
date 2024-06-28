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
package org.yuezhikong.Server.plugin;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.yuezhikong.Server.plugin.Plugin.Plugin;
import org.yuezhikong.Server.plugin.Plugin.PluginData;
import org.yuezhikong.Server.plugin.event.Listener;
import org.yuezhikong.Server.plugin.event.events.Event;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.concurrent.ExecutorService;

@SuppressWarnings("unused")
public interface PluginManager {

    /**
     * 预加载一个插件
     * @param PluginFile 这个插件的文件
     * @throws IOException Input/Output出现错误
     * @throws ClassNotFoundException 插件指定的主类不存在
     * @throws NoSuchMethodException 找不到无参数构造器
     * @throws InvocationTargetException 插件构造器抛出了一个异常
     * @throws InstantiationException 插件主类是一个抽象类或接口
     * @throws IllegalAccessException 没有权限访问构造器
     * @throws ClassCastException 插件主类未实现Plugin接口
     * @throws UnsupportedOperationException 此插件不允许在此时间节点加载
     */
    void preloadPlugin(@NotNull File PluginFile) throws IOException, NoSuchMethodException, InvocationTargetException, InstantiationException, IllegalAccessException, ClassNotFoundException, UnsupportedOperationException;

    /**
     * 加载一个插件
     * @param PluginFile 这个插件的文件
     */
    void loadPlugin(@NotNull File PluginFile);

    /**
     * 添加一个事件监听器
     * @param listener 监听器实例
     * @param plugin 插件实例
     */
    void addEventListener(Listener listener, Plugin plugin);

    /**
     * 获取注册的所有事件监听器
     * @param plugin 插件实例
     * @return 监听器实例
     */
    List<Listener> getEventListener(Plugin plugin);

    /**
     * 取消注册一个事件监听器
     * @param listener 事件监听器实例
     * @param plugin 插件实例
     */
    void removeEventListener(Listener listener, Plugin plugin);

    /**
     * 卸载一个插件
     * @param plugin 插件信息
     * @throws IOException 无法关闭URLClassLoader
     * @throws IllegalStateException 在PreLoad阶段调用UnLoadPlugin抛出
     */
    void unloadPlugin(@NotNull Plugin plugin) throws IOException;

    /**
     * 卸载所有插件
     * @throws IOException 无法关闭URLClassLoader
     */
    void unloadAllPlugin() throws IOException;

    /**
     * 预加载一个文件夹中的插件
     * @param Directory 文件夹
     * @param StartUpThreadPool 用于加载插件的线程池
     * @apiNote 注意，通过此方法加载，只会加载后缀为.jar的插件
     */
    void preloadPluginOnDirectory(@NotNull File Directory, ExecutorService StartUpThreadPool);
    /**
     * 加载一个文件夹中的插件
     * @param Directory 文件夹
     * @param StartUpThreadPool 用于加载插件的线程池
     * @apiNote 注意，通过此方法加载，只会加载后缀为.jar的插件
     */
    void loadPluginOnDirectory(@NotNull File Directory, ExecutorService StartUpThreadPool);

    /**
     * 调用事件处理程序
     * @param event 事件
     */
    void callEvent(@NotNull Event event);

    /**
     * 根据插件名获取插件信息
     * @param name 插件名
     * @return 插件信息
     */
    @Nullable Plugin getPluginByName(@NotNull String name);

    /**
     * 获取插件数量
     * @return 已加载的插件数量
     */
    int getPluginNumber();

    /**
     * 获取插件数据集
     * @return 插件数据集
     */
    List<PluginData> getPluginDataList();
}
