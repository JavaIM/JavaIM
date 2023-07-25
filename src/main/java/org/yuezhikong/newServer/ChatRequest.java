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
package org.yuezhikong.newServer;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.yuezhikong.CodeDynamicConfig;
import org.yuezhikong.newServer.UserData.Permission;
import org.yuezhikong.newServer.UserData.user;
import org.yuezhikong.newServer.api.api;
import org.yuezhikong.newServer.plugin.event.events.UserChatEvent;
import org.yuezhikong.newServer.plugin.event.events.UserCommandEvent;
import org.yuezhikong.utils.CustomVar;
import org.yuezhikong.utils.DataBase.Database;
import org.yuezhikong.utils.Protocol.NormalProtocol;
import org.yuezhikong.utils.SaveStackTrace;

import javax.security.auth.login.AccountNotFoundException;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class ChatRequest {
    public static class ChatRequestInput
    {
        private final user User;
        private final NormalProtocol protocol;
        private String ChatMessage;

        public ChatRequestInput(@NotNull user User, @NotNull NormalProtocol protocol)
        {
            this.User = User;
            this.protocol = protocol;
            setChatMessage(protocol.getMessageBody().getMessage());
        }
        public ChatRequestInput(@NotNull user User,@NotNull String ChatMessage)
        {
            this.User = User;
            setChatMessage(ChatMessage);
            protocol = null;
        }
        public void setChatMessage(String chatMessage) {
            ChatMessage = chatMessage;
        }

        public String getChatMessage() {
            return ChatMessage;
        }

        public user getUser() {
            return User;
        }

        public @Nullable NormalProtocol getProtocol() {
            return protocol;
        }
    }
    private final SimpleDateFormat formatter;
    /**
     * 对于用户聊天信息的进一步处理
     * @param ChatMessageInfo 聊天信息<p></p>包括用户信息与原始通讯协议信息
     * @return {@code true 阻止将信息发送至客户端} <p></p>{@code false 继续将信息发送到客户端}
     */
    public boolean UserChatRequests(@NotNull ChatRequestInput ChatMessageInfo)
    {
        //被格式化过的当前时间
        String CurrentTimeFormatted = formatter.format(new Date(System.currentTimeMillis()));
        //每条消息的头
        String ChatPrefix = String.format("[%s] [%s]:",CurrentTimeFormatted,ChatMessageInfo.getUser().getUserName());

        if (ChatMessageInfo.getChatMessage().isEmpty())
        {
            //如果发送过来的消息是空的，就没必要再继续处理了
            return true;
        }

        //执行命令处理程序
        if (CommandRequest(ChatMessageInfo))
        {
            return true;
        }

        //执行插件处理程序
        UserChatEvent chatEvent = new UserChatEvent(ChatMessageInfo.getUser(), ChatMessageInfo.getChatMessage());
        ServerMain.getServer().getPluginManager().callEvent(chatEvent);
        if (chatEvent.isCancel())
        {
            //插件表明取消此事件，就不要额外处理了
            return true;
        }

        //为消息补上消息头
        ChatMessageInfo.setChatMessage(ChatPrefix+ChatMessageInfo.ChatMessage);
        //如果出现换行，把他处理为一个“新的”消息，加上一个新的消息头，这样看来就是两条消息了，防止通过换行来伪装发信
        ChatMessageInfo.setChatMessage(ChatMessageInfo.getChatMessage().replaceAll("\n",
                "\n"+ChatPrefix
                ));
        return false;
    }
    /**
     * 用户命令处理程序
     * @param chatMessageInfo 聊天信息<p></p>包括用户信息与原始通讯协议信息
     * @return {@code true 这是一条命令} <p></p>{@code false 这不是一条命令}
     */
    public boolean CommandRequest(@NotNull ChatRequestInput chatMessageInfo) {
        if (chatMessageInfo.getChatMessage().charAt(0) == '/')
        {
            final api API = ServerMain.getServer().getServerAPI();
            CustomVar.Command CommandInformation = API.CommandFormat(chatMessageInfo.getChatMessage());
            UserCommandEvent event = new UserCommandEvent(chatMessageInfo.getUser(),CommandInformation);
            ServerMain.getServer().getPluginManager().callEvent(event);
            if (event.isCancel())
            {
                return true;
            }
            switch (CommandInformation.Command())
            {
                case "/about" -> {
                    API.SendMessageToUser(chatMessageInfo.getUser(),"JavaIM是根据GNU General Public License v3.0开源的自由程序（开源软件）");
                    API.SendMessageToUser(chatMessageInfo.getUser(),"主仓库位于：https://github.com/JavaIM/JavaIM");
                    API.SendMessageToUser(chatMessageInfo.getUser(),"主要开发者名单：");
                    API.SendMessageToUser(chatMessageInfo.getUser(),"QiLechan（柒楽）");
                    API.SendMessageToUser(chatMessageInfo.getUser(),"AlexLiuDev233 （阿白）");
                }
                case "/help" -> {
                    API.SendMessageToUser(chatMessageInfo.getUser(),"JavaIM Server的服务器帮助");
                    API.SendMessageToUser(chatMessageInfo.getUser(),"/about 查询此程序有关的信息");
                    API.SendMessageToUser(chatMessageInfo.getUser(),"/help 显示服务器帮助信息");
                    if (Permission.ADMIN.equals(chatMessageInfo.getUser().getUserPermission()))
                    {
                        API.SendMessageToUser(chatMessageInfo.getUser(),"/op <用户名> 给予管理员权限");
                        API.SendMessageToUser(chatMessageInfo.getUser(),"/deop <用户名> 剥夺管理员权限");
                        API.SendMessageToUser(chatMessageInfo.getUser(),"/ban <用户名> 封禁一位用户");
                        API.SendMessageToUser(chatMessageInfo.getUser(),"/unban <用户名> 解除一位用户的封禁");
                        API.SendMessageToUser(chatMessageInfo.getUser(),"/quit 安全的退出程序");
                    }
                }
                case "/op" -> {
                    if (CommandInformation.argv().length == 1) {
                        try {
                            user User = API.GetUserByUserName(CommandInformation.argv()[0], ServerMain.getServer(), true);
                            if (Permission.ADMIN.equals(User.getUserPermission()))
                            {
                                API.SendMessageToUser(chatMessageInfo.getUser(),"无法给予权限，对方已是管理员");
                            }
                            else {
                                User.SetUserPermission(1, false);
                                API.SendMessageToUser(chatMessageInfo.getUser(), "已将" + User.getUserName() + "设为管理员");
                                API.SendMessageToUser(User,"["+chatMessageInfo.getUser()+":已将" + User.getUserName() + "设为管理员]");
                            }
                        } catch (AccountNotFoundException e) {
                            API.SendMessageToUser(chatMessageInfo.getUser(),"您所输入的用户不存在");
                        }
                    }
                    else
                    {
                        API.SendMessageToUser(chatMessageInfo.getUser(),"语法错误，正确的语法为：/op <用户名>");
                    }
                }
                case "/deop" -> {
                    if (CommandInformation.argv().length == 1) {
                        try {
                            user User = API.GetUserByUserName(CommandInformation.argv()[0], ServerMain.getServer(), true);
                            if (Permission.ADMIN.equals(User.getUserPermission()))
                            {
                                User.SetUserPermission(0, false);
                                API.SendMessageToUser(chatMessageInfo.getUser(), "已剥夺" + User.getUserName() + "的管理员权限");
                                API.SendMessageToUser(User,"["+chatMessageInfo.getUser()+":已剥夺"+ User.getUserName()+"的管理员权限]");
                            }
                            else {
                                API.SendMessageToUser(chatMessageInfo.getUser(),"无法剥夺权限，对方不是管理员");
                            }
                        } catch (AccountNotFoundException e) {
                            API.SendMessageToUser(chatMessageInfo.getUser(),"您所输入的用户不存在");
                        }
                    }
                    else
                    {
                        API.SendMessageToUser(chatMessageInfo.getUser(),"语法错误，正确的语法为：/deop <用户名>");
                    }
                }
                case "/ban" -> {
                    if (CommandInformation.argv().length == 1) {
                        try {
                            user User = API.GetUserByUserName(CommandInformation.argv()[0], ServerMain.getServer(), true);
                            if (Permission.BAN.equals(User.getUserPermission()))
                            {
                                API.SendMessageToUser(chatMessageInfo.getUser(),"无法封禁，对方已被封禁");
                            }
                            else {
                                User.SetUserPermission(-1, false);
                                User.UserDisconnect();
                                API.SendMessageToUser(chatMessageInfo.getUser(), "已将" + User.getUserName() + "封禁");
                            }
                        } catch (AccountNotFoundException e) {
                            API.SendMessageToUser(chatMessageInfo.getUser(),"您所输入的用户不存在");
                        }
                    }
                    else
                    {
                        API.SendMessageToUser(chatMessageInfo.getUser(),"语法错误，正确的语法为：/ban <用户名>");
                    }
                }
                case "/unban" -> {
                    if (CommandInformation.argv().length == 1) {
                        try {
                            new Thread() {
                                @Override
                                public void run() {
                                    this.setName("SQL Worker");
                                    try {
                                        Connection DatabaseConnection = Database.Init(CodeDynamicConfig.GetMySQLDataBaseHost(), CodeDynamicConfig.GetMySQLDataBasePort(), CodeDynamicConfig.GetMySQLDataBaseName(), CodeDynamicConfig.GetMySQLDataBaseUser(), CodeDynamicConfig.GetMySQLDataBasePasswd());
                                        String sql = "select * from UserData where UserName = ?";
                                        PreparedStatement ps = DatabaseConnection.prepareStatement(sql);
                                        ps.setString(1, CommandInformation.argv()[0]);
                                        ResultSet rs = ps.executeQuery();
                                        if (rs.next())
                                        {
                                            sql = "UPDATE UserData SET Permission = 0 where UserName = ?";
                                            ps = DatabaseConnection.prepareStatement(sql);
                                            ps.setString(1, CommandInformation.argv()[0]);
                                            ps.executeUpdate();
                                            API.SendMessageToUser(chatMessageInfo.getUser(), "已解封"+CommandInformation.argv()[0]);
                                        }
                                    } catch (Database.DatabaseException | SQLException e) {
                                        SaveStackTrace.saveStackTrace(e);
                                    }
                                    finally {
                                        Database.close();
                                    }
                                }

                                public Thread start2() {
                                    start();
                                    return this;
                                }
                            }.start2().join();
                        } catch (InterruptedException e) {
                            SaveStackTrace.saveStackTrace(e);
                        }
                    }
                    else
                    {
                        API.SendMessageToUser(chatMessageInfo.getUser(),"语法错误，正确的语法为：/unban <用户名>");
                    }
                }
                case "/quit" -> {
                    ServerMain.getServer().getServerAPI().SendMessageToAllClient("服务器已关闭",ServerMain.getServer());
                    for (user User : ServerMain.getServer().getUsers())
                    {
                        User.UserDisconnect();
                    }
                    ServerMain.getServer().authThread.interrupt();
                    try {
                        ServerMain.getServer().getPluginManager().UnLoadAllPlugin();
                    } catch (IOException e) {
                        SaveStackTrace.saveStackTrace(e);
                    }
                    System.exit(0);
                }
                case "/crash" -> {
                    if (CodeDynamicConfig.GetDebugMode())
                    {
                        ServerMain.getServer().runOnMainThread(() -> {
                            throw new RuntimeException("Debug Crash");
                        });
                    }
                    else
                    {
                        API.SendMessageToUser(chatMessageInfo.getUser(),"未知的命令！请输入/help查看帮助！");
                    }
                }
                default -> API.SendMessageToUser(chatMessageInfo.getUser(),"未知的命令！请输入/help查看帮助！");
            }
            return true;
        }
        return false;
    }

    public ChatRequest()
    {
        formatter = new SimpleDateFormat("yyyy-MM-dd 'at' HH:mm:ss z");
    }
}