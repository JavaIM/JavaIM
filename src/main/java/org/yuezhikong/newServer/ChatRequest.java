package org.yuezhikong.newServer;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.yuezhikong.CodeDynamicConfig;
import org.yuezhikong.newServer.UserData.Permission;
import org.yuezhikong.newServer.UserData.user;
import org.yuezhikong.utils.CustomVar;
import org.yuezhikong.utils.DataBase.Database;
import org.yuezhikong.utils.Protocol.NormalProtocol;
import org.yuezhikong.utils.SaveStackTrace;

import javax.security.auth.login.AccountNotFoundException;
import java.sql.*;
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
            CustomVar.Command CommandInformation = api.CommandFormat(chatMessageInfo.getChatMessage());
            switch (CommandInformation.Command())
            {
                case "/about" -> {
                    api.SendMessageToUser(chatMessageInfo.getUser(),"JavaIM是根据GNU General Public License v3.0开源的自由程序（开源软件）");
                    api.SendMessageToUser(chatMessageInfo.getUser(),"主仓库位于：https://github.com/JavaIM/JavaIM");
                    api.SendMessageToUser(chatMessageInfo.getUser(),"主要开发者名单：");
                    api.SendMessageToUser(chatMessageInfo.getUser(),"QiLechan（柒楽）");
                    api.SendMessageToUser(chatMessageInfo.getUser(),"AlexLiuDev233 （阿白）");
                }
                case "/help" -> {
                    api.SendMessageToUser(chatMessageInfo.getUser(),"JavaIM Server的服务器帮助");
                    api.SendMessageToUser(chatMessageInfo.getUser(),"/about 查询此程序有关的信息");
                    api.SendMessageToUser(chatMessageInfo.getUser(),"/help 显示服务器帮助信息");
                    if (Permission.ADMIN.equals(chatMessageInfo.getUser().getUserPermission()))
                    {
                        api.SendMessageToUser(chatMessageInfo.getUser(),"/op <用户名> 给予管理员权限");
                        api.SendMessageToUser(chatMessageInfo.getUser(),"/deop <用户名> 剥夺管理员权限");
                        api.SendMessageToUser(chatMessageInfo.getUser(),"/ban <用户名> 封禁一位用户");
                        api.SendMessageToUser(chatMessageInfo.getUser(),"/unban <用户名> 解除一位用户的封禁");
                    }
                }
                case "/op" -> {
                    if (CommandInformation.argv().length == 1) {
                        try {
                            user User = api.GetUserByUserName(CommandInformation.argv()[0], ServerMain.getServer(), true);
                            if (Permission.ADMIN.equals(User.getUserPermission()))
                            {
                                api.SendMessageToUser(chatMessageInfo.getUser(),"无法给予权限，对方已是管理员");
                            }
                            else {
                                User.SetUserPermission(1, false);
                                api.SendMessageToUser(chatMessageInfo.getUser(), "已将" + User.getUserName() + "设为管理员");
                                api.SendMessageToUser(User,"["+chatMessageInfo.getUser()+":已将" + User.getUserName() + "设为管理员]");
                            }
                        } catch (AccountNotFoundException e) {
                            api.SendMessageToUser(chatMessageInfo.getUser(),"您所输入的用户不存在");
                        }
                    }
                    else
                    {
                        api.SendMessageToUser(chatMessageInfo.getUser(),"语法错误，正确的语法为：/op <用户名>");
                    }
                }
                case "/deop" -> {
                    if (CommandInformation.argv().length == 1) {
                        try {
                            user User = api.GetUserByUserName(CommandInformation.argv()[0], ServerMain.getServer(), true);
                            if (Permission.ADMIN.equals(User.getUserPermission()))
                            {
                                User.SetUserPermission(0, false);
                                api.SendMessageToUser(chatMessageInfo.getUser(), "已剥夺" + User.getUserName() + "的管理员权限");
                                api.SendMessageToUser(User,"["+chatMessageInfo.getUser()+":已剥夺"+ User.getUserName()+"的管理员权限]");
                            }
                            else {
                                api.SendMessageToUser(chatMessageInfo.getUser(),"无法剥夺权限，对方不是管理员");
                            }
                        } catch (AccountNotFoundException e) {
                            api.SendMessageToUser(chatMessageInfo.getUser(),"您所输入的用户不存在");
                        }
                    }
                    else
                    {
                        api.SendMessageToUser(chatMessageInfo.getUser(),"语法错误，正确的语法为：/deop <用户名>");
                    }
                }
                case "/ban" -> {
                    if (CommandInformation.argv().length == 1) {
                        try {
                            user User = api.GetUserByUserName(CommandInformation.argv()[0], ServerMain.getServer(), true);
                            if (Permission.BAN.equals(User.getUserPermission()))
                            {
                                api.SendMessageToUser(chatMessageInfo.getUser(),"无法封禁，对方已被封禁");
                            }
                            else {
                                User.SetUserPermission(-1, false);
                                User.UserDisconnect();
                                api.SendMessageToUser(chatMessageInfo.getUser(), "已将" + User.getUserName() + "封禁");
                            }
                        } catch (AccountNotFoundException e) {
                            api.SendMessageToUser(chatMessageInfo.getUser(),"您所输入的用户不存在");
                        }
                    }
                    else
                    {
                        api.SendMessageToUser(chatMessageInfo.getUser(),"语法错误，正确的语法为：/ban <用户名>");
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
                                            api.SendMessageToUser(chatMessageInfo.getUser(), "已解封"+CommandInformation.argv()[0]);
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
                        api.SendMessageToUser(chatMessageInfo.getUser(),"语法错误，正确的语法为：/unban <用户名>");
                    }
                }
                default -> api.SendMessageToUser(chatMessageInfo.getUser(),"未知的命令！请输入/help查看帮助！");
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
