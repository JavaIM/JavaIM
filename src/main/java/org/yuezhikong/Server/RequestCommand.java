package org.yuezhikong.Server;

import org.apache.logging.log4j.Level;
import org.yuezhikong.config;
import org.yuezhikong.utils.DataBase.MySQL;
import org.yuezhikong.utils.Logger;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.Socket;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

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
     * 发出信息
     * @param Message 消息
     */
    public void info(String Message)
    {
        if (type == 0)
        {
            org.yuezhikong.utils.Logger logger = new org.yuezhikong.utils.Logger();
            logger.info(Message);
        }
        else
        {
            try {
                Thread.sleep(25);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            org.yuezhikong.Server.utils.utils.SendMessageToUser(User,Message);
        }
    }
    /**
     * 发出信息
     * @param Message 消息
     */
    public void error(String Message)
    {
        org.yuezhikong.utils.Logger logger = new org.yuezhikong.utils.Logger();
        logger.error(Message);
        if (type == 1)
        {
            org.yuezhikong.Server.utils.utils.SendMessageToUser(User,Message);
        }
    }

    /**
     * 发出信息
     * @param level 日志等级
     * @param Message 消息
     */
    public void log(Level level, String Message)
    {
        org.yuezhikong.utils.Logger logger = new org.yuezhikong.utils.Logger();
        logger.log(level,Message);
        if (type == 1)
        {
            org.yuezhikong.Server.utils.utils.SendMessageToUser(User,Message);
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
        if (UserClass == null)
        {
            CommandLogger logger = new CommandLogger(0,null);
            CommandRequestPrivate(command,argv,logger,true);
            return;
        }
        CommandLogger logger = new CommandLogger(1,UserClass);
        CommandRequestPrivate(command,argv,logger,false);
    }

    /**
     * CommandRequest的内部调用
     * @param command 命令
     * @param argv 参数
     * @param logger 发送信息给发信者用的logger
     * @param ISServer 是否为服务端调用，如果是，就为true，如果否，就为false
     */
    private static void CommandRequestPrivate(String command,String[] argv, CommandLogger logger,boolean ISServer)
    {
        switch (command) {
            case "/help" -> {
                logger.info("命令格式为：");
                logger.info("/kick ip 端口");
                logger.info("/say 信息");
                logger.info("/help 查看帮助");
                logger.info("/quit 退出程序");
                logger.info("/SetPermission <权限等级> <用户名> 设置权限等级");
                logger.info("目前只有三个权限等级，为：0和1，0为普通用户，1为管理员，-1为封禁，如为其他，自动认为为普通用户");
                logger.info("多余的空格将会被忽略");
            }
            case "/quit" -> System.exit(0);
            case "/say" -> {
                StringBuilder TheServerWillSay = new StringBuilder();//服务端将要发出的信息
                {
                    int i = 0;
                    if (0 != argv.length)
                    {
                        while (i < argv.length) {
                            if (i == argv.length - 1) {
                                TheServerWillSay.append(argv[i]);
                                break;
                            }
                            TheServerWillSay.append(argv[i]).append(" ");
                            i++;
                        }
                    }
                    else {
                        logger.info("您所输入的命令不正确");
                        logger.info("此命令的语法为say 信息");
                    }
                }
                // 发送信息
                org.yuezhikong.Server.utils.utils.SendMessageToAllClient("[Server] "+TheServerWillSay,newServer.GetInstance());
                logger.info("[Server] "+TheServerWillSay);
            }
            case "/SetPermission" -> {
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
                    int i = 0;
                    int tmpclientidall = newServer.GetInstance().getClientIDAll();
                    tmpclientidall = tmpclientidall - 1;
                    boolean found = false;
                    while (true) {
                        if (i > tmpclientidall) {
                            logger.info("错误，找不到此用户");
                            break;
                        }
                        user RequestUser = newServer.GetInstance().getUsers().get(i);
                        if (RequestUser.GetUserSocket() == null) {
                            i = i + 1;
                            continue;
                        }
                        if (RequestUser.GetUserName().equals(argv[1])) {
                            RequestUser.SetUserPermission(PermissionLevel);
                            found = true;
                        }
                        if (i == tmpclientidall) {
                            if (!found)
                            {
                                logger.info("错误，找不到此用户");
                            }
                            break;
                        }
                        i = i + 1;
                    }
                    Runnable SQLUpdateThread = () -> {
                        try {
                            Connection mySQLConnection = MySQL.GetMySQLConnection(config.GetMySQLDataBaseHost(), config.GetMySQLDataBasePort(), config.GetMySQLDataBaseName(), config.GetMySQLDataBaseUser(), config.GetMySQLDataBasePasswd());
                            String sql = "UPDATE UserData SET Permission=? where UserName = ?";
                            PreparedStatement ps = mySQLConnection.prepareStatement(sql);
                            ps.setInt(1,PermissionLevel);
                            ps.setString(2,argv[1]);
                            ps.executeUpdate();
                            mySQLConnection.close();
                        } catch (ClassNotFoundException | SQLException e) {
                            e.printStackTrace();
                        }
                    };
                    Thread UpdateThread = new Thread(SQLUpdateThread);
                    UpdateThread.start();
                    UpdateThread.setName("SQL Update Thread");
                    try {
                        UpdateThread.join();
                    } catch (InterruptedException e) {
                        logger.error("发生异常InterruptedException");
                        return;
                    }
                    if (found) {
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
                        logger.info("如果您是在更改一个离线的用户，您可去查询您的数据库，应当已经成功");
                    }
                }
                else
                {
                    logger.info("此命令的语法为：/SetPermission <权限等级> <用户名>");
                }
            }
            case "/debug" -> {
                if (argv.length >= 1)
                {
                    if (argv[0].equals("on"))
                    {
                        config.Debug_Mode = true;
                    }
                    else if (argv[0].equals("off"))
                    {
                        config.Debug_Mode = false;
                    }
                    else
                    {
                        logger.info("命令语法不正确，正确的语法为debug <on/off>");
                    }
                }
                else
                {
                    logger.info("此命令的语法为：/debug <on/off>");
                }
            }
            case "/kick" -> {
                if (argv.length >= 2) {
                    String IpAddress = argv[0];
                    int Port;
                    try {
                        Port = Integer.parseInt(argv[1]);
                    }
                    catch (NumberFormatException e)
                    {
                        if (config.GetDebugMode())
                        {
                            StringWriter sw = new StringWriter();
                            PrintWriter pw = new PrintWriter(sw);
                            e.printStackTrace(pw);
                            pw.flush();
                            sw.flush();
                            Logger.logger_root.debug(sw.toString());
                            pw.close();
                            try {
                                sw.close();
                            }
                            catch (IOException ex)
                            {
                                e.printStackTrace();
                            }
                        }
                        logger.info("输入的命令语法不正确，请检查后再输入");
                        return;
                    }
                    {
                        int i = 0;
                        int tmpclientidall = newServer.GetInstance().getClientIDAll();
                        tmpclientidall = tmpclientidall - 1;
                        try {
                            while (true) {
                                if (i > tmpclientidall) {
                                    logger.info("错误，找不到此用户");
                                    break;
                                }
                                Socket sendsocket = newServer.GetInstance().getUsers().get(i).GetUserSocket();
                                if (sendsocket == null) {
                                    i = i + 1;
                                    continue;
                                }
                                if (sendsocket.getInetAddress().toString().equals("/"+IpAddress))
                                {
                                    if (sendsocket.getPort() == Port)
                                    {
                                        logger.info("成功，已关闭IP为:"+sendsocket.getInetAddress()+"端口为："+sendsocket.getPort()+"的连接！");
                                        sendsocket.close();
                                        break;
                                    }
                                }
                                if (i == tmpclientidall) {
                                    logger.info("错误，找不到此用户");
                                    break;
                                }
                                i = i + 1;
                            }
                        } catch (IOException e) {
                            logger.log(Level.ERROR, "遍历用户时出现IOException!");
                            StringWriter sw = new StringWriter();
                            PrintWriter pw = new PrintWriter(sw);
                            e.printStackTrace(pw);
                            pw.flush();
                            sw.flush();
                            Logger.logger_root.debug(sw.toString());
                            pw.close();
                            try {
                                sw.close();
                            }
                            catch (IOException ex)
                            {
                                e.printStackTrace();
                            }
                        }
                    }
                }
                else
                {
                    logger.info("此命令的语法为：/kick ip 端口");
                }
            }
            default ->{
                if (ISServer) {
                    logger.info("无效的命令！");
                }
            }
        }
    }
}