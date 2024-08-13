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
package org.yuezhikong.Server.request;

import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jline.reader.Completer;
import org.yuezhikong.Server.IServer;
import org.yuezhikong.Server.ServerTools;
import org.yuezhikong.Server.userData.Permission;
import org.yuezhikong.Server.userData.user;
import org.yuezhikong.Server.command.InternalCommands;
import org.yuezhikong.Server.plugin.plugin.Plugin;
import org.yuezhikong.Server.plugin.event.events.User.UserChatEvent;
import org.yuezhikong.Server.plugin.event.events.User.UserCommandEvent;
import org.yuezhikong.utils.checks;

import java.util.Collections;
import java.util.List;

@Slf4j
public class ChatRequestImpl implements ChatRequest {
    private final JavaIMCompleter completer = new JavaIMCompleter();
    private final IServer instance = ServerTools.getServerInstanceOrThrow();

    public Completer getCompleter() {
        return completer;
    }

    /**
     * 注册指令
     *
     * @param information 指令信息
     */
    @ApiStatus.Internal
    private void registerCommand0(CommandInformation information) {
        this.completer.informations.add(information);
    }

    public ChatRequestImpl() {
        registerCommand0(new CommandInformation(null, new InternalCommands.AboutCommand(), "about"));
        registerCommand0(new CommandInformation(null, new InternalCommands.CrashCommand(), "crash"));
        registerCommand0(new CommandInformation(null, new InternalCommands.HelpCommand(), "help"));
        registerCommand0(new CommandInformation(null, new InternalCommands.ListCommand(), "list"));
        registerCommand0(new CommandInformation(null, new InternalCommands.TellCommand(), "tell"));
        registerCommand0(new CommandInformation(null, new InternalCommands.OpCommand(), "op"));
        registerCommand0(new CommandInformation(null, new InternalCommands.DeopCommand(), "deop"));
        registerCommand0(new CommandInformation(null, new InternalCommands.BanCommand(), "ban"));
        registerCommand0(new CommandInformation(null, new InternalCommands.UnbanCommand(), "unban"));
        registerCommand0(new CommandInformation(null, new InternalCommands.QuitCommand(), "quit"));
        registerCommand0(new CommandInformation(null, new InternalCommands.ChangePasswordCommand(), "change-password"));
        registerCommand0(new CommandInformation(null, new InternalCommands.KickCommand(), "kick"));
        registerCommand0(new CommandInformation(null, new InternalCommands.GetUploadFilesCommand(), "getUploadFiles"));
        registerCommand0(new CommandInformation(null, new InternalCommands.RunGCCommand(), "runGC"));
        registerCommand0(new CommandInformation(null, new InternalCommands.DeleteFileByFileIdCommand(), "deleteFileByFileId"));
    }

    @Override
    public void commandRequest(String command, String[] args, user User) {
        try {
            for (CommandInformation information : completer.informations) {
                if (information.command().equals(command)) {
                    if (!information.commandInstance().execute(command, args, User)) {
                        instance.getServerAPI().sendMessageToUser(
                                User,
                                "语法错误! 正确的语法为：" + information.commandInstance().getUsage()
                        );
                        return;
                    }
                    if (information.commandInstance().isAllowBroadcastCommandRunning()) {
                        StringBuilder stringBuilder = new StringBuilder("/").append(command);
                        stringBuilder.append(" ");
                        for (String arg : args) {
                            stringBuilder.append(arg).append(" ");
                        }

                        stringBuilder.deleteCharAt(stringBuilder.length() - 1);
                        String orig_Command = stringBuilder.toString();
                        String tipMessage = String.format("%s 执行了指令: %s", User.getUserName(), orig_Command);
                        log.info(tipMessage);
                        for (user sendUser : instance.getServerAPI().getValidUserList(true)) {
                            if (!Permission.ADMIN.equals(sendUser.getUserPermission()))
                                continue;
                            instance.getServerAPI().sendMessageToUser(sendUser, tipMessage);
                        }
                    }
                    return;
                }
            }
            instance.getServerAPI().sendMessageToUser(User, "未知的命令！请输入/help查看帮助！");
        } catch (Throwable t) {
            log.error("在执行{}命令时出现错误!", command, t);
            instance.getServerAPI().sendMessageToUser(User, "在执行此命令的过程中出现未知的错误");
        }
    }

    @Override
    public List<CommandInformation> getRegisterCommands() {
        return Collections.unmodifiableList(completer.informations);
    }

    @Override
    public void registerCommand(CommandInformation information) {
        checks.checkArgument(information == null || information.plugin() == null || information.command() == null,
                "CommandInformation can not be null");
        for (CommandInformation information1 : completer.informations) {
            checks.checkState(
                    information1.command().equals(information.command()),
                    String.format("Command %s has been already registered", information.command())
            );
        }
        registerCommand0(information);
    }

    @Override
    public void unregisterCommand(CommandInformation information) {
        checks.checkArgument(information.plugin() == null, "can not unregister system command");
        completer.informations.remove(information);
    }

    @Override
    public void unregisterCommand(Plugin plugin) {
        checks.checkArgument(plugin == null, "can not unregister system command");
        completer.informations.removeIf((information) -> information.plugin() == plugin);
    }

    /**
     * 对于用户聊天信息的特别处理
     *
     * @param user    用户
     * @param message 消息
     * @return {@code true 阻止将信息发送至客户端} <p>{@code false 继续将信息发送到客户端}</p>
     */
    public boolean userChatRequests(@NotNull user user, @NotNull String message) {
        if (message.isEmpty())
            //如果发送过来的消息是空的，就没必要再继续处理了
            return true;

        //执行命令处理程序
        if (message.startsWith("/")) {
            String[] tmp = message.split("\\s+");

            String command = tmp[0].substring(1);
            String[] args = new String[tmp.length - 1];
            System.arraycopy(tmp, 1, args, 0, tmp.length - 1);

            UserCommandEvent commandEvent = new UserCommandEvent(user, command, args);
            instance.getPluginManager().callEvent(commandEvent);
            if (commandEvent.isCancelled())
                return true;
            commandRequest(command, args, user);
            return true;
        }

        //执行插件处理程序
        UserChatEvent chatEvent = new UserChatEvent(user, message);
        instance.getPluginManager().callEvent(chatEvent);
        // 根据插件返回，决定是否继续发送
        return chatEvent.isCancelled();
    }
}
