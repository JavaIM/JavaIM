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

import com.google.gson.Gson;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yuezhikong.Server.UserData.Permission;
import org.yuezhikong.Server.UserData.dao.userInformationDao;
import org.yuezhikong.Server.UserData.tcpUser.tcpUser;
import org.yuezhikong.Server.UserData.user;
import org.yuezhikong.Server.UserData.userInformation;
import org.yuezhikong.Server.api.api;
import org.yuezhikong.Server.plugin.event.events.User.UserChatEvent;
import org.yuezhikong.Server.plugin.event.events.User.UserCommandEvent;
import org.yuezhikong.utils.CustomVar;
import org.yuezhikong.utils.Protocol.ChatProtocol;
import org.yuezhikong.utils.SHA256;
import org.yuezhikong.utils.SaveStackTrace;
import org.yuezhikong.utils.logging.CustomLogger;

import javax.security.auth.login.AccountNotFoundException;
import java.util.List;

public class ChatRequest {
    public static class ChatRequestInput
    {
        private final user User;
        private String ChatMessage;

        public ChatRequestInput(@NotNull user User, @NotNull String ChatMessage)
        {
            this.User = User;
            setChatMessage(ChatMessage);
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
    }

    /**
     * 对于用户聊天信息的进一步处理
     * @param ChatMessageInfo 聊天信息<p>包括用户信息与原始通讯协议信息</p>
     * @return {@code true 阻止将信息发送至客户端} <p>{@code false 继续将信息发送到客户端}</p>
     */
    public boolean UserChatRequests(@NotNull ChatRequestInput ChatMessageInfo)
    {
        if (ChatMessageInfo.getChatMessage().isEmpty())
            //如果发送过来的消息是空的，就没必要再继续处理了
            return true;

        //执行命令处理程序
        if (CommandRequest(ChatMessageInfo))
            return true;

        //执行插件处理程序
        UserChatEvent chatEvent = new UserChatEvent(ChatMessageInfo.getUser(), ChatMessageInfo.getChatMessage());
        instance.getPluginManager().callEvent(chatEvent);
        // 根据插件返回，决定是否继续发送
        return chatEvent.isCancelled();
    }
    /**
     * 用户命令处理程序
     * @param chatMessageInfo 聊天信息<p></p>包括用户信息与原始通讯协议信息
     * @return {@code true 这是一条命令} <p></p>{@code false 这不是一条命令}
     */
    public boolean CommandRequest(@NotNull ChatRequestInput chatMessageInfo) {
        if (chatMessageInfo.getChatMessage().charAt(0) == '/')
        {
            CustomVar.Command CommandInformation = instance.getServerAPI().CommandFormat(chatMessageInfo.getChatMessage());

            //插件事件处理
            UserCommandEvent event = new UserCommandEvent(chatMessageInfo.getUser(), CommandInformation);
            instance.getPluginManager().callEvent(event);
            if (event.isCancelled())
                return true;
            CommandRequest0(chatMessageInfo.getUser(),instance.getServerAPI().CommandFormat(chatMessageInfo.getChatMessage()));
            return true;
        }
        return false;
    }

    private final Logger logger = LoggerFactory.getLogger(ChatRequest.class);

    /**
     * 实际的指令处理程序
     * @param User 用户
     * @param command 指令
     */
    public void CommandRequest0(@NotNull user User, @NotNull CustomVar.Command command)
    {
        final api API = instance.getServerAPI();
        try {
            // 对于非/tell指令，将XX用户执行指令的信息发送给所有管理员
            if (!command.Command().startsWith("/tell")) {
                StringBuilder stringBuilder = new StringBuilder(command.Command());
                stringBuilder.append(" ");
                for (String arg : command.argv()) {
                    stringBuilder.append(arg).append(" ");
                }

                stringBuilder.deleteCharAt(stringBuilder.length() - 1);
                String orig_Command = stringBuilder.toString();
                String tipMessage = String.format("%s 执行了指令: %s", User.getUserName(), orig_Command);
                logger.info(tipMessage);
                for (user sendUser : API.GetValidClientList(true)) {
                    if (!Permission.ADMIN.equals(sendUser.getUserPermission()))
                        continue;
                    API.SendMessageToUser(sendUser, tipMessage);
                }
            }
            //插件指令处理
            if (instance.getPluginManager().RequestPluginCommand(command,User))
                return;
            switch (command.Command()) {
                case "/about" -> {
                    API.SendMessageToUser(User, "JavaIM是根据GNU General Public License v3.0开源的自由程序（开源软件）");
                    API.SendMessageToUser(User, "主仓库于：https://github.com/JavaIM/JavaIM");
                    API.SendMessageToUser(User, "主要开发者名单：");
                    API.SendMessageToUser(User, "QiLechan（柒楽）");
                    API.SendMessageToUser(User, "AlexLiuDev233 （阿白）");
                }
                case "/list" -> {
                    List<user> onlineUserList = API.GetValidClientList(true);
                    if (Permission.ADMIN.equals(User.getUserPermission()))
                        onlineUserList.forEach((user) -> {
                            if (user instanceof tcpUser)
                                API.SendMessageToUser(User,
                                        String.format("%s 权限：%s IP地址：%s",
                                                user.getUserName(),
                                                user.getUserPermission().toString(),
                                                ((tcpUser) user).getNetworkClient().getSocketAddress()
                                        )
                                );
                            else
                                API.SendMessageToUser(User, String.format("%s 权限：%s", user.getUserName(), user.getUserPermission().toString()));
                        });
                    else
                        onlineUserList.forEach((user) ->
                                API.SendMessageToUser(User, String.format("%s 权限：%s", user.getUserName(), user.getUserPermission().toString()))
                        );
                }
                case "/runGC" -> {
                    if (!User.isServer()) {
                        API.SendMessageToUser(User, "此命令只能由服务端执行！");
                        break;
                    }

                    System.gc();
                    logger.info("已经完成垃圾回收");
                }
                case "/help" -> {
                    API.SendMessageToUser(User, "JavaIM服务器帮助");
                    API.SendMessageToUser(User, "/about 查询此程序有关的信息");
                    API.SendMessageToUser(User, "/help 显示服务器帮助信息");
                    API.SendMessageToUser(User, "/tell <用户> <消息> 发送私聊");
                    API.SendMessageToUser(User, "/list 显示在线用户列表");
                    if (Permission.ADMIN.equals(User.getUserPermission())) {
                        API.SendMessageToUser(User, "/op <用户名> 给予管理员权限");
                        API.SendMessageToUser(User, "/deop <用户名> 剥夺管理员权限");
                        API.SendMessageToUser(User, "/ban <用户名> 封禁一位用户");
                        API.SendMessageToUser(User, "/unban <用户名> 解除一位用户的封禁");
                        API.SendMessageToUser(User, "/quit 安全的退出程序");
                        API.SendMessageToUser(User, "/change-password <用户名> <密码> 强制修改某用户密码");
                        API.SendMessageToUser(User, "/kick <用户名> 踢出某用户");
                    }
                    if (User.isServer())
                        API.SendMessageToUser(User, "/runGC 手动执行垃圾回收");
                    if (!instance.getPluginManager().getPluginCommandsDescription().isEmpty())
                        API.SendMessageToUser(User,"插件指令帮助");
                    for (String msg : instance.getPluginManager().getPluginCommandsDescription()) {
                        API.SendMessageToUser(User,msg);
                    }
                    if (Permission.ADMIN.equals(User.getUserPermission()))
                    {
                        if (instance.getPluginManager().getPluginNumber() > 0)
                            API.SendMessageToUser(User,"插件详细信息 ("+instance.getPluginManager().getPluginNumber()+"个插件)");
                        API.SendMessageToUser(User,"插件：("+instance.getPluginManager().getPluginNumber()+")");
                        instance.getPluginManager().getPluginDataList().forEach(
                                pluginData ->  API.SendMessageToUser(User,pluginData.getStaticData().PluginName()+" ")
                        );
                    }
                }
                case "/op" -> {
                    if (!(Permission.ADMIN.equals(User.getUserPermission()))) {
                        API.SendMessageToUser(User, "你没有权限这样做");
                        break;
                    }
                    if (command.argv().length == 1) {
                        userInformationDao mapper = ServerTools.getServerInstanceOrThrow().getSqlSession().getMapper(userInformationDao.class);
                        userInformation information = mapper.getUser(command.argv()[0],null,null);
                        if (information == null) {
                            API.SendMessageToUser(User, "您所操作的用户从来没有来到过本服务器");
                            return;
                        }
                        if (information.getPermission() != 1)
                            API.SendMessageToUser(User, "已将" + information.getUserName() + "设为服务器管理员");
                        else {
                            API.SendMessageToUser(User, "无法设置，对方已是管理员");
                            break;
                        }

                        information.setPermission(1);
                        mapper.updateUser(information);

                        user targetUser;
                        try {
                            targetUser = API.GetUserByUserName(command.argv()[0]);
                        } catch (AccountNotFoundException e) {
                            break;
                        }
                        API.SendMessageToUser(targetUser, "您已被设为服务器管理员");
                        targetUser.SetUserPermission(1);
                    } else {
                        API.SendMessageToUser(User, "语法错误，正确的语法为：/op <用户名>");
                    }
                }
                case "/deop" -> {
                    if (!(Permission.ADMIN.equals(User.getUserPermission()))) {
                        API.SendMessageToUser(User, "你没有权限这样做");
                        break;
                    }
                    if (command.argv().length == 1) {
                        userInformationDao mapper = ServerTools.getServerInstanceOrThrow().getSqlSession().getMapper(userInformationDao.class);
                        userInformation information = mapper.getUser(command.argv()[0],null,null);
                        if (information == null) {
                            API.SendMessageToUser(User, "您所操作的用户从来没有来到过本服务器");
                            return;
                        }
                        if (information.getPermission() == 1)
                            API.SendMessageToUser(User, "已夺去" + information.getUserName() + "的管理员权限");
                        else {
                            API.SendMessageToUser(User, "无法夺去权限，对方不是管理员");
                            break;
                        }

                        information.setPermission(0);
                        mapper.updateUser(information);

                        user targetUser;
                        try {
                            targetUser = API.GetUserByUserName(command.argv()[0]);
                        } catch (AccountNotFoundException e) {
                            break;
                        }
                        API.SendMessageToUser(targetUser, "您已被夺去管理员权限");
                        targetUser.SetUserPermission(0);
                    } else {
                        API.SendMessageToUser(User, "语法错误，正确的语法为：/deop <用户名>");
                    }
                }
                case "/ban" -> {
                    if (!(Permission.ADMIN.equals(User.getUserPermission()))) {
                        API.SendMessageToUser(User, "你没有权限这样做");
                        break;
                    }
                    if (command.argv().length == 1) {
                        userInformationDao mapper = ServerTools.getServerInstanceOrThrow().getSqlSession().getMapper(userInformationDao.class);
                        userInformation information = mapper.getUser(command.argv()[0],null,null);
                        if (information == null) {
                            API.SendMessageToUser(User, "您所操作的用户从来没有来到过本服务器");
                            return;
                        }
                        information.setPermission(-1);
                        mapper.updateUser(information);

                        user kickUser;
                        try {
                            kickUser = API.GetUserByUserName(command.argv()[0]);
                        } catch (AccountNotFoundException e) {
                            break;
                        }
                        API.SendMessageToUser(kickUser, "您已被封禁");
                        kickUser.UserDisconnect();
                    } else {
                        API.SendMessageToUser(User, "语法错误，正确的语法为：/ban <用户名>");
                    }
                }
                case "/unban" -> {
                    if (!(Permission.ADMIN.equals(User.getUserPermission()))) {
                        API.SendMessageToUser(User, "你没有权限这样做");
                        break;
                    }
                    if (command.argv().length == 1) {
                        userInformationDao mapper = ServerTools.getServerInstanceOrThrow().getSqlSession().getMapper(userInformationDao.class);
                        userInformation information = mapper.getUser(command.argv()[0],null,null);
                        if (information == null) {
                            API.SendMessageToUser(User, "您所操作的用户从来没有来到过本服务器");
                            return;
                        }
                        information.setPermission(0);
                        mapper.updateUser(information);
                    } else {
                        API.SendMessageToUser(User, "语法错误，正确的语法为：/unban <用户名>");
                    }
                }
                case "/quit" -> {
                    if (!(Permission.ADMIN.equals(User.getUserPermission()))) {
                        API.SendMessageToUser(User, "你没有权限这样做");
                        break;
                    }
                    instance.getServerAPI().SendMessageToAllClient("服务器已关闭");
                    for (user requestUser : API.GetValidClientList(false)) {
                        requestUser.UserDisconnect();
                    }
                    instance.stop();
                }
                case "/change-password" -> {
                    if (!(Permission.ADMIN.equals(User.getUserPermission()))) {
                        API.SendMessageToUser(User, "你没有权限这样做");
                        break;
                    }
                    if (command.argv().length == 2) {
                        StringBuilder argv = new StringBuilder();//使用StringBuilder而非String，效率更高，string拼接效率较慢
                        for (String arg : command.argv()) {
                            argv.append(arg).append(" ");//将每个arg均append到argv中
                        }
                        API.SendMessageToUser(User, "请输入/change-password force " + argv+ "来确认此操作");//向用户发送提示
                    } else if (command.argv().length == 3) {
                        if ("force".equals(command.argv()[0])) {
                            userInformationDao mapper = ServerTools.getServerInstanceOrThrow().getSqlSession().getMapper(userInformationDao.class);
                            userInformation information = mapper.getUser(command.argv()[1],null,null);
                            if (information == null) {
                                API.SendMessageToUser(User, "您所操作的用户从来没有来到过本服务器");
                                return;
                            }
                            information.setPasswd(SHA256.sha256(command.argv()[2]));
                            mapper.updateUser(information);
                            API.SendMessageToUser(User, "操作成功完成。");
                        } else {
                            API.SendMessageToUser(User, "语法错误，正确的语法为：/change-password <用户名> <密码>");
                        }
                    } else {
                        API.SendMessageToUser(User, "语法错误，正确的语法为：/change-password <用户名> <密码>");
                    }
                }
                case "/kick" -> {
                    if (!(Permission.ADMIN.equals(User.getUserPermission()))) {
                        API.SendMessageToUser(User, "你没有权限这样做");
                        break;
                    }
                    if (command.argv().length == 1) {
                        user kickUser;
                        try {
                            kickUser = API.GetUserByUserName(command.argv()[0]);
                        } catch (AccountNotFoundException e) {
                            API.SendMessageToUser(User, "此用户不存在");
                            break;
                        }
                        API.SendMessageToUser(kickUser, "您已被踢出此服务器");
                        String UserName = kickUser.getUserName();
                        kickUser.UserDisconnect();
                        API.SendMessageToUser(User, "已成功踢出用户：" + UserName);
                    } else {
                        API.SendMessageToUser(User, "语法错误，正确的语法为：/kick <用户名>");
                    }
                }
                case "/tell" -> {
                    if (command.argv().length >= 2) {
                        StringBuilder stringBuilder = new StringBuilder();
                        for (String arg : command.argv())
                        {
                            stringBuilder.append(arg).append(" ");
                        }

                        if (!stringBuilder.isEmpty())
                        {
                            stringBuilder.deleteCharAt(stringBuilder.length() - 1);
                        }

                        stringBuilder.delete(0,command.argv()[0].length() + 1);
                        stringBuilder.insert(0,"[私聊] ");

                        String ChatMessage = stringBuilder.toString();

                        if (command.argv()[0].equals("Server"))//当私聊目标为后台时
                        {
                            ((CustomLogger) logger).ChatMsg("["+User.getUserName()+"]:"+ChatMessage);
                            API.SendMessageToUser(User, "你对" + command.argv()[0] + "发送了私聊：" + ChatMessage);
                            break;
                        }
                        try {
                            ChatProtocol chatProtocol = new ChatProtocol();
                            chatProtocol.setSourceUserName(User.getUserName());
                            chatProtocol.setMessage(ChatMessage);
                            API.SendJsonToClient(API.GetUserByUserName(command.argv()[0]),new Gson().toJson(chatProtocol),"ChatProtocol");
                            API.SendMessageToUser(User, "你对" + command.argv()[0] + "发送了私聊：" + ChatMessage);
                        } catch (AccountNotFoundException e) {
                            API.SendMessageToUser(User, "此用户不存在");
                        }
                    } else {
                        API.SendMessageToUser(User, "语法错误，正确的语法为：/tell <用户> <消息>");
                    }
                }
                default -> API.SendMessageToUser(User, "未知的命令！请输入/help查看帮助！");
            }
        } catch (Throwable throwable)
        {
            SaveStackTrace.saveStackTrace(throwable);
            API.SendMessageToUser(User,"在执行此命令的过程中出现了意外的错误");
        }
    }

    private final IServer instance;
    public ChatRequest(IServer serverInstance)
    {
        instance = serverInstance;
    }
}
