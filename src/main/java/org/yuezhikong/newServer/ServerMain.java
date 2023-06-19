package org.yuezhikong.newServer;

import cn.hutool.crypto.SecureUtil;
import cn.hutool.crypto.symmetric.SymmetricAlgorithm;
import com.google.gson.Gson;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.yuezhikong.CodeDynamicConfig;
import org.yuezhikong.GeneralMethod;
import org.yuezhikong.newServer.UserData.user;
import org.yuezhikong.utils.DataBase.Database;
import org.yuezhikong.utils.Logger;
import org.yuezhikong.utils.Protocol.LoginProtocol;
import org.yuezhikong.utils.Protocol.NormalProtocol;
import org.yuezhikong.utils.RSA;
import org.yuezhikong.utils.SaveStackTrace;

import javax.crypto.SecretKey;
import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * 新服务端
 * @author AlexLiuDev233
 * @Date 2023/6/10
 */
public class ServerMain extends GeneralMethod {
    private final List<user> Users = new ArrayList<>();
    private int clientIDAll = 0;
    private static class UserAuthThread extends Thread
    {
        private final Logger logger;
        private final ServerSocket socket;
        public UserAuthThread()
        {
            this.logger = getServer().logger;
            this.socket = getServer().socket;
        }
        @Override
        public void run() {
            this.setName("UserAuthThread");
            this.setUncaughtExceptionHandler((t, e) -> {
                SaveStackTrace.saveStackTrace(e);
                logger.error("由于出现未捕获的异常，程序出现故障，即将退出");
                getServer().ExitSystem(1);
            });
            while (true)
            {
                Socket clientSocket;//接受客户端Socket请求
                try {
                    clientSocket = socket.accept();
                } catch (IOException e) {
                    SaveStackTrace.saveStackTrace(e);
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
                    if (getServer().clientIDAll >= CodeDynamicConfig.getMaxClient() -1)
                    {
                        try {
                            clientSocket.close();
                        } catch (IOException e) {
                            continue;
                        }
                        continue;
                    }
                }
                user CurrentUser = new user(clientSocket,getServer().clientIDAll);//创建用户class
                getServer().Users.add(CurrentUser);
                getServer().Users.set(getServer().clientIDAll,CurrentUser);//添加到List中
                getServer().StartRecvMessageThread(getServer().clientIDAll);//启动RecvMessage线程
                getServer().clientIDAll++;//当前的最大ClientID加1
                logger.info("连入了新的socket请求");
                logger.info("基本信息如下：");
                logger.info("远程ip地址为："+clientSocket.getRemoteSocketAddress());
                logger.info("远程端口为："+clientSocket.getPort());
            }
            getServer().ExitSystem(2);
        }
    }

    public List<user> getUsers() {
        return Users;
    }

    public static class RecvMessageThread extends Thread
    {
        private final user CurrentUser;
        public RecvMessageThread(int ClientID)
        {
            CurrentUser = getServer().Users.get(ClientID);
        }
        private boolean CheckPassword(String UserName,String Passwd,user RequestUser)
        {
            try {
                class IOWorker extends Thread {
                    private boolean Success = false;
                    @Override
                    public void run() {
                        this.setUncaughtExceptionHandler((t, e) -> SaveStackTrace.saveStackTrace(e));
                        this.setName("SQL Request Thread");
                        if ("Server".equals(UserName))
                        {
                            Success = false;
                            api.SendMessageToUser(RequestUser,"不得使用被禁止的用户名：Server");
                            return;
                        }
                        try {
                            Connection DatabaseConnection = Database.Init(CodeDynamicConfig.GetMySQLDataBaseHost(), CodeDynamicConfig.GetMySQLDataBasePort(), CodeDynamicConfig.GetMySQLDataBaseName(), CodeDynamicConfig.GetMySQLDataBaseUser(), CodeDynamicConfig.GetMySQLDataBasePasswd());
                            String sql = "select * from UserData where UserName = ?";
                            PreparedStatement ps = DatabaseConnection.prepareStatement(sql);
                            ps.setString(1,UserName);
                            ResultSet rs = ps.executeQuery();
                            if (rs.next())
                            {
                                String salt;
                                String sha256;
                                if (rs.getInt("UserLogged") == 1)
                                {
                                    Success = false;
                                    api.SendMessageToUser(RequestUser,"此用户已经登录了!");
                                    DatabaseConnection.close();
                                    return;
                                }
                                salt = rs.getString("salt");
                                sha256 = SecureUtil.sha256(Passwd + salt);
                                if (rs.getString("Passwd").equals(sha256))
                                {
                                    int PermissionLevel = rs.getInt("Permission");
                                    if (PermissionLevel != 0)
                                    {
                                        if (PermissionLevel != 1)
                                        {
                                            if (PermissionLevel != -1)
                                            {
                                                PermissionLevel = 0;
                                            }
                                            else
                                            {
                                                api.SendMessageToUser(RequestUser,"您的账户已被永久封禁！");
                                                Success = false;
                                                DatabaseConnection.close();
                                                return;
                                            }
                                        }
                                    }
                                    long muted = rs.getLong("UserMuted");
                                    long MuteTime = rs.getLong("UserMuteTime");
                                    if (muted == 1)
                                    {
                                        RequestUser.setMuteTime(MuteTime);
                                        RequestUser.setMuted(true);
                                    }
                                    Success = true;
                                    RequestUser.SetUserPermission(PermissionLevel,true);
                                    RequestUser.UserLogin(UserName);
                                    sql = "UPDATE UserData SET UserLogged = 1 where UserName = ?;";
                                    ps = DatabaseConnection.prepareStatement(sql);
                                    ps.setString(1,UserName);
                                    ps.executeUpdate();
                                }
                            }
                            else
                            {
                                String salt;
                                do {
                                    salt = UUID.randomUUID().toString();
                                    sql = "select * from UserData where salt = ?";
                                    ps = DatabaseConnection.prepareStatement(sql);
                                    ps.setString(1, sql);
                                    rs = ps.executeQuery();
                                } while (rs.next());
                                String sha256 = SecureUtil.sha256(Passwd + salt);
                                sql = "INSERT INTO `UserData` (`Permission`,`UserName`, `Passwd`,`salt`) VALUES (0,?, ?, ?);";
                                ps = DatabaseConnection.prepareStatement(sql);
                                ps.setString(1,UserName);
                                ps.setString(2,sha256);
                                ps.setString(3,salt);
                                ps.executeUpdate();
                                Success = true;
                                RequestUser.UserLogin(UserName);
                            }
                            String token;
                            do {
                                token = UUID.randomUUID().toString();
                                sql = "select * from UserData where token = ?";
                                ps = DatabaseConnection.prepareStatement(sql);
                                ps.setString(1, sql);
                                rs = ps.executeQuery();
                            } while (rs.next());
                            sql = "UPDATE UserData SET token = ? where UserName = ?;";
                            ps = DatabaseConnection.prepareStatement(sql);
                            ps.setString(1,token);
                            ps.setString(2,UserName);
                            ps.executeUpdate();
                            final String finalToken = token;
                            new Thread()
                            {
                                @Override
                                public void run() {
                                    this.setName("I/O Thread");
                                    try {
                                        BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(RequestUser.getUserSocket().getOutputStream(),StandardCharsets.UTF_8));
                                        Gson gson = new Gson();
                                        NormalProtocol protocolData = new NormalProtocol();
                                        NormalProtocol.MessageHead MessageHead = new NormalProtocol.MessageHead();
                                        MessageHead.setType("Login");
                                        MessageHead.setVersion(CodeDynamicConfig.getProtocolVersion());
                                        protocolData.setMessageHead(MessageHead);
                                        NormalProtocol.MessageBody MessageBody = new NormalProtocol.MessageBody();
                                        MessageBody.setFileLong(0);
                                        MessageBody.setMessage(finalToken);
                                        protocolData.setMessageBody(MessageBody);
                                        String data = gson.toJson(protocolData);
                                        data = RequestUser.getUserAES().encryptBase64(data);
                                        writer.write(data);
                                        writer.newLine();
                                        writer.flush();
                                    } catch (IOException e) {
                                        SaveStackTrace.saveStackTrace(e);
                                    }
                                }
                                public Thread start2()
                                {
                                    super.start();
                                    return this;
                                }
                            }.start2().join();
                        } catch (ClassNotFoundException e)
                        {
                            RequestUser.UserDisconnect();
                            SaveStackTrace.saveStackTrace(e);
                            org.apache.logging.log4j.Logger DEBUG = LogManager.getLogger("Debug");
                            DEBUG.fatal("ClassNotFoundException，无法找到MySQL驱动");
                            DEBUG.fatal("程序已崩溃");
                            System.exit(-2);
                            Success = false;
                        }
                        catch (SQLException e)
                        {
                            RequestUser.UserDisconnect();
                            SaveStackTrace.saveStackTrace(e);
                            Success = false;
                        } catch (InterruptedException e) {
                            SaveStackTrace.saveStackTrace(e);
                        }
                    }
                    public IOWorker start2() {
                        super.start();
                        return this;
                    }

                    public boolean isSuccess() {
                        return Success;
                    }
                    public IOWorker join2() throws InterruptedException {
                        join();
                        return this;
                    }
                }
                return new IOWorker()
                        .start2()
                        .join2()
                        .isSuccess();
            } catch (InterruptedException e) {
                SaveStackTrace.saveStackTrace(e);
            }
            return false;
        }
        public void LoginSystem(@NotNull user User) throws IOException
        {
            String json;
            LoginProtocol protocol;
            final BufferedReader reader = new BufferedReader(new InputStreamReader(User.getUserSocket().getInputStream(),StandardCharsets.UTF_8));
            final BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(User.getUserSocket().getOutputStream(),StandardCharsets.UTF_8));
            do {
                json = reader.readLine();
            } while (json == null);
            json = unicodeToString(json);
            Gson gson = new Gson();
            json = User.getUserAES().decryptStr(json);
            protocol = gson.fromJson(json,LoginProtocol.class);
            if ("Token".equals(protocol.getLoginPacketHead().getType()))
            {
                final List<String> username = new ArrayList<>();
                try {
                    LoginProtocol finalProtocol = protocol;
                    new Thread() {
                        @Override
                        public void run() {
                            this.setName("SQL Process Thread");
                            try {
                                Connection DatabaseConnection = Database.Init(CodeDynamicConfig.GetMySQLDataBaseHost(), CodeDynamicConfig.GetMySQLDataBasePort(), CodeDynamicConfig.GetMySQLDataBaseName(), CodeDynamicConfig.GetMySQLDataBaseUser(), CodeDynamicConfig.GetMySQLDataBasePasswd());
                                String sql = "select * from UserData where token = ?";
                                PreparedStatement ps = DatabaseConnection.prepareStatement(sql);
                                ps.setString(1, finalProtocol.getLoginPacketBody().getReLogin().getToken());
                                ResultSet rs = ps.executeQuery();
                                if (rs.next()) {
                                    username.add(rs.getString("UserName"));
                                }
                                DatabaseConnection.close();
                            } catch (SQLException | ClassNotFoundException e) {
                                SaveStackTrace.saveStackTrace(e);
                            }
                        }
                        public Thread start2() {
                            super.start();
                            return this;
                        }
                    }.start2().join();
                } catch (InterruptedException ignored) {}
                NormalProtocol normalProtocol = new NormalProtocol();
                NormalProtocol.MessageHead head = new NormalProtocol.MessageHead();
                NormalProtocol.MessageBody body = new NormalProtocol.MessageBody();
                if (username.isEmpty())
                {
                    head.setVersion(CodeDynamicConfig.getProtocolVersion());
                    head.setType("Login");
                    normalProtocol.setMessageHead(head);
                    body.setMessage("Fail");
                    normalProtocol.setMessageBody(body);
                    writer.write(CurrentUser.getUserAES().encryptBase64(gson.toJson(normalProtocol)));
                    writer.newLine();
                    writer.flush();
                    do {
                        json = reader.readLine();
                    } while (json == null);
                    json = unicodeToString(json);
                    json = User.getUserAES().decryptStr(json);
                    protocol = gson.fromJson(json,LoginProtocol.class);
                    if (!("passwd".equals(protocol.getLoginPacketHead().getType())))
                    {
                        System.gc();
                        throw new RuntimeException("Login Mode wrong");
                    }
                    if (CheckPassword(protocol.getLoginPacketBody().getNormalLogin().getUserName(), protocol.getLoginPacketBody().getNormalLogin().getPasswd(),User))
                    {
                        User.UserLogin(protocol.getLoginPacketBody().getNormalLogin().getUserName());
                    }
                    else
                    {
                        User.UserDisconnect();
                    }
                }
                else
                {
                    head.setType("Login");
                    head.setVersion(CodeDynamicConfig.getProtocolVersion());
                    normalProtocol.setMessageHead(head);
                    body.setMessage("Success");
                    normalProtocol.setMessageBody(body);
                    json = gson.toJson(normalProtocol);
                    json = User.getUserAES().encryptBase64(json);
                    writer.write(json);
                    writer.newLine();
                    writer.flush();
                }
            }
            else if ("passwd".equals(protocol.getLoginPacketHead().getType()))
            {
                if (CheckPassword(protocol.getLoginPacketBody().getNormalLogin().getUserName(), protocol.getLoginPacketBody().getNormalLogin().getPasswd(),User))
                {
                    User.UserLogin(protocol.getLoginPacketBody().getNormalLogin().getUserName());
                }
                else
                {
                    User.UserDisconnect();
                }
            }
            else
            {
                System.gc();
                throw new RuntimeException("Login Mode wrong");
            }
            System.gc();
        }
        @Override
        public void run() {
            this.setName("RecvMessageThread");
            Logger logger = getServer().logger;
            CurrentUser.setRecvMessageThread(this);
            try (Socket CurrentUserSocket = CurrentUser.getUserSocket()) {
                //服务器密钥加载
                final String ServerPrivateKey = FileUtils.readFileToString(new File("Private.txt"),StandardCharsets.UTF_8);
                //开始握手
                BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(CurrentUserSocket.getOutputStream(),StandardCharsets.UTF_8));
                BufferedReader reader = new BufferedReader(new InputStreamReader(CurrentUserSocket.getInputStream(),StandardCharsets.UTF_8));
                //测试明文通讯
                writer.write("Hello Client");
                writer.newLine();
                writer.flush();
                logger.info("正在连接的客户端返回："+unicodeToString(reader.readLine()));
                writer.write("你好，客户端");
                writer.newLine();
                writer.flush();
                logger.info("正在连接的客户端返回："+unicodeToString(reader.readLine()));
                //测试通讯协议
                Gson gson = new Gson();
                NormalProtocol protocol = new NormalProtocol();
                NormalProtocol.MessageHead head = new NormalProtocol.MessageHead();
                head.setVersion(CodeDynamicConfig.getProtocolVersion());
                head.setType("Test");
                protocol.setMessageHead(head);
                NormalProtocol.MessageBody body = new NormalProtocol.MessageBody();
                body.setMessage("你好，客户端");
                body.setFileLong(0);
                protocol.setMessageBody(body);
                writer.write(gson.toJson(protocol));
                writer.newLine();
                writer.flush();
                String json;
                do {
                    json = reader.readLine();
                } while (json == null);
                json = unicodeToString(json);
                protocol = getServer().protocolRequest(json);
                if (protocol.getMessageHead().getVersion() != CodeDynamicConfig.getProtocolVersion() || !("Test".equals(protocol.getMessageHead().getType())))
                {
                    return;
                }
                logger.info("正在连接的客户端返回："+protocol.getMessageBody().getMessage());
                //RSA Key传递
                do {
                    json = reader.readLine();
                } while (json == null);
                json = unicodeToString(json);
                protocol = getServer().protocolRequest(json);
                if (protocol.getMessageHead().getVersion() != CodeDynamicConfig.getProtocolVersion() || !("Encryption".equals(protocol.getMessageHead().getType())))
                {
                    return;
                }
                CurrentUser.setPublicKey(RSA.decrypt(protocol.getMessageBody().getMessage(), ServerPrivateKey));
                //测试RSA
                protocol = new NormalProtocol();
                head = new NormalProtocol.MessageHead();
                head.setType("Test");
                head.setVersion(CodeDynamicConfig.getProtocolVersion());
                protocol.setMessageHead(head);
                body = new NormalProtocol.MessageBody();
                body.setMessage(RSA.encrypt("你好客户端",CurrentUser.getPublicKey()));
                protocol.setMessageBody(body);
                writer.write(gson.toJson(protocol));
                writer.newLine();
                writer.flush();
                do {
                    json = reader.readLine();
                } while (json == null);
                json = unicodeToString(json);
                protocol = getServer().protocolRequest(json);
                if (protocol.getMessageHead().getVersion() != CodeDynamicConfig.getProtocolVersion() || !("Test".equals(protocol.getMessageHead().getType())))
                {
                    return;
                }
                logger.info("正在连接的客户端返回："+RSA.decrypt(protocol.getMessageBody().getMessage(),ServerPrivateKey));
                //AES制造开始
                protocol = new NormalProtocol();
                head = new NormalProtocol.MessageHead();
                head.setType("Encryption");
                head.setVersion(CodeDynamicConfig.getProtocolVersion());
                protocol.setMessageHead(head);
                body = new NormalProtocol.MessageBody();
                String RandomForServer = UUID.randomUUID().toString();
                body.setMessage(RSA.encrypt(RandomForServer,CurrentUser.getPublicKey()));
                protocol.setMessageBody(body);
                writer.write(gson.toJson(protocol));
                writer.newLine();
                writer.flush();
                do {
                    json = reader.readLine();
                } while (json == null);
                json = unicodeToString(json);
                protocol = getServer().protocolRequest(json);
                if (protocol.getMessageHead().getVersion() != CodeDynamicConfig.getProtocolVersion() || !("Encryption".equals(protocol.getMessageHead().getType())))
                {
                    return;
                }
                SecretKey key = SecureUtil.generateKey(SymmetricAlgorithm.AES.getValue(), Base64.decodeBase64(getServer().GenerateKey(RandomForServer+RSA.decrypt(protocol.getMessageBody().getMessage(),ServerPrivateKey))));
                CurrentUser.setUserAES(cn.hutool.crypto.SecureUtil.aes(key.getEncoded()));
                //测试AES
                protocol = new NormalProtocol();
                head = new NormalProtocol.MessageHead();
                head.setVersion(CodeDynamicConfig.getProtocolVersion());
                head.setType("Test");
                protocol.setMessageHead(head);
                body = new NormalProtocol.MessageBody();
                body.setMessage(CurrentUser.getUserAES().encryptBase64("你好客户端"));
                body.setFileLong(0);
                protocol.setMessageBody(body);
                writer.write(gson.toJson(protocol));
                writer.newLine();
                writer.flush();
                do {
                    json = reader.readLine();
                } while (json == null);
                json = unicodeToString(json);
                protocol = getServer().protocolRequest(json);
                    if (protocol.getMessageHead().getVersion() != CodeDynamicConfig.getProtocolVersion() || !("Test".equals(protocol.getMessageHead().getType())))
                {
                    return;
                }
                logger.info("正在连接的客户端返回："+CurrentUser.getUserAES().decryptStr(protocol.getMessageBody().getMessage()));
                //升级通讯协议
                protocol = new NormalProtocol();
                head = new NormalProtocol.MessageHead();
                head.setVersion(CodeDynamicConfig.getProtocolVersion());
                head.setType("UpdateProtocol");
                protocol.setMessageHead(head);
                body = new NormalProtocol.MessageBody();
                body.setMessage(CurrentUser.getUserAES().encryptBase64("Update To All Encryption"));
                body.setFileLong(0);
                protocol.setMessageBody(body);
                writer.write(gson.toJson(protocol));
                writer.newLine();
                writer.flush();
                do {
                    json = reader.readLine();
                } while (json == null);
                json = unicodeToString(json);
                protocol = getServer().protocolRequest(json);
                if (protocol.getMessageHead().getVersion() != CodeDynamicConfig.getProtocolVersion() || !("UpdateProtocol".equals(protocol.getMessageHead().getType())))
                {
                    return;
                }
                if (!("ok".equals(CurrentUser.getUserAES().decryptStr(protocol.getMessageBody().getMessage()))))
                {
                    return;
                }
                System.gc();
                //握手全部完毕，后续是登录系统
                try {
                    LoginSystem(CurrentUser);
                } catch (RuntimeException e)
                {
                    SaveStackTrace.saveStackTrace(e);
                    if ("Login Mode wrong".equals(e.getMessage()) || !(CurrentUser.isUserLogined()))
                    {
                        return;
                    }
                }
                logger.info("正在开发中");
                logger.info("前面的区域以后再来探索吧");
            }
            catch (IOException ignored) {
            }
        }

    }

    private static boolean started = false;
    protected ServerSocket socket;
    private static ServerMain server;
    private Logger logger;
    protected UserAuthThread authThread;

    protected void RSA_KeyAutogenerate()
    {
        if (!(new File("Public.txt").exists()))
        {
            if (!(new File("Private.txt").exists()))
            {
                try {
                    RSA.generateKeyToFile("Public.txt", "Private.txt");
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
                    RSA.generateKeyToFile("Public.txt", "Private.txt");
                }
                catch (Exception e)
                {
                    SaveStackTrace.saveStackTrace(e);
                }
            }
        }
        else
        {
            if (!(new File("Private.txt").exists()))
            {
                logger.warning("系统检测到您的目录下存在公钥，但，不存在私钥，系统将为您覆盖一个新的rsa key");
                try {
                    RSA.generateKeyToFile("Public.txt", "Private.txt");
                }
                catch (Exception e)
                {
                    SaveStackTrace.saveStackTrace(e);
                }
            }
        }
    }

    private void StartRecvMessageThread(int ClientID)
    {
        RecvMessageThread recvMessageThread = new RecvMessageThread(ClientID);
        recvMessageThread.start();
    }
    @Contract(pure = true)
    public static ServerMain getServer() {
        return server;
    }
    //退出系统
    private void ExitSystem(int code)
    {
        System.exit(code);
    }
    //Logger init
    protected Logger initLogger()
    {
        return new Logger(false,false,null,null);
    }
    //服务端main
    public void start(int bindPort)
    {
        if (!started)
        {
            started = true;
            server = this;
            logger = initLogger();
            try {
                socket = new ServerSocket(bindPort);
            } catch (IOException e) {
                SaveStackTrace.saveStackTrace(e);
                throw new RuntimeException("Socket Create Failed", e);
            }
            RSA_KeyAutogenerate();
            authThread = new UserAuthThread();
            authThread.start();
            //debugonly，后续加入指令系统
            try {
                authThread.join();
            } catch (InterruptedException e) {
                SaveStackTrace.saveStackTrace(e);
                e.printStackTrace();
            }
        }
        else
            throw new RuntimeException("Server is Already Started");
    }
}
