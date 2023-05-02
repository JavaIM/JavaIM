package org.yuezhikong.Server;


import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.yuezhikong.CodeDynamicConfig;
import org.yuezhikong.Server.UserData.RecvMessageThread;
import org.yuezhikong.Server.UserData.user;
import org.yuezhikong.Server.api.ServerAPI;
import org.yuezhikong.Server.plugin.load.PluginManager;
import org.yuezhikong.utils.CustomExceptions.ModeDisabledException;
import org.yuezhikong.utils.CustomVar;
import org.yuezhikong.utils.DataBase.Database;
import org.yuezhikong.utils.Logger;
import org.yuezhikong.utils.RSA;
import org.yuezhikong.utils.SaveStackTrace;

import java.io.File;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import static org.yuezhikong.Server.Commands.RequestCommand.CommandRequest;

public class Server {
    private Thread userAuthThread;
    private boolean ExitSystem = false;
    public static final org.apache.logging.log4j.Logger DEBUG = LogManager.getLogger("Debug");
    public Logger logger;
    private final List<user> Users = new ArrayList<>();
    //private final List<Socket> sockets = new ArrayList<>();
    private int clientIDAll = 0;
    private ServerSocket serverSocket = null;
    //服务端实例
    private static Server instance = null;
    public org.yuezhikong.Server.timer timer;

    /**
     * 获取客户端总数量
     * @return 客户端总数量
     */
    public int getClientIDAll() {
        return clientIDAll;
    }

    /**
     * 获取用户Class List
     * @return 用户Class List
     */
    public List<user> getUsers() {
        return Users;
    }

    /**
     * @apiNote 自动创建RSA key而不替换已存在的key
     */
    protected void RSA_KeyAutogenerate()
    {
        if (!(new File("Public.key").exists()))
        {
            if (!(new File("Private.key").exists()))
            {
                try {
                    RSA.generateKeyToFile("Public.key", "Private.key");
                }
                catch (Exception e)
                {
                    SaveStackTrace.saveStackTrace(e);
                }
            }
            else
            {
                logger.warning("系统检测到您的目录下不存在公钥，但，存在私钥，系统将为您覆盖一个新的rsa key");
                try {
                    RSA.generateKeyToFile("Public.key", "Private.key");
                }
                catch (Exception e)
                {
                    SaveStackTrace.saveStackTrace(e);
                }
            }
        }
        else
        {
            if (!(new File("Private.key").exists()))
            {
                logger.warning("系统检测到您的目录下存在公钥，但，不存在私钥，系统将为您覆盖一个新的rsa key");
                try {
                    RSA.generateKeyToFile("Public.key", "Private.key");
                }
                catch (Exception e)
                {
                    SaveStackTrace.saveStackTrace(e);
                }
            }
        }
    }

    /**
     * @apiNote 获取服务端实例
     * @return 服务端实例
     */
    public static Server GetInstance()
    {
        return instance;
    }

    /**
     * @apiNote 启动recvMessage线程
     * @param ClientID 客户端ID
     */
    private void StartRecvMessageThread(int ClientID)
    {
        Thread recvmessage = new RecvMessageThread(ClientID,Users);
        recvmessage.start();
        recvmessage.setName("recvMessage Thread");
    }


    /**
     * @apiNote 启动用户登录的线程
     */
    protected void StartUserAuthThread()
    {
        Runnable UserAuthThread = () -> {
            while (true)
            {
                try {
                    assert serverSocket != null;
                    if (ExitSystem)
                    {
                        serverSocket.close();
                        break;
                    }
                    Socket clientSocket = serverSocket.accept();//接受客户端Socket请求
                    if (ExitSystem)
                    {
                        serverSocket.close();
                        clientSocket.close();
                        break;
                    }
                    if (CodeDynamicConfig.getMaxClient() >= 0)//检查是否已到最大
                    {
                        //说下这里的逻辑
                        //客户端ID 客户端数量
                        //0 1
                        //1 2
                        //2 3
                        //假如限制为3
                        //那么就需要检测接下来要写入的ID是不是2或者大于2，如果是，那就是超过了
                        if (clientIDAll >= CodeDynamicConfig.getMaxClient() -1)
                        {
                            clientSocket.close();
                            continue;
                        }
                    }
                    user CureentUser = new user(clientSocket,clientIDAll);//创建用户class
                    Users.add(CureentUser);//保险措施
                    Users.set(clientIDAll,CureentUser);//添加到List中
                    StartRecvMessageThread(clientIDAll);//启动RecvMessage线程
                    clientIDAll++;//当前的最大ClientID加1
                }
                catch (IOException e)
                {
                    if (e.getMessage().equals("Socket closed"))
                    {
                        break;
                    }
                    logger.log(Level.ERROR,"在accept时发生IOException");
                    SaveStackTrace.saveStackTrace(e);
                }
            }
        };
        userAuthThread = new Thread(UserAuthThread);
        userAuthThread.start();
        userAuthThread.setName("UserAuthThread");
    }
    /**
     * 启动timer
     */
    protected void StartTimer()
    {
        timer = new timer();
        timer.Start();
    }
    /**
     * @apiNote 启动命令系统
     */
    protected void StartCommandSystem()
    {
        logger.info("服务端启动完成");
        Scanner sc = new Scanner(System.in);
        while (true)
        {
            CustomVar.Command CommandRequestReturn = ServerAPI.CommandFormat(sc.nextLine());
            String command = CommandRequestReturn.Command();
            logger.info("控制台 执行了命令 /"+command);

            if (command.equals("quit"))
            {
                ExitSystem = true;
                try {
                    serverSocket.close();
                    userAuthThread.join();
                } catch (IOException | InterruptedException e) {
                    SaveStackTrace.saveStackTrace(e);
                    timer.cancel();
                    if (CodeDynamicConfig.GetPluginSystemMode()) {
                        try {
                            PluginManager.getInstance("./plugins").OnProgramExit(1);
                        } catch (ModeDisabledException ex) {
                            System.exit(1);
                        }
                    }
                }
                timer.cancel();
                if (CodeDynamicConfig.GetPluginSystemMode()) {
                    try {
                        PluginManager.getInstance("./plugins").OnProgramExit(0);
                    } catch (ModeDisabledException e) {
                        System.exit(0);
                    }
                }
            }
            CommandRequest("/"+command,CommandRequestReturn.argv(),null);
        }
    }

    /**
     * 重置数据库中的用户登录状态，因为服务器重启后，所有用户都会被踢出
     */
    protected void UserLoginStatusReset() {
        Runnable SQLUpdateThread = () -> {
            try {
                Connection mySQLConnection = Database.Init(CodeDynamicConfig.GetMySQLDataBaseHost(), CodeDynamicConfig.GetMySQLDataBasePort(), CodeDynamicConfig.GetMySQLDataBaseName(), CodeDynamicConfig.GetMySQLDataBaseUser(), CodeDynamicConfig.GetMySQLDataBasePasswd());
                String sql = "UPDATE UserData SET UserLogged = 0 where UserLogged = 1;";
                PreparedStatement ps = mySQLConnection.prepareStatement(sql);
                ps.executeUpdate();
            } catch (Exception e) {
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
            SaveStackTrace.saveStackTrace(e);
        }
    }

    /**
     * 严禁集成到构造函数！
     * 此函数是由GUIServer重写的，用来替换logger，这样可以将信息传输到GUI
     */
    protected void SetupLoggerSystem()
    {
        logger = new Logger(false,false,null,null);
    }
    /**
     * @apiNote 服务端main函数
     * @param port 要开始监听的端口
     */
    public Server(int port) {
        instance = this;
        SetupLoggerSystem();
        RSA_KeyAutogenerate();
        try {
            serverSocket = new ServerSocket(port);
        } catch (IOException e) {
            SaveStackTrace.saveStackTrace(e);
        }
        UserLoginStatusReset();
        StartUserAuthThread();
        StartTimer();
        //这里"getInstance"其实并不是真的为了获取实例，而是为了创建新实例
        try {
            PluginManager.getInstance("./plugins");
        } catch (ModeDisabledException e) {
            e.printStackTrace();
        }
        StartCommandSystem();
    }
}
