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
package org.yuezhikong.Server;

import org.apache.ibatis.session.SqlSession;
import org.yuezhikong.Server.UserData.user;
import org.yuezhikong.Server.api.api;
import org.yuezhikong.Server.network.NetworkServer;
import org.yuezhikong.Server.plugin.PluginManager;
import org.yuezhikong.utils.logging.CustomLogger;

import java.util.List;
import java.util.concurrent.ExecutorService;


public interface IServer {

    /**
     * 获取IO线程池
     * @return IO线程池
     */
    @SuppressWarnings("unused")
    ExecutorService getIOThreadPool();

    /**
     * 获取用户列表
     * @return 包含所有用户的列表
     */
    List<user> getUsers();

    /**
     * 将一个用户注册到Users
     * @param User 用户
     * @return 是否成功注册(失败一般是因为同用户名的用户已经登录
     */
    boolean RegisterUser(user User);

    /**
     * 取消注册一个用户
     * @param User 用户
     */
    void UnRegisterUser(user User);

    /**
     * 获取聊天处理器
     * @return 聊天处理器
     */
    ChatRequest getRequest();

    /**
     * 获取控制台用户
     * @return 控制台用户，此用户的操作将被视为为控制台操作
     */
    user getConsoleUser();

    /**
     * 获取插件管理器
     * @return 插件管理器
     */
    PluginManager getPluginManager();

    /**
     * 获取服务端API
     * @return 服务端API
     */
    api getServerAPI();

    /**
     * 获取Logger
     * @return Logger
     * @deprecated 请使用Slf4j获取Logger
     * @see org.slf4j.LoggerFactory#getLogger(Class)
     */
    @Deprecated(since="1.4", forRemoval = true)
    CustomLogger getLogger();

    /**
     * 获取Mybatis SQL会话
     * @return SQL会话
     */
    SqlSession getSqlSession();

    /**
     * 获取网络层服务器
     */
    NetworkServer getNetwork();

    /**
     * 关闭服务器
     */
    void stop();

    /**
     * 用户接收到消息处理
     * 警告，请最好不要使用此方法来模拟用户的消息，此方法设计仅用作网络层调用
     */
    void onReceiveMessage(NetworkServer.NetworkClient client, String message);

    /**
     * 服务器是否启动完成
     * @return 是/否
     */
    boolean isServerCompleateStart();
}
