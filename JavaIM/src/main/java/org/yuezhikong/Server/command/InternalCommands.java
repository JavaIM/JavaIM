package org.yuezhikong.Server.command;

import com.google.gson.Gson;
import lombok.extern.slf4j.Slf4j;
import org.jline.builtins.Completers;
import org.jline.reader.Candidate;
import org.jline.reader.LineReader;
import org.jline.reader.ParsedLine;
import org.jline.reader.impl.completer.NullCompleter;
import org.yuezhikong.Main;
import org.yuezhikong.Server.IServer;
import org.yuezhikong.Server.ServerTools;
import org.yuezhikong.Server.UserData.Permission;
import org.yuezhikong.Server.UserData.tcpUser.tcpUser;
import org.yuezhikong.Server.UserData.user;
import org.yuezhikong.Server.UserData.userInformation;
import org.yuezhikong.Server.UserData.userUploadFile;
import org.yuezhikong.Server.api.api;
import org.yuezhikong.utils.Protocol.ChatProtocol;
import org.yuezhikong.utils.SHA256;
import org.yuezhikong.utils.database.dao.userInformationDao;
import org.yuezhikong.utils.database.dao.userUploadFileDao;
import org.yuezhikong.utils.logging.CustomLogger;

import javax.security.auth.login.AccountNotFoundException;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

import static org.jline.builtins.Completers.TreeCompleter.node;

@Slf4j
public class InternalCommands {
    private InternalCommands() {}
    public static class AboutCommand implements Command {

        @Override
        public void complete(LineReader reader, ParsedLine line, List<Candidate> candidates) {
            new Completers.TreeCompleter(
                    node("/about",
                            node(NullCompleter.INSTANCE)
                    )
            ).complete(reader, line, candidates);
        }

        @Override
        public boolean execute(String command, String[] args, user User) {
            if (args.length != 0)
                return false;
            api serverAPI = ServerTools.getServerInstanceOrThrow().getServerAPI();
            serverAPI.SendMessageToUser(User, "JavaIM是根据GNU General Public License v3.0开源的自由程序（开源软件）");
            serverAPI.SendMessageToUser(User, "主仓库于：https://github.com/JavaIM/JavaIM");
            serverAPI.SendMessageToUser(User, "主要开发者名单：");
            serverAPI.SendMessageToUser(User, "QiLechan（柒楽）");
            serverAPI.SendMessageToUser(User, "AlexLiuDev233 （阿白）");
            return true;
        }

        @Override
        public String getDescription() {
            return "查询此程序有关的信息";
        }

        @Override
        public String getUsage() {
            return "/about";
        }

        @Override
        public boolean isAllowBroadcastCommandRunning() {
            return true;
        }
    }
    public static class HelpCommand implements Command {

        @Override
        public void complete(LineReader reader, ParsedLine line, List<Candidate> candidates) {
            new Completers.TreeCompleter(
                    node("/help",
                            node(NullCompleter.INSTANCE)
                    )
            ).complete(reader, line, candidates);
        }

        @Override
        public boolean execute(String command, String[] args, user User) {
            if (args.length != 0)
                return false;
            IServer serverInstance = ServerTools.getServerInstanceOrThrow();
            api serverAPI = serverInstance.getServerAPI();
            serverAPI.SendMessageToUser(User, "JavaIM服务器帮助");
            serverInstance.getRequest().getRegisterCommands().forEach(information ->
                    serverAPI.SendMessageToUser(User, information.commandInstance().getUsage() + " " + information.commandInstance().getDescription())
            );
            return true;
        }

        @Override
        public String getDescription() {
            return "查询帮助";
        }

        @Override
        public String getUsage() {
            return "/help";
        }

        @Override
        public boolean isAllowBroadcastCommandRunning() {
            return true;
        }
    }
    public static class ListCommand implements Command {

        @Override
        public void complete(LineReader reader, ParsedLine line, List<Candidate> candidates) {
            new Completers.TreeCompleter(
                    node("/list",
                            node(NullCompleter.INSTANCE)
                    )
            ).complete(reader, line, candidates);
        }

        @Override
        public boolean execute(String command, String[] args, user User) {
            if (args.length != 0)
                return false;
            api serverAPI = ServerTools.getServerInstanceOrThrow().getServerAPI();
            List<user> onlineUserList = serverAPI.GetValidClientList(true);
            if (Permission.ADMIN.equals(User.getUserPermission()))
                onlineUserList.forEach((user) -> {
                    if (user instanceof tcpUser)
                        serverAPI.SendMessageToUser(User,
                                String.format("%s 权限：%s IP地址：%s",
                                        user.getUserName(),
                                        user.getUserPermission().toString(),
                                        ((tcpUser) user).getNetworkClient().getSocketAddress()
                                )
                        );
                    else
                        serverAPI.SendMessageToUser(User, String.format("%s 权限：%s", user.getUserName(), user.getUserPermission().toString()));
                });
            else
                onlineUserList.forEach((user) ->
                        serverAPI.SendMessageToUser(User, String.format("%s 权限：%s", user.getUserName(), user.getUserPermission().toString()))
                );
            return true;
        }

        @Override
        public String getDescription() {
            return "显示在线用户列表";
        }

        @Override
        public String getUsage() {
            return "/list";
        }

        @Override
        public boolean isAllowBroadcastCommandRunning() {
            return true;
        }
    }
    public static class TellCommand implements Command {

        @Override
        public void complete(LineReader reader, ParsedLine line, List<Candidate> candidates) {
            List<Completers.TreeCompleter.Node> canTellUsers = new ArrayList<>();
            canTellUsers.add(node("Server", node(NullCompleter.INSTANCE)));
            ServerTools.getServerInstanceOrThrow().getServerAPI().GetValidClientList(true).forEach((user) -> {
                if (!user.isUserLogged())
                    return;
                canTellUsers.add(node(user.getUserName(), node(NullCompleter.INSTANCE)));
            });

            Object[] nodes = new Object[canTellUsers.size() + 1];
            nodes[0] = "/tell";
            System.arraycopy(canTellUsers.toArray(), 0, nodes, 1, canTellUsers.size());
            new Completers.TreeCompleter(
                    node(nodes)
            ).complete(reader, line, candidates);
        }

        @Override
        public boolean execute(String command, String[] args, user User) {
            if (args.length < 2)
                return false;
            api serverAPI = ServerTools.getServerInstanceOrThrow().getServerAPI();
            StringBuilder stringBuilder = new StringBuilder();
            for (String arg : args)
            {
                stringBuilder.append(arg).append(" ");
            }

            if (!stringBuilder.isEmpty())
            {
                stringBuilder.deleteCharAt(stringBuilder.length() - 1);
            }

            stringBuilder.delete(0,args[0].length() + 1);
            stringBuilder.insert(0,"[私聊] ");

            String ChatMessage = stringBuilder.toString();

            if (args[0].equals("Server"))//当私聊目标为后台时
            {
                ((CustomLogger) log).ChatMsg("["+User.getUserName()+"]:"+ChatMessage);
                serverAPI.SendMessageToUser(User, "你对" + args[0] + "发送了私聊：" + ChatMessage);
                return true;
            }
            try {
                ChatProtocol chatProtocol = new ChatProtocol();
                chatProtocol.setSourceUserName(User.getUserName());
                chatProtocol.setMessage(ChatMessage);
                serverAPI.SendJsonToClient(serverAPI.GetUserByUserName(args[0]),new Gson().toJson(chatProtocol),"ChatProtocol");
                serverAPI.SendMessageToUser(User, "你对" + args[0] + "发送了私聊：" + ChatMessage);
            } catch (AccountNotFoundException e) {
                serverAPI.SendMessageToUser(User, "此用户不存在");
            }
            return true;
        }

        @Override
        public String getDescription() {
            return "私聊某用户";
        }

        @Override
        public String getUsage() {
            return "/tell <目标用户> <消息>";
        }

        @Override
        public boolean isAllowBroadcastCommandRunning() {
            return false;
        }
    }
    public static class OpCommand implements Command {

        @Override
        public void complete(LineReader reader, ParsedLine line, List<Candidate> candidates) {
            List<Completers.TreeCompleter.Node> canControlUsers = new ArrayList<>();
            ServerTools.getServerInstanceOrThrow().getServerAPI().GetValidClientList(true).forEach((user) -> {
                if (!user.isUserLogged())
                    return;
                canControlUsers.add(node(user.getUserName(), node(NullCompleter.INSTANCE)));
            });

            Object[] nodes = new Object[canControlUsers.size() + 1];
            nodes[0] = "/op";
            System.arraycopy(canControlUsers.toArray(), 0, nodes, 1, canControlUsers.size());
            new Completers.TreeCompleter(
                    node(nodes)
            ).complete(reader, line, candidates);
        }

        @Override
        public boolean execute(String command, String[] args, user User) {
            if (args.length != 1)
                return false;
            api serverAPI = ServerTools.getServerInstanceOrThrow().getServerAPI();
            if (!(Permission.ADMIN.equals(User.getUserPermission()))) {
                serverAPI.SendMessageToUser(User, "你没有权限这样做");
                return true;
            }

            userInformationDao mapper = ServerTools.getServerInstanceOrThrow().getSqlSession().getMapper(userInformationDao.class);
            userInformation information = mapper.getUser(null,args[0],null,null);
            if (information == null) {
                serverAPI.SendMessageToUser(User, "您所操作的用户从来没有来到过本服务器");
                return true;
            }
            if (information.getPermission() != 1)
                serverAPI.SendMessageToUser(User, "已将" + information.getUserName() + "设为服务器管理员");
            else {
                serverAPI.SendMessageToUser(User, "无法设置，对方已是管理员");
                return true;
            }

            information.setPermission(1);
            mapper.updateUser(information);

            user targetUser;
            try {
                targetUser = serverAPI.GetUserByUserName(args[0]);
            } catch (AccountNotFoundException e) {
                return true;
            }
            serverAPI.SendMessageToUser(targetUser, "您已被设置为服务器管理员");
            targetUser.SetUserPermission(1);
            return true;
        }

        @Override
        public String getDescription() {
            return "设置管理员";
        }

        @Override
        public String getUsage() {
            return "/op <目标用户>";
        }

        @Override
        public boolean isAllowBroadcastCommandRunning() {
            return true;
        }
    }
    public static class DeopCommand implements Command {

        @Override
        public void complete(LineReader reader, ParsedLine line, List<Candidate> candidates) {
            List<Completers.TreeCompleter.Node> canControlUsers = new ArrayList<>();
            ServerTools.getServerInstanceOrThrow().getServerAPI().GetValidClientList(true).forEach((user) -> {
                if (!user.isUserLogged())
                    return;
                canControlUsers.add(node(user.getUserName(), node(NullCompleter.INSTANCE)));
            });

            Object[] nodes = new Object[canControlUsers.size() + 1];
            nodes[0] = "/deop";
            System.arraycopy(canControlUsers.toArray(), 0, nodes, 1, canControlUsers.size());
            new Completers.TreeCompleter(
                    node(nodes)
            ).complete(reader, line, candidates);
        }

        @Override
        public boolean execute(String command, String[] args, user User) {
            if (args.length != 1)
                return false;
            api serverAPI = ServerTools.getServerInstanceOrThrow().getServerAPI();
            if (!(Permission.ADMIN.equals(User.getUserPermission()))) {
                serverAPI.SendMessageToUser(User, "你没有权限这样做");
                return true;
            }

            userInformationDao mapper = ServerTools.getServerInstanceOrThrow().getSqlSession().getMapper(userInformationDao.class);
            userInformation information = mapper.getUser(null,args[0],null,null);
            if (information == null) {
                serverAPI.SendMessageToUser(User, "您所操作的用户从来没有来到过本服务器");
                return true;
            }
            if (information.getPermission() == 1)
                serverAPI.SendMessageToUser(User, "已夺去" + information.getUserName() + "的管理员权限");
            else {
                serverAPI.SendMessageToUser(User, "无法夺去权限，对方不是管理员");
                return true;
            }

            information.setPermission(0);
            mapper.updateUser(information);

            user targetUser;
            try {
                targetUser = serverAPI.GetUserByUserName(args[0]);
            } catch (AccountNotFoundException e) {
                return true;
            }
            serverAPI.SendMessageToUser(targetUser, "您已被夺去管理员权限");
            targetUser.SetUserPermission(0);
            return true;
        }

        @Override
        public String getDescription() {
            return "取消设置管理员";
        }

        @Override
        public String getUsage() {
            return "/deop <目标用户>";
        }

        @Override
        public boolean isAllowBroadcastCommandRunning() {
            return true;
        }
    }
    public static class BanCommand implements Command {

        @Override
        public void complete(LineReader reader, ParsedLine line, List<Candidate> candidates) {
            List<Completers.TreeCompleter.Node> canControlUsers = new ArrayList<>();
            ServerTools.getServerInstanceOrThrow().getServerAPI().GetValidClientList(true).forEach((user) -> {
                if (!user.isUserLogged())
                    return;
                canControlUsers.add(node(user.getUserName(), node(NullCompleter.INSTANCE)));
            });

            Object[] nodes = new Object[canControlUsers.size() + 1];
            nodes[0] = "/ban";
            System.arraycopy(canControlUsers.toArray(), 0, nodes, 1, canControlUsers.size());
            new Completers.TreeCompleter(
                    node(nodes)
            ).complete(reader, line, candidates);
        }

        @Override
        public boolean execute(String command, String[] args, user User) {
            if (args.length != 1)
                return false;
            api serverAPI = ServerTools.getServerInstanceOrThrow().getServerAPI();
            if (!(Permission.ADMIN.equals(User.getUserPermission()))) {
                serverAPI.SendMessageToUser(User, "你没有权限这样做");
                return true;
            }

            userInformationDao mapper = ServerTools.getServerInstanceOrThrow().getSqlSession().getMapper(userInformationDao.class);
            userInformation information = mapper.getUser(null,args[0],null,null);
            if (information == null) {
                serverAPI.SendMessageToUser(User, "您所操作的用户从来没有来到过本服务器");
                return true;
            }
            information.setPermission(-1);
            mapper.updateUser(information);

            user kickUser;
            try {
                kickUser = serverAPI.GetUserByUserName(args[0]);
            } catch (AccountNotFoundException e) {
                return true;
            }
            serverAPI.SendMessageToUser(kickUser, "您已被封禁");
            kickUser.UserDisconnect();
            return true;
        }

        @Override
        public String getDescription() {
            return "封禁用户";
        }

        @Override
        public String getUsage() {
            return "/ban <目标用户>";
        }

        @Override
        public boolean isAllowBroadcastCommandRunning() {
            return true;
        }
    }
    public static class UnbanCommand implements Command {

        @Override
        public void complete(LineReader reader, ParsedLine line, List<Candidate> candidates) {
            List<Completers.TreeCompleter.Node> canControlUsers = new ArrayList<>();
            ServerTools.getServerInstanceOrThrow().getServerAPI().GetValidClientList(true).forEach((user) -> {
                if (!user.isUserLogged())
                    return;
                canControlUsers.add(node(user.getUserName(), node(NullCompleter.INSTANCE)));
            });

            Object[] nodes = new Object[canControlUsers.size() + 1];
            nodes[0] = "/unban";
            System.arraycopy(canControlUsers.toArray(), 0, nodes, 1, canControlUsers.size());
            new Completers.TreeCompleter(
                    node(nodes)
            ).complete(reader, line, candidates);
        }

        @Override
        public boolean execute(String command, String[] args, user User) {
            if (args.length != 1)
                return false;
            api serverAPI = ServerTools.getServerInstanceOrThrow().getServerAPI();
            if (!(Permission.ADMIN.equals(User.getUserPermission()))) {
                serverAPI.SendMessageToUser(User, "你没有权限这样做");
                return true;
            }

            userInformationDao mapper = ServerTools.getServerInstanceOrThrow().getSqlSession().getMapper(userInformationDao.class);
            userInformation information = mapper.getUser(null,args[0],null,null);
            if (information == null) {
                serverAPI.SendMessageToUser(User, "您所操作的用户从来没有来到过本服务器");
                return true;
            }
            information.setPermission(0);
            mapper.updateUser(information);
            return true;
        }

        @Override
        public String getDescription() {
            return "解封用户";
        }

        @Override
        public String getUsage() {
            return "/unban <目标用户>";
        }

        @Override
        public boolean isAllowBroadcastCommandRunning() {
            return true;
        }
    }
    public static class QuitCommand implements Command {

        @Override
        public void complete(LineReader reader, ParsedLine line, List<Candidate> candidates) {
            new Completers.TreeCompleter(
                    node("/quit",
                            node(NullCompleter.INSTANCE)
                    )
            ).complete(reader, line, candidates);
        }

        @Override
        public boolean execute(String command, String[] args, user User) {
            if (args.length != 0)
                return false;
            IServer serverInstance = ServerTools.getServerInstanceOrThrow();
            api serverAPI = serverInstance.getServerAPI();
            if (!(Permission.ADMIN.equals(User.getUserPermission()))) {
                serverAPI.SendMessageToUser(User, "你没有权限这样做");
                return true;
            }
            serverInstance.stop();
            return true;
        }

        @Override
        public String getDescription() {
            return "关闭服务器";
        }

        @Override
        public String getUsage() {
            return "/quit";
        }

        @Override
        public boolean isAllowBroadcastCommandRunning() {
            return true;
        }
    }
    public static class ChangePasswordCommand implements Command {

        @Override
        public void complete(LineReader reader, ParsedLine line, List<Candidate> candidates) {
            List<Completers.TreeCompleter.Node> canControlUsers = new ArrayList<>();
            ServerTools.getServerInstanceOrThrow().getServerAPI().GetValidClientList(true).forEach((user) -> {
                if (!user.isUserLogged())
                    return;
                canControlUsers.add(node(user.getUserName(), node(NullCompleter.INSTANCE)));
            });

            Object[] nodes = new Object[canControlUsers.size() + 1];
            nodes[0] = "/change-password";
            System.arraycopy(canControlUsers.toArray(), 0, nodes, 1, canControlUsers.size());
            new Completers.TreeCompleter(
                    node(nodes)
            ).complete(reader, line, candidates);
        }

        @Override
        public boolean execute(String command, String[] args, user User) {
            if (args.length != 2)
                return false;
            api serverAPI = ServerTools.getServerInstanceOrThrow().getServerAPI();
            if (!(Permission.ADMIN.equals(User.getUserPermission()))) {
                serverAPI.SendMessageToUser(User, "你没有权限这样做");
                return true;
            }

            userInformationDao mapper = ServerTools.getServerInstanceOrThrow().getSqlSession().getMapper(userInformationDao.class);
            userInformation information = mapper.getUser(null,args[0],null,null);
            if (information == null) {
                serverAPI.SendMessageToUser(User, "您所操作的用户从来没有来到过本服务器");
                return true;
            }
            information.setPasswd(SHA256.sha256(args[1]));
            mapper.updateUser(information);
            serverAPI.SendMessageToUser(User, "操作成功完成。");
            return true;
        }

        @Override
        public String getDescription() {
            return "修改用户的密码";
        }

        @Override
        public String getUsage() {
            return "/change-password <目标用户> <密码>";
        }

        @Override
        public boolean isAllowBroadcastCommandRunning() {
            return false;
        }
    }
    public static class KickCommand implements Command {

        @Override
        public void complete(LineReader reader, ParsedLine line, List<Candidate> candidates) {
            List<Completers.TreeCompleter.Node> canControlUsers = new ArrayList<>();
            ServerTools.getServerInstanceOrThrow().getServerAPI().GetValidClientList(true).forEach((user) -> {
                if (!user.isUserLogged())
                    return;
                canControlUsers.add(node(user.getUserName(), node(NullCompleter.INSTANCE)));
            });

            Object[] nodes = new Object[canControlUsers.size() + 1];
            nodes[0] = "/kick";
            System.arraycopy(canControlUsers.toArray(), 0, nodes, 1, canControlUsers.size());
            new Completers.TreeCompleter(
                    node(nodes)
            ).complete(reader, line, candidates);
        }

        @Override
        public boolean execute(String command, String[] args, user User) {
            if (args.length != 1)
                return false;
            api serverAPI = ServerTools.getServerInstanceOrThrow().getServerAPI();
            if (!(Permission.ADMIN.equals(User.getUserPermission()))) {
                serverAPI.SendMessageToUser(User, "你没有权限这样做");
                return true;
            }

            user kickUser;
            try {
                kickUser = serverAPI.GetUserByUserName(args[0]);
            } catch (AccountNotFoundException e) {
                serverAPI.SendMessageToUser(User, "此用户不存在");
                return true;
            }
            serverAPI.SendMessageToUser(kickUser, "您已被踢出此服务器");
            String UserName = kickUser.getUserName();
            kickUser.UserDisconnect();
            serverAPI.SendMessageToUser(User, "已成功踢出用户：" + UserName);
            return true;
        }

        @Override
        public String getDescription() {
            return "踢出用户";
        }

        @Override
        public String getUsage() {
            return "/kick <目标用户>";
        }

        @Override
        public boolean isAllowBroadcastCommandRunning() {
            return true;
        }
    }
    public static class GetUploadFilesCommand implements Command {

        @Override
        public void complete(LineReader reader, ParsedLine line, List<Candidate> candidates) {
            new Completers.TreeCompleter(
                    node("/getUploadFiles",
                            node(NullCompleter.INSTANCE)
                    )
            ).complete(reader, line, candidates);
        }

        @Override
        public boolean execute(String command, String[] args, user User) {
            if (args.length != 0)
                return false;
            IServer instance = ServerTools.getServerInstanceOrThrow();
            api serverAPI = instance.getServerAPI();
            if (!(Permission.ADMIN.equals(User.getUserPermission()))) {
                serverAPI.SendMessageToUser(User, "你没有权限这样做");
                return true;
            }

            userUploadFileDao mapper = instance.getSqlSession().getMapper(userUploadFileDao.class);
            List<userUploadFile> uploadFiles = mapper.getUploadFiles();
            if (uploadFiles == null || uploadFiles.isEmpty()) {
                serverAPI.SendMessageToUser(User,"没有上传的文件");
                return true;
            }
            uploadFiles.forEach((uploadFile) -> {
                try {
                    serverAPI.SendMessageToUser(User, String.format("文件Id: %s, 用户Id: %s(用户名：%s)，文件名：%s",
                            uploadFile.getOwnFile(),
                            uploadFile.getUserId(),
                            serverAPI.GetUserByUserId(uploadFile.getUserId()).getUserName(),
                            uploadFile.getOrigFileName()));
                } catch (AccountNotFoundException e) {
                    serverAPI.SendMessageToUser(User, String.format("文件Id: %s, 用户Id: %s，文件名：%s",
                            uploadFile.getOwnFile(),
                            uploadFile.getUserId(),
                            uploadFile.getOrigFileName()));
                }
            });
            return true;
        }

        @Override
        public String getDescription() {
            return "获取上传的文件";
        }

        @Override
        public String getUsage() {
            return "/getUploadFiles";
        }

        @Override
        public boolean isAllowBroadcastCommandRunning() {
            return true;
        }
    }
    public static class RunGCCommand implements Command {

        @Override
        public void complete(LineReader reader, ParsedLine line, List<Candidate> candidates) {
            new Completers.TreeCompleter(
                    node("/runGC",
                            node(NullCompleter.INSTANCE)
                    )
            ).complete(reader, line, candidates);
        }

        @Override
        public boolean execute(String command, String[] args, user User) {
            if (args.length != 0)
                return false;
            if (!User.isServer()) {
                ServerTools.getServerInstanceOrThrow().getServerAPI().SendMessageToUser(User, "此命令只能由服务端执行！");
                return true;
            }

            System.gc();
            log.info("已经完成垃圾回收");
            return true;
        }

        @Override
        public String getDescription() {
            return "初始化垃圾回收";
        }

        @Override
        public String getUsage() {
            return "/runGC";
        }

        @Override
        public boolean isAllowBroadcastCommandRunning() {
            return true;
        }
    }
    public static class DeleteFileByFileIdCommand implements Command {

        @Override
        public void complete(LineReader reader, ParsedLine line, List<Candidate> candidates) {
            List<Completers.TreeCompleter.Node> canControlUsers = new ArrayList<>();

            if (!reader.getTerminal().equals(Main.getTerminal())) {
                new Completers.TreeCompleter(
                        node("/deleteFileByFileId",
                                node(NullCompleter.INSTANCE)
                        )
                ).complete(reader, line, candidates);
                return;
            }
            IServer server = ServerTools.getServerInstanceOrThrow();
            userUploadFileDao mapper = server.getSqlSession().getMapper(userUploadFileDao.class);
            List<userUploadFile> uploadFiles = mapper.getUploadFiles();
            if (!(uploadFiles == null || uploadFiles.isEmpty())) {
                uploadFiles.forEach((uploadFile) ->
                        canControlUsers.add(node(uploadFile.getOwnFile(), node(NullCompleter.INSTANCE))));
            }
            Object[] nodes = new Object[canControlUsers.size() + 1];
            nodes[0] = "/deleteFileByFileId";
            System.arraycopy(canControlUsers.toArray(), 0, nodes, 1, canControlUsers.size());
            new Completers.TreeCompleter(
                    node(nodes)
            ).complete(reader, line, candidates);
        }

        @Override
        public boolean execute(String command, String[] args, user User) {
            if (args.length != 1)
                return false;
            IServer server = ServerTools.getServerInstanceOrThrow();
            api serverAPI = server.getServerAPI();
            if (!(Permission.ADMIN.equals(User.getUserPermission()))) {
                serverAPI.SendMessageToUser(User, "你没有权限这样做");
                return true;
            }

            userUploadFileDao mapper = server.getSqlSession().getMapper(userUploadFileDao.class);
            userUploadFile uploadFile = mapper.getUploadFileByFileId(args[0]);
            if (uploadFile == null) {
                serverAPI.SendMessageToUser(User,"文件不存在");
                return true;
            }

            File file = new File("./uploadFiles",uploadFile.getOwnFile());
            if (!file.delete()) {
                serverAPI.SendMessageToUser(User,"出现错误，访问被拒绝或不存在");
                return true;
            }

            if (!mapper.deleteFile(uploadFile)) {
                serverAPI.SendMessageToUser(User,"出现错误，访问被拒绝或不存在");
                return true;
            }
            serverAPI.SendMessageToUser(User, "操作成功完成。");
            return true;
        }

        @Override
        public String getDescription() {
            return "根据文件Id删除上传的文件";
        }

        @Override
        public String getUsage() {
            return "/deleteFileByFileId <文件Id>";
        }

        @Override
        public boolean isAllowBroadcastCommandRunning() {
            return true;
        }
    }
}
