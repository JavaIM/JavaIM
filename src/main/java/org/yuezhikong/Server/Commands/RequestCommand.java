package org.yuezhikong.Server.Commands;

import org.apache.logging.log4j.LogManager;
import org.yuezhikong.CodeDynamicConfig;
import org.yuezhikong.Server.Server;
import org.yuezhikong.Server.UserData.user;
import org.yuezhikong.Server.api.ServerAPI;
import org.yuezhikong.Server.plugin.PluginManager;
import org.yuezhikong.utils.CustomExceptions.ModeDisabledException;
import org.yuezhikong.utils.CustomVar;
import org.yuezhikong.utils.DataBase.Database;
import org.yuezhikong.utils.SaveStackTrace;

import javax.security.auth.login.AccountNotFoundException;
import java.io.IOException;
import java.lang.reflect.Field;
import java.net.ServerSocket;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Date;
import java.util.List;

import static org.yuezhikong.CodeDynamicConfig.About_System;

class CommandLogger
{

    //客户端 or 服务端，0为服务端，1为客户端
    private final int type;
    //如果是客户端，那么他的class是什么？
    private final user User;

    /**
     * “Logger”的构造函数
     * @param Type 类型：0：服务端，1：服务端
     * @param user 如果是客户端，请在这里加上客户端Class，如果是服务端，请改为null
     */
    public CommandLogger(int Type,user user)
    {
        type = Type;
        User = user;
    }
    /**
     * 发出聊天信息
     * @param Message 消息
     */
    public void ChatMsg(String Message)
    {
        if (type == 0)
        {
            org.yuezhikong.utils.Logger logger = Server.GetInstance().logger;
            logger.ChatMsg(Message);
        }
        else
        {
            ServerAPI.SendMessageToUser(User,Message);
        }
    }
    /**
     * 发出信息
     * @param Message 消息
     */
    public void info(String Message)
    {
        if (type == 0)
        {
            org.yuezhikong.utils.Logger logger = Server.GetInstance().logger;
            logger.info(Message);
        }
        else
        {
            ServerAPI.SendMessageToUser(User,Message);
        }
    }
    /**
     * 发出信息
     * @param Message 消息
     */
    public void error(String Message)
    {
        org.yuezhikong.utils.Logger logger = Server.GetInstance().logger;
        logger.error(Message);
        if (type == 1)
        {
            ServerAPI.SendMessageToUser(User,"[错误] "+Message);
        }
    }
}
public class RequestCommand {
    /**
     * 命令处理函数
     * @param command 命令
     * @param argv 参数
     * @param UserClass 操作者的Class
     * @apiNote 如果用户的Class为null，则视为为服务端请求！
     */
    public static void CommandRequest(String command,String[] argv, user UserClass)
    {
        //获取调用者，使用Throwable的getStackTrace而不是用Thread.getCurrentThread().getStackTrace是因为Thread.getCurrentThread会造成额外性能开销
        //又用try catch包装，因为直接new idea会警告未抛出
        try {
            throw new Throwable("getStackTrace");
        }
        catch (Throwable throwable)
        {
            StackTraceElement stackTraceElement = throwable.getStackTrace()[1];
            org.apache.logging.log4j.Logger DebugLogger = LogManager.getLogger("Debug");
            StringBuilder stringBuilder = new StringBuilder();
            //调用信息（第一条log)
            stringBuilder
                    .append("正在被调用命令：")
                    .append(command)
                    .append(" 参数为：");
            if (argv.length == 0)
            {
                stringBuilder.append("无参数");
            }
            else {
                for (String arg : argv) {
                    stringBuilder.append(arg).append(" ");
                }
                stringBuilder.deleteCharAt(stringBuilder.length() - 1);//删除多余的空格
            }
            DebugLogger.debug(stringBuilder.toString());
            //清空
            stringBuilder.setLength(0);
            //堆栈信息（第二条log）
            stringBuilder
                    .append("调用的来源与信息：")
                    .append("来源为：")
                    .append(stackTraceElement.getClassName())
                    .append(":")
                    .append(stackTraceElement.getMethodName())
                    .append(" (")
                    .append(stackTraceElement.getFileName())
                    .append(":")
                    .append(stackTraceElement.getLineNumber())
                    .append(")");
            DebugLogger.debug(stringBuilder.toString());
        }

        if (UserClass == null)
        {
            user User = new user(null,-1);
            User.setServer(true);
            User.UserLogin("Server");
            User.SetUserPermission(1,true);
            CommandLogger logger = new CommandLogger(0,null);
            CommandRequestPrivate(command,argv,logger,User);
            return;
        }
        CommandLogger logger = new CommandLogger(1,UserClass);
        CommandRequestPrivate(command,argv,logger,UserClass);
    }

    /**
     * CommandRequest的内部调用
     * @param command 命令
     * @param argv 参数
     * @param logger 发送信息给发信者用的logger
     * @param User User Data，如果是服务端调用时为一个被标记为服务端，用户名为Server且权限为管理员的虚拟用户，此用户的socket等为null，但调用SendMessageToUser是可以打印的（有单独处理，但如果这样打印到服务端，消息将被处理为“日志”）
     * @apiNote 这里的logger和CommandLogger Class为历史遗留问题，以前是仅服务端，直接调用logger.info输出，而后来兼容客户端时不得已而如此，有没有好心人把上面的自动适配给弄过来呢?
     */
    private static void CommandRequestPrivate(String command,String[] argv, CommandLogger logger,user User)
    {
        if (About_System)
        {
            if (command.equals("/about")) {
                logger.info("来自于服务器的帮助信息");
                logger.info("此服务端当前状态为：");
                logger.info("是否启用RSA加密功能：" + CodeDynamicConfig.GetRSA_Mode());
                logger.info("是否启用了SQLITE功能：" + CodeDynamicConfig.GetSQLITEMode());
                logger.info("服务端的database table版本：" + CodeDynamicConfig.GetDatabaseProtocolVersion());
                logger.info("服务端最大允许的会话数量，为-1代表禁用：" + CodeDynamicConfig.getMaxClient());
                logger.info("JavaIM是根据GNU General Public License v3.0开源的自由程序（开源软件）");
                logger.info("主仓库位于：https://github.com/QiLechan/JavaIM");
                logger.info("主要开发者名单：");
                logger.info("QiLechan（柒楽）");
                logger.info("AlexLiuDev233 （阿白）");
                logger.info("仓库启用了不允许协作者直接推送到主分支，需审核后再提交");
                logger.info("因此，如果想要体验最新功能，请查看fork仓库，但不保证稳定性");
                return;
            }
        }
        switch (command) {
            case "/help" -> {
                logger.info("命令格式为：");
                logger.info("/kick 用户名");
                logger.info("/say 信息");
                logger.info("/help 查看帮助");
                logger.info("/quit 退出程序");
                logger.info("/SetPermission <权限等级> <用户名> 设置权限等级");
                logger.info("/mute <用户名> <时长> [时长单位] 设置用户禁言");
                logger.info("时长单位如果不填，默认为毫秒，可填写s（秒），m(分)，h(小时),d(天)");
                logger.info("/unmute <用户名> 解除用户禁言");
                if (About_System) {
                    logger.info("/about 查看服务端程序相关信息");
                }
                //插件命令处理
                for (CustomVar.CommandInformation commandInformation : Server.GetInstance().PluginSetCommands)
                {
                    logger.info("/"+commandInformation.Command()+" 命令来源："+commandInformation.plugin().getInformation().PluginName()+" 来自"+commandInformation.plugin().getInformation().PluginName()+"的帮助信息："+commandInformation.Help());
                }
                logger.info("/list 查看服务器用户基本信息");
                logger.info("注：");
                logger.info("目前只有三个权限等级，为：0和1，0为普通用户，1为管理员，-1为封禁，如为其他，自动认为为普通用户");
                logger.info("多余的空格将会被忽略");
            }
            case "/list" -> {
                List<user> ValidClientList = ServerAPI.GetValidClientList(Server.GetInstance(),true);
                logger.info("服务器目前有"+ValidClientList.size()+"名用户：");
                for (user RequestUser : ValidClientList)
                {
                    logger.info("用户名为："+RequestUser.GetUserName()+"的用户的用户ID为："+RequestUser.GetUserClientID());
                }
            }
            case "/unmute" -> {
                if (User.GetUserPermission() != 1)
                {
                    logger.info("未知的命令，请输入/help查看帮助");
                    return;
                }
                if (argv.length >= 1)
                {
                    try {
                        user RequestUser = ServerAPI.GetUserByUserName(argv[0], Server.GetInstance(),true);
                        RequestUser.setMuteTime(0);
                        RequestUser.setMuted(false);
                        Runnable SQLUpdateThread = () -> {
                            try {
                                Connection DatabaseConnect = Database.Init(CodeDynamicConfig.GetMySQLDataBaseHost(), CodeDynamicConfig.GetMySQLDataBasePort(), CodeDynamicConfig.GetMySQLDataBaseName(), CodeDynamicConfig.GetMySQLDataBaseUser(), CodeDynamicConfig.GetMySQLDataBasePasswd());
                                String sql = "UPDATE UserData SET UserMuted = 0 and UserMuteTime = 0 where UserName = ?";
                                PreparedStatement ps = DatabaseConnect.prepareStatement(sql);
                                ps.setString(1,argv[0]);
                                ps.executeUpdate();
                                DatabaseConnect.close();
                            } catch (ClassNotFoundException | SQLException e) {
                                org.yuezhikong.utils.SaveStackTrace.saveStackTrace(e);
                            }
                        };
                        Thread UpdateThread = new Thread(SQLUpdateThread);
                        UpdateThread.start();
                        UpdateThread.setName("SQL Update Thread");
                        try {
                            UpdateThread.join();
                        } catch (InterruptedException e) {
                            logger.error("发生异常InterruptedException");
                            logger.error("目前解除禁言已动态完成，但未成功写入到数据库");
                            logger.error("在他下次登录时，他将再次被禁言");
                        }
                    } catch(AccountNotFoundException e)
                    {
                        logger.info("此用户不存在");
                    }
                }
            }
            case "/mute" -> {
                if (User.GetUserPermission() != 1)
                {
                    logger.info("未知的命令，请输入/help查看帮助");
                    return;
                }
                if (argv.length == 2)
                {
                    int UserMuteTime;
                    try
                    {
                        UserMuteTime = Integer.parseInt(argv[1]);
                    }
                    catch (NumberFormatException e)
                    {
                        logger.info("命令语法不正确，正确的语法为/mute <用户名> <时长> [时长单位]");
                        logger.info("详细帮助信息，请输入/help");
                        return;
                    }
                    try {
                        user RequestUser = ServerAPI.GetUserByUserName(argv[0],Server.GetInstance(),true);
                        RequestUser.setMuteTime(UserMuteTime);
                        RequestUser.setMuted(true);
                        Runnable SQLUpdateThread = () -> {
                            try {
                                Connection DatabaseConnect = Database.Init(CodeDynamicConfig.GetMySQLDataBaseHost(), CodeDynamicConfig.GetMySQLDataBasePort(), CodeDynamicConfig.GetMySQLDataBaseName(), CodeDynamicConfig.GetMySQLDataBaseUser(), CodeDynamicConfig.GetMySQLDataBasePasswd());
                                String sql = "UPDATE UserData SET UserMuted = 1 UserMuteTime = ? where UserName = ?";
                                PreparedStatement ps = DatabaseConnect.prepareStatement(sql);
                                ps.setInt(1, UserMuteTime);
                                ps.setString(2,argv[0]);
                                ps.executeUpdate();
                                DatabaseConnect.close();
                            } catch (ClassNotFoundException | SQLException e) {
                                org.yuezhikong.utils.SaveStackTrace.saveStackTrace(e);
                            }
                        };
                        Thread UpdateThread = new Thread(SQLUpdateThread);
                        UpdateThread.start();
                        UpdateThread.setName("SQL Update Thread");
                        try {
                            UpdateThread.join();
                        } catch (InterruptedException e) {
                            logger.error("发生异常InterruptedException");
                            logger.error("目前已动态完成禁言操作");
                            logger.error("但由于错误，未能成功写入到数据库");
                            logger.error("当此用户重新登录时，他将被解除禁言");
                        }
                    } catch (AccountNotFoundException e) {
                        logger.info("此用户不存在！");
                    }
                }
                else if (argv.length >= 3)
                {
                    long UserMuteTime;
                    try
                    {
                        UserMuteTime = Integer.parseInt(argv[1]);
                    }
                    catch (NumberFormatException e)
                    {
                        logger.info("命令语法不正确，正确的语法为/mute <用户名> <时长> [时长单位]");
                        logger.info("详细帮助信息，请输入/help");
                        return;
                    }
                    switch (argv[2]) {
                        case "s" -> UserMuteTime = UserMuteTime * 1000;//X1000换算为毫秒
                        case "m" -> UserMuteTime = UserMuteTime * 60 * 1000;//X60换算为秒，再X1000换算为毫秒
                        case "h" -> UserMuteTime = UserMuteTime * 60 * 60 * 1000;//X60换算为分钟，再X60换算为秒，最后X1000换算为毫秒
                        case "d" -> UserMuteTime = UserMuteTime * 24 * 60 * 60 * 1000;//X24换算为小时，然后X60换算为分钟，再X60换算为秒，最后X1000换算为毫秒
                    }
                    Date date = new Date();
                    long Time = date.getTime();//获取当前时间毫秒数
                    UserMuteTime = UserMuteTime + Time;
                    try {
                        user RequestUser = ServerAPI.GetUserByUserName(argv[0],Server.GetInstance(),true);
                        RequestUser.setMuteTime(UserMuteTime);
                        RequestUser.setMuted(true);
                        long finalUserMuteTime = UserMuteTime;
                        Runnable SQLUpdateThread = () -> {
                            try {
                                Connection DatabaseConnect = Database.Init(CodeDynamicConfig.GetMySQLDataBaseHost(), CodeDynamicConfig.GetMySQLDataBasePort(), CodeDynamicConfig.GetMySQLDataBaseName(), CodeDynamicConfig.GetMySQLDataBaseUser(), CodeDynamicConfig.GetMySQLDataBasePasswd());
                                String sql = "UPDATE UserData SET UserMuted = 1 UserMuteTime = ? where UserName = ?";
                                PreparedStatement ps = DatabaseConnect.prepareStatement(sql);
                                ps.setLong(1, finalUserMuteTime);
                                ps.setString(2,argv[0]);
                                ps.executeUpdate();
                                DatabaseConnect.close();
                            } catch (ClassNotFoundException | SQLException e) {
                                org.yuezhikong.utils.SaveStackTrace.saveStackTrace(e);
                            }
                        };
                        Thread UpdateThread = new Thread(SQLUpdateThread);
                        UpdateThread.start();
                        UpdateThread.setName("SQL Update Thread");
                        try {
                            UpdateThread.join();
                        } catch (InterruptedException e) {
                            logger.error("发生异常InterruptedException");
                            logger.error("目前已动态完成禁言操作");
                            logger.error("但由于错误，未能成功写入到数据库");
                            logger.error("当此用户重新登录时，他将被解除禁言");
                        }
                    } catch (AccountNotFoundException e) {
                        logger.info("此用户不存在！");
                    }
                }
                else
                {
                    logger.info("无效语法，正确的语法应该为：/mute <用户名> <时长> [时间单位]");
                    logger.info("详细语法请见/help");
                }
            }
            case "/quit" -> {
                if (User.GetUserPermission() != 1)
                {
                    logger.info("未知的命令，请输入/help查看帮助");
                    return;
                }
                try {
                    Field field = Server.class.getDeclaredField("ExitSystem");
                    field.setAccessible(true);
                    field.set(Server.GetInstance(),true);
                    field.setAccessible(false);

                    field = Server.class.getDeclaredField("userAuthThread");
                    field.setAccessible(true);
                    Thread userAuthThread = (Thread) field.get(Server.GetInstance());
                    field.setAccessible(false);

                    field = Server.class.getDeclaredField("serverSocket");
                    field.setAccessible(true);
                    ServerSocket ServerSocket = (java.net.ServerSocket) field.get(Server.GetInstance());
                    field.setAccessible(false);
                    ServerSocket.close();
                    userAuthThread.join();
                    Server.GetInstance().timer.cancel();
                    if (CodeDynamicConfig.GetPluginSystemMode()) {
                        PluginManager.getInstance("./plugins").OnProgramExit(0);
                    }
                    System.exit(0);
                } catch (ModeDisabledException  | IOException | NoSuchFieldException | InterruptedException | IllegalAccessException e) {
                    Server.GetInstance().timer.cancel();
                    System.exit(1);
                }
            }
            case "/say" -> {
                if (User.GetUserPermission() != 1)
                {
                    logger.info("未知的命令，请输入/help查看帮助");
                    return;
                }
                if (argv.length > 0)
                {
                    StringBuilder stringBuilder = new StringBuilder();//服务端将要发出的信息
                    for (String arg : argv)
                    {
                        stringBuilder.append(arg).append(" ");
                    }
                    stringBuilder.deleteCharAt(stringBuilder.length() -1);
                    // 发送信息
                    ServerAPI.SendMessageToAllClient("[Server] "+stringBuilder, Server.GetInstance());
                    logger.ChatMsg("[Server] "+stringBuilder);
                }
                else {
                    logger.info("您所输入的命令不正确");
                    logger.info("此命令的语法为say 信息");
                }
            }
            case "/SetPermission" -> {
                if (User.GetUserPermission() != 1)
                {
                    logger.info("未知的命令，请输入/help查看帮助");
                    return;
                }
                if (argv.length >= 2)
                {
                    int PermissionLevel;
                    try
                    {
                        PermissionLevel = Integer.parseInt(argv[0]);
                    }
                    catch (NumberFormatException e)
                    {
                        logger.info("命令语法不正确，正确的语法为/SetPermission <权限等级> <用户名>");
                        return;
                    }
                    user RequestUser;
                    try {
                        RequestUser = ServerAPI.GetUserByUserName(argv[1],Server.GetInstance(),true);
                    } catch (AccountNotFoundException e) {
                        logger.info("此用户不存在！");
                        return;
                    }
                    RequestUser.SetUserPermission(PermissionLevel,false);
                    Runnable SQLUpdateThread = () -> {
                        try {
                            Connection DatabaseConnect = Database.Init(CodeDynamicConfig.GetMySQLDataBaseHost(), CodeDynamicConfig.GetMySQLDataBasePort(), CodeDynamicConfig.GetMySQLDataBaseName(), CodeDynamicConfig.GetMySQLDataBaseUser(), CodeDynamicConfig.GetMySQLDataBasePasswd());
                            String sql = "UPDATE UserData SET Permission=? where UserName = ?";
                            PreparedStatement ps = DatabaseConnect.prepareStatement(sql);
                            ps.setInt(1,PermissionLevel);
                            ps.setString(2,argv[1]);
                            ps.executeUpdate();
                            DatabaseConnect.close();
                        } catch (ClassNotFoundException | SQLException e) {
                            SaveStackTrace.saveStackTrace(e);
                        }
                    };
                    Thread UpdateThread = new Thread(SQLUpdateThread);
                    UpdateThread.start();
                    UpdateThread.setName("SQL Update Thread");
                    try {
                        UpdateThread.join();
                    } catch (InterruptedException e) {
                        logger.error("发生异常InterruptedException");
                        logger.error("目前成功动态修改掉用户权限，但保存到数据库失败，在此用户重新登录后，他的权限将消失");
                        return;
                    }
                    if (PermissionLevel == 1) {
                        logger.info("已将" + argv[1] + "的权限更改为 管理员");
                    } else if (PermissionLevel == 0) {
                        logger.info("已将" + argv[1] + "的权限更改为 标准用户");
                    } else if (PermissionLevel == -1) {
                        logger.info("已将" + argv[1] + "的权限更改为 已封禁用户");
                    } else {
                        logger.info("已将" + argv[1] + "的权限更改为 未使用权限组");
                    }
                }
                else
                {
                    logger.info("此命令的语法为：/SetPermission <权限等级> <用户名>");
                }
            }
            case "/kick" -> {
                if (User.GetUserPermission() != 1)
                {
                    logger.info("未知的命令，请输入/help查看帮助");
                    return;
                }
                if (argv.length >= 1) {
                    try {
                        ServerAPI.GetUserByUserName(argv[0], Server.GetInstance(),false).UserDisconnect();
                    } catch (AccountNotFoundException e) {
                        logger.info("此用户不存在！");
                    }
                }
                else
                {
                    logger.info("此命令的语法为：/kick 用户名");
                }
            }
            default -> {
                for (CustomVar.CommandInformation commandInformation : Server.GetInstance().PluginSetCommands)
                {
                    if (("/"+commandInformation.Command()).equals(command))
                    {
                        commandInformation.plugin().OnCommand(command,argv,User);
                        return;
                    }
                }
                logger.info("未知的命令，请输入/help查看帮助");
            }
        }
    }
}
