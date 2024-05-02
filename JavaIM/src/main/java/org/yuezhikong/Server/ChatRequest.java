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

import org.jetbrains.annotations.NotNull;
import org.yuezhikong.Server.UserData.Permission;
import org.yuezhikong.Server.UserData.dao.userInformationDao;
import org.yuezhikong.Server.UserData.user;
import org.yuezhikong.Server.UserData.userInformation;
import org.yuezhikong.Server.api.api;
import org.yuezhikong.Server.plugin.event.events.User.UserChatEvent;
import org.yuezhikong.Server.plugin.event.events.User.UserCommandEvent;
import org.yuezhikong.utils.CustomVar;
import org.yuezhikong.utils.SaveStackTrace;

import javax.security.auth.login.AccountNotFoundException;
import java.text.SimpleDateFormat;
import java.util.Date;
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
    private final SimpleDateFormat formatter;

    /**
     * 聊天信息格式化
     * @param ChatMessageInfo 聊天信息
     */
    public void ChatFormat(@NotNull ChatRequestInput ChatMessageInfo)
    {
        //被格式化过的当前时间
        String CurrentTimeFormatted = formatter.format(new Date(System.currentTimeMillis()));
        //每条消息的头
        String ChatPrefix = String.format("[%s] [%s]:",CurrentTimeFormatted,ChatMessageInfo.getUser().getUserName());
        //为消息补上消息头
        ChatMessageInfo.setChatMessage(ChatPrefix+ChatMessageInfo.ChatMessage);
        //如果出现换行，把他处理为一个“新的”消息，加上一个新的消息头，这样看来就是两条消息了，防止通过换行来伪装发信
        ChatMessageInfo.setChatMessage(ChatMessageInfo.getChatMessage().replaceAll("\n",
                "\n"+ChatPrefix
        ));
    }
    /**
     * 对于用户聊天信息的进一步处理
     * @param ChatMessageInfo 聊天信息<p>包括用户信息与原始通讯协议信息</p>
     * @return {@code true 阻止将信息发送至客户端} <p>{@code false 继续将信息发送到客户端}</p>
     */
    public boolean UserChatRequests(@NotNull ChatRequestInput ChatMessageInfo)
    {
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
        instance.getPluginManager().callEvent(chatEvent);
        if (chatEvent.isCancelled())
        {
            //插件表明取消此事件，就不要额外处理了
            return true;
        }
        ChatFormat(ChatMessageInfo);
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
            CustomVar.Command CommandInformation = instance.getServerAPI().CommandFormat(chatMessageInfo.getChatMessage());

            //插件事件处理
            UserCommandEvent event = new UserCommandEvent(chatMessageInfo.getUser(), CommandInformation);
            instance.getPluginManager().callEvent(event);
            if (event.isCancelled()) {
                return true;
            }
            CommandRequest0(chatMessageInfo.getUser(),instance.getServerAPI().CommandFormat(chatMessageInfo.getChatMessage()));
            return true;
        }
        return false;
    }

    /**
     * 实际的指令处理程序
     * @param User 用户
     * @param command 指令
     */
    public void CommandRequest0(@NotNull user User, @NotNull CustomVar.Command command)
    {
        final api API = instance.getServerAPI();
        try {
            //插件指令处理
            if (instance.getPluginManager().RequestPluginCommand(command,User))
            {
                return;
            }
            switch (command.Command()) {
                case "/about" -> {
                    API.SendMessageToUser(User, "JavaIM是根据GNU General Public License v3.0开源的自由程序（开源软件）");
                    API.SendMessageToUser(User, "主仓库位于：https://github.com/JavaIM/JavaIM");
                    API.SendMessageToUser(User, "主要开发者名单：");
                    API.SendMessageToUser(User, "QiLechan（柒楽）");
                    API.SendMessageToUser(User, "AlexLiuDev233 （阿白）");
                }
                case "/kick-all-user-and-free-memory" -> {
                    if (!User.isServer()) {
                        API.SendMessageToUser(User,
                                "此命令只能由服务端执行！");
                        break;
                    }
                    API.SendMessageToAllClient("服务端正在强制清理资源，请重新登录！");
                    List<user> users = API.GetValidClientList(false);
                    for (user user : users) {
                        user.UserDisconnect();
                    }
                    System.gc();
                    instance.getLogger().info("已经完成内存释放，并且踢出了所有用户");
                }
                case "/help" -> {
                    API.SendMessageToUser(User, "JavaIM服务器帮助");
                    API.SendMessageToUser(User, "/about 查询此程序有关的信息");
                    API.SendMessageToUser(User, "/help 显示服务器帮助信息");
                    API.SendMessageToUser(User, "/tell <用户> <消息> 发送私聊");
                    if (Permission.ADMIN.equals(User.getUserPermission())) {
                        API.SendMessageToUser(User, "/op <用户名> 给予管理员权限");
                        API.SendMessageToUser(User, "/deop <用户名> 剥夺管理员权限");
                        API.SendMessageToUser(User, "/ban <用户名> 封禁一位用户");
                        API.SendMessageToUser(User, "/unban <用户名> 解除一位用户的封禁");
                        API.SendMessageToUser(User, "/quit 安全的退出程序");
                        API.SendMessageToUser(User, "/change-password <用户名> <密码> 强制修改某用户密码");
                        API.SendMessageToUser(User, "/kick <用户名> 踢出某用户");
                        API.SendMessageToUser(User, "/Send-UnModify-Message <消息> 发送不会被服务端修改的消息");
                    }
                    if (User.isServer()) {
                        API.SendMessageToUser(User, "/kick-all-user-and-free-memory 踢出所有用户，并且尽可能释放内存");
                    }
                    if (instance.getPluginManager().getPluginNumber() > 0)
                        API.SendMessageToUser(User,"插件指令帮助");
                    for (String msg : instance.getPluginManager().getPluginCommandsDescription())
                    {
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
                        try {
                            user targetUser = API.GetUserByUserName(command.argv()[0]);
                            if (Permission.ADMIN.equals(targetUser.getUserPermission())) {
                                API.SendMessageToUser(User, "无法给予权限，对方已是管理员");
                            } else {
                                targetUser.SetUserPermission(1);
                                API.SendMessageToUser(User, "已将" + targetUser.getUserName() + "设为管理员");
                                API.SendMessageToUser(targetUser, User.getUserName() + "已将你设为管理员");
                            }
                        } catch (AccountNotFoundException e) {
                            API.SendMessageToUser(User, "您所输入的用户不存在");
                        }
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
                        try {
                            user targetUser = API.GetUserByUserName(command.argv()[0]);
                            if (Permission.ADMIN.equals(targetUser.getUserPermission())) {
                                targetUser.SetUserPermission(0);
                                API.SendMessageToUser(User, "已剥夺" + targetUser.getUserName() + "的管理员权限");
                            } else {
                                API.SendMessageToUser(User, "无法剥夺权限，对方不是管理员");
                            }
                        } catch (AccountNotFoundException e) {
                            API.SendMessageToUser(User, "您所输入的用户不存在");
                        }
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
                        try {
                            user targetUser = API.GetUserByUserName(command.argv()[0]);
                            if (Permission.BAN.equals(targetUser.getUserPermission())) {
                                API.SendMessageToUser(User, "无法封禁，对方已被封禁");
                            } else {
                                targetUser.SetUserPermission(-1);
                                targetUser.UserDisconnect();
                                API.SendMessageToUser(User, "已将" + targetUser.getUserName() + "封禁");
                            }
                        } catch (AccountNotFoundException e) {
                            API.SendMessageToUser(User, "您所输入的用户不存在");
                        }
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
                        userInformation information = mapper.getUserByName(command.argv()[0]);
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
                    for (user requestUser : instance.getUsers()) {
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
                            try {
                                API.ChangeUserPassword(
                                        API.GetUserByUserName(
                                                command.argv()[1]
                                        ),
                                        command.argv()[2]);//根据用户名获取用户，并强制修改密码
                            } catch (AccountNotFoundException e) {
                                API.SendMessageToUser(User, "无法找到用户：" + command.argv()[1]);
                                return;
                            }
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
                case "/Send-UnModify-Message" -> {
                    if (!(Permission.ADMIN.equals(User.getUserPermission()))) {
                        API.SendMessageToUser(User, "你没有权限这样做");
                        break;
                    }
                    if (command.argv().length == 0)
                    {
                        API.SendMessageToUser(User, "语法错误，正确的语法为：/Send-UnModify-Message <消息>");
                        break;
                    }
                    StringBuilder stringBuilder = new StringBuilder();
                    for (String arg : command.argv())
                    {
                        stringBuilder.append(arg).append(" ");
                        if (!stringBuilder.isEmpty())
                        {
                            stringBuilder.deleteCharAt(stringBuilder.length() - 1);
                        }
                    }

                    String arg = stringBuilder.toString();
                    API.SendMessageToAllClient(arg);
                    instance.getLogger().ChatMsg(arg);
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

                        String ChatMessage = stringBuilder.toString();
                        ChatRequestInput input = new ChatRequestInput(User, ChatMessage);
                        ChatFormat(input);

                        if (command.argv()[0].equals("Server"))//当私聊目标为后台时
                        {
                            instance.getLogger().ChatMsg("[私聊] " + input.getChatMessage());
                            API.SendMessageToUser(User, "你对" + command.argv()[0] + "发送了私聊：" + ChatMessage);
                            break;
                        }
                        try {
                            API.SendMessageToUser(API.GetUserByUserName(command.argv()[0]), "[私聊] " + input.getChatMessage());
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
        formatter = new SimpleDateFormat("yyyy-MM-dd 'at' HH:mm:ss z");
    }
}
