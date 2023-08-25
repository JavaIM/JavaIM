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

import cn.hutool.crypto.SecureUtil;
import cn.hutool.crypto.symmetric.SymmetricAlgorithm;
import com.google.gson.Gson;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.FileUtils;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.yuezhikong.CodeDynamicConfig;
import org.yuezhikong.CrashReport;
import org.yuezhikong.GeneralMethod;
import org.yuezhikong.newServer.UserData.user;
import org.yuezhikong.newServer.UserData.userImpl;
import org.yuezhikong.newServer.api.SingleAPI;
import org.yuezhikong.newServer.api.api;
import org.yuezhikong.newServer.plugin.PluginManager;
import org.yuezhikong.newServer.plugin.SimplePluginManager;
import org.yuezhikong.newServer.plugin.event.events.PreLoginEvent;
import org.yuezhikong.utils.DataBase.Database;
import org.yuezhikong.utils.Logger;
import org.yuezhikong.utils.Protocol.LoginProtocol;
import org.yuezhikong.utils.Protocol.NormalProtocol;
import org.yuezhikong.utils.Protocol.TransferProtocol;
import org.yuezhikong.utils.RSA;
import org.yuezhikong.utils.SaveStackTrace;

import javax.crypto.SecretKey;
import javax.security.auth.login.AccountNotFoundException;
import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.UUID;

/**
 * 新服务端
 * @author AlexLiuDev233
 */
public non-sealed class ServerMain extends GeneralMethod implements ServerInterface{
    private final List<user> Users = new ArrayList<>();
    private int clientIDAll = 0;
    protected static class UserAuthThread extends Thread
    {
        private final Logger logger;
        private final ServerSocket socket;
        public UserAuthThread()
        {
            this.logger = getServer().logger;
            this.socket = getServer().socket;
            this.setDaemon(true);
        }
        @Override
        public void run() {
            this.setName("UserAuthThread");
            this.setUncaughtExceptionHandler(new CrashReport());
            while (true)
            {
                Socket clientSocket;//接受客户端Socket请求
                try {
                    clientSocket = socket.accept();
                    clientSocket.setSoTimeout(CodeDynamicConfig.SocketTimeout);
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
                user CurrentUser = new userImpl(clientSocket,getServer().clientIDAll,false);//创建用户class
                getServer().Users.add(CurrentUser);
                getServer().Users.set(getServer().clientIDAll,CurrentUser);//添加到List中
                getServer().StartRecvMessageThread(getServer().clientIDAll);//启动RecvMessage线程
                getServer().clientIDAll++;//当前的最大ClientID加1
                logger.info("连入了新的socket请求");
                logger.info("基本信息如下：");
                logger.info("远程ip地址为："+clientSocket.getRemoteSocketAddress());
                logger.info("远程端口为："+clientSocket.getPort());
            }
        }
    }


    @Override
    public List<user> getUsers() {
        return Users;
    }

    public static class RecvMessageThread extends Thread
    {
        private final user CurrentUser;
        public RecvMessageThread(int ClientID)
        {
            this.setDaemon(true);
            CurrentUser = getServer().Users.get(ClientID);
        }
        private boolean CheckPassword(String UserName,String Passwd,user RequestUser)
        {
            try {
                class IOWorker extends Thread {
                    private boolean Success = false;
                    public IOWorker()
                    {
                        this.setDaemon(true);
                    }
                    @Override
                    public void run() {
                        this.setUncaughtExceptionHandler((t, e) -> SaveStackTrace.saveStackTrace(e));
                        this.setName("SQL Request Thread");
                        api API = getServer().API;
                        if ("Server".equals(UserName))
                        {
                            Success = false;
                            API.SendMessageToUser(RequestUser,"不得使用被禁止的用户名：Server");
                            try {
                                new Thread()
                                {
                                    @Override
                                    public void run() {
                                        try {
                                            this.setName("IO Worker");
                                            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(RequestUser.getUserSocket().getOutputStream(), StandardCharsets.UTF_8));
                                            Gson gson = new Gson();
                                            NormalProtocol protocolData = new NormalProtocol();
                                            NormalProtocol.MessageHead MessageHead = new NormalProtocol.MessageHead();
                                            MessageHead.setType("Fail");
                                            MessageHead.setVersion(CodeDynamicConfig.getProtocolVersion());
                                            protocolData.setMessageHead(MessageHead);
                                            String data = gson.toJson(protocolData);
                                            data = RequestUser.getUserAES().encryptBase64(data);
                                            writer.write(data);
                                            writer.newLine();
                                            writer.flush();
                                        } catch (IOException e)
                                        {
                                            if (e instanceof SocketTimeoutException)
                                            {
                                                getServer().getLogger().info("用户："+CurrentUser.getUserName()+"长时间无回应，已断开连接");
                                            }
                                            CurrentUser.UserDisconnect();
                                        }
                                    }
                                    public Thread start2()
                                    {
                                        start();
                                        return this;
                                    }
                                }.start2().join();
                            } catch (InterruptedException ex) {
                                SaveStackTrace.saveStackTrace(ex);
                            }
                            return;
                        }
                        try
                        {
                            Connection DatabaseConnection = Database.Init(CodeDynamicConfig.GetMySQLDataBaseHost(), CodeDynamicConfig.GetMySQLDataBasePort(), CodeDynamicConfig.GetMySQLDataBaseName(), CodeDynamicConfig.GetMySQLDataBaseUser(), CodeDynamicConfig.GetMySQLDataBasePasswd());
                            String sql = "select * from UserData where UserName = ?";
                            PreparedStatement ps = DatabaseConnection.prepareStatement(sql);
                            ps.setString(1, UserName);
                            ResultSet rs = ps.executeQuery();
                            //如果找到了这个用户，启动登录逻辑
                            if (rs.next()) {
                                String salt;
                                String sha256;
                                if (rs.getInt("UserLogged") == 1) {
                                    Success = false;
                                    API.SendMessageToUser(RequestUser, "此用户已经登录了!");
                                    try {
                                        new Thread() {
                                            @Override
                                            public void run() {
                                                try {
                                                    this.setName("IO Worker");
                                                    BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(RequestUser.getUserSocket().getOutputStream(), StandardCharsets.UTF_8));
                                                    Gson gson = new Gson();
                                                    NormalProtocol protocolData = new NormalProtocol();
                                                    NormalProtocol.MessageHead MessageHead = new NormalProtocol.MessageHead();
                                                    MessageHead.setType("Fail");
                                                    MessageHead.setVersion(CodeDynamicConfig.getProtocolVersion());
                                                    protocolData.setMessageHead(MessageHead);
                                                    String data = gson.toJson(protocolData);
                                                    data = RequestUser.getUserAES().encryptBase64(data);
                                                    writer.write(data);
                                                    writer.newLine();
                                                    writer.flush();
                                                } catch (IOException e) {
                                                    if (e instanceof SocketTimeoutException)
                                                    {
                                                        getServer().getLogger().info("用户："+CurrentUser.getUserName()+"长时间无回应，已断开连接");
                                                    }
                                                    CurrentUser.UserDisconnect();
                                                }
                                            }

                                            public Thread start2() {
                                                start();
                                                return this;
                                            }
                                        }.start2().join();
                                    } catch (InterruptedException ex) {
                                        SaveStackTrace.saveStackTrace(ex);
                                    }
                                    return;
                                }
                                salt = rs.getString("salt");
                                //为保护安全，保存密码是加盐sha256
                                sha256 = SecureUtil.sha256(Passwd + salt);
                                if (rs.getString("Passwd").equals(sha256)) {
                                    //权限处理
                                    int PermissionLevel = rs.getInt("Permission");
                                    if (PermissionLevel != 0) {
                                        if (PermissionLevel != 1) {
                                            if (PermissionLevel == -1) {
                                                API.SendMessageToUser(RequestUser, "您的账户已被永久封禁！");
                                                Success = false;
                                                try {
                                                    new Thread() {
                                                        @Override
                                                        public void run() {
                                                            try {
                                                                this.setName("IO Worker");
                                                                BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(RequestUser.getUserSocket().getOutputStream(), StandardCharsets.UTF_8));
                                                                Gson gson = new Gson();
                                                                NormalProtocol protocolData = new NormalProtocol();
                                                                NormalProtocol.MessageHead MessageHead = new NormalProtocol.MessageHead();
                                                                MessageHead.setType("Fail");
                                                                MessageHead.setVersion(CodeDynamicConfig.getProtocolVersion());
                                                                protocolData.setMessageHead(MessageHead);
                                                                String data = gson.toJson(protocolData);
                                                                data = RequestUser.getUserAES().encryptBase64(data);
                                                                writer.write(data);
                                                                writer.newLine();
                                                                writer.flush();
                                                            } catch (IOException e) {
                                                                if (e instanceof SocketTimeoutException)
                                                                {
                                                                    getServer().getLogger().info("用户："+CurrentUser.getUserName()+"长时间无回应，已断开连接");
                                                                }
                                                                CurrentUser.UserDisconnect();
                                                            }
                                                        }

                                                        public Thread start2() {
                                                            start();
                                                            return this;
                                                        }
                                                    }.start2().join();
                                                } catch (InterruptedException ex) {
                                                    SaveStackTrace.saveStackTrace(ex);
                                                }
                                                return;
                                            }
                                        }
                                    }
                                    //处理禁言
                                    long muted = rs.getLong("UserMuted");
                                    long MuteTime = rs.getLong("UserMuteTime");
                                    if (muted == 1) {
                                        RequestUser.setMuteTime(MuteTime);
                                        RequestUser.setMuted(true);
                                    }
                                    //设置已成功并更新内存中的用户信息
                                    Success = true;
                                    RequestUser.SetUserPermission(PermissionLevel, true);
                                    RequestUser.UserLogin(UserName);
                                    //设置用户已登录了
                                    sql = "UPDATE UserData SET UserLogged = 1 where UserName = ?;";
                                    ps = DatabaseConnection.prepareStatement(sql);
                                    ps.setString(1, UserName);
                                    ps.executeUpdate();
                                }
                            }
                            //如果没找到这个用户，启动注册逻辑
                            else {
                                String salt;
                                do {
                                    //寻找一个安全的盐
                                    salt = UUID.randomUUID().toString();
                                    sql = "select * from UserData where salt = ?";
                                    ps = DatabaseConnection.prepareStatement(sql);
                                    ps.setString(1, sql);
                                    rs = ps.executeQuery();
                                } while (rs.next());
                                //密码加盐并保存
                                String sha256 = SecureUtil.sha256(Passwd + salt);
                                sql = "INSERT INTO `UserData` (`Permission`,`UserName`, `Passwd`,`salt`) VALUES (0,?, ?, ?);";
                                ps = DatabaseConnection.prepareStatement(sql);
                                ps.setString(1, UserName);
                                ps.setString(2, sha256);
                                ps.setString(3, salt);
                                ps.executeUpdate();
                                //设置已成功并更新内存中的用户信息
                                Success = true;
                                RequestUser.UserLogin(UserName);
                                //设置用户已登录了
                                sql = "UPDATE UserData SET UserLogged = 1 where UserName = ?;";
                                ps = DatabaseConnection.prepareStatement(sql);
                                ps.setString(1, UserName);
                                ps.executeUpdate();
                            }
                            if (Success) {
                                //如果登录/注册成功，开始分发Token
                                String token;
                                do {
                                    //获取一个安全的，不重复的token
                                    token = UUID.randomUUID().toString();
                                    sql = "select * from UserData where token = ?";
                                    ps = DatabaseConnection.prepareStatement(sql);
                                    ps.setString(1, sql);
                                    rs = ps.executeQuery();
                                } while (rs.next());
                                //将这个token填入数据库
                                sql = "UPDATE UserData SET token = ? where UserName = ?;";
                                ps = DatabaseConnection.prepareStatement(sql);
                                ps.setString(1, token);
                                ps.setString(2, UserName);
                                ps.executeUpdate();
                                //发送给登录的yonghu
                                final String finalToken = token;
                                new Thread() {
                                    @Override
                                    public void run() {
                                        this.setName("I/O Thread");
                                        try {
                                            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(RequestUser.getUserSocket().getOutputStream(), StandardCharsets.UTF_8));
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
                                            if (e instanceof SocketTimeoutException)
                                            {
                                                getServer().getLogger().info("用户："+CurrentUser.getUserName()+"长时间无回应，已断开连接");
                                            }
                                            CurrentUser.UserDisconnect();
                                        }
                                    }

                                    public Thread start2() {
                                        super.start();
                                        return this;
                                    }
                                }.start2().join();
                            }
                        }
                        catch (Database.DatabaseException | SQLException e) {
                            RequestUser.UserDisconnect();
                            SaveStackTrace.saveStackTrace(e);
                            Success = false;
                            try {
                                new Thread() {
                                    @Override
                                    public void run() {
                                        try {
                                            this.setName("IO Worker");
                                            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(RequestUser.getUserSocket().getOutputStream(), StandardCharsets.UTF_8));
                                            Gson gson = new Gson();
                                            NormalProtocol protocolData = new NormalProtocol();
                                            NormalProtocol.MessageHead MessageHead = new NormalProtocol.MessageHead();
                                            MessageHead.setType("Fail");
                                            MessageHead.setVersion(CodeDynamicConfig.getProtocolVersion());
                                            protocolData.setMessageHead(MessageHead);
                                            String data = gson.toJson(protocolData);
                                            data = RequestUser.getUserAES().encryptBase64(data);
                                            writer.write(data);
                                            writer.newLine();
                                            writer.flush();
                                        } catch (IOException e) {
                                            if (e instanceof SocketTimeoutException)
                                            {
                                                getServer().getLogger().info("用户："+CurrentUser.getUserName()+"长时间无回应，已断开连接");
                                            }
                                            CurrentUser.UserDisconnect();
                                        }
                                    }

                                    public Thread start2() {
                                        start();
                                        return this;
                                    }
                                }.start2().join();
                            } catch (InterruptedException ex) {
                                SaveStackTrace.saveStackTrace(ex);
                            }
                        } catch (InterruptedException e) {
                            SaveStackTrace.saveStackTrace(e);
                        }
                        finally {
                            Database.close();
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
                if (new IOWorker()
                        .start2()
                        .join2()
                        .isSuccess())
                {
                    PreLoginEvent event = new PreLoginEvent(UserName,false);
                    getServer().getPluginManager().callEvent(event);
                    return !event.isCancel();
                }
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
                if ("Alive".equals(json))
                {
                    json = null;
                }
            } while (json == null);
            json = getServer().unicodeToString(json);
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
                                    if (rs.getInt("UserLogged") != 1)
                                    {
                                        sql = "UPDATE UserData SET UserLogged = 1 where UserName = ?;";
                                        ps = DatabaseConnection.prepareStatement(sql);
                                        ps.setString(1, rs.getString("UserName"));
                                        ps.executeUpdate();
                                        username.add(rs.getString("UserName"));
                                    }
                                    else
                                        ServerMain.getServer().getServerAPI().SendMessageToUser(CurrentUser,"此用户已经登录了！");
                                }
                                DatabaseConnection.close();
                            } catch (SQLException | Database.DatabaseException e) {
                                SaveStackTrace.saveStackTrace(e);
                            }
                            finally {
                                Database.close();
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
                        if ("Alive".equals(json))
                        {
                            json = null;
                        }
                    } while (json == null);
                    json = getServer().unicodeToString(json);
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
                    PreLoginEvent event = new PreLoginEvent(username.get(0),true);
                    getServer().getPluginManager().callEvent(event);
                    if (event.isCancel())
                    {
                        User.UserDisconnect();
                    }
                    else
                    {
                        User.UserLogin(username.get(0));
                    }
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
            this.setUncaughtExceptionHandler((Thread t,Throwable throwable) -> {
                if (CodeDynamicConfig.GetDebugMode())
                {
                    SaveStackTrace.saveStackTrace(throwable);
                }
                getServer().getLogger().warning("处理用户："+CurrentUser.getUserName()+"的线程出现错误");
                getServer().getLogger().warning("正在强行踢出此用户");
                CurrentUser.UserDisconnect();
            });
            Logger logger = getServer().logger;
            api API = getServer().API;
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
                logger.info("正在连接的客户端返回："+getServer().unicodeToString(reader.readLine()));
                writer.write("你好，客户端");
                writer.newLine();
                writer.flush();
                logger.info("正在连接的客户端返回："+getServer().unicodeToString(reader.readLine()));
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
                    if ("Alive".equals(json))
                    {
                        json = null;
                    }
                } while (json == null);
                json = getServer().unicodeToString(json);
                protocol = getServer().protocolRequest(json);
                if (protocol.getMessageHead().getVersion() != CodeDynamicConfig.getProtocolVersion() || !("Test".equals(protocol.getMessageHead().getType())))
                {
                    return;
                }
                logger.info("正在连接的客户端返回："+protocol.getMessageBody().getMessage());
                //RSA Key传递
                do {
                    json = reader.readLine();
                    if ("Alive".equals(json))
                    {
                        json = null;
                    }
                } while (json == null);
                json = getServer().unicodeToString(json);
                protocol = getServer().protocolRequest(json);
                if (protocol.getMessageHead().getVersion() != CodeDynamicConfig.getProtocolVersion() || !("Encryption".equals(protocol.getMessageHead().getType())))
                {
                    return;
                }
                try {
                    CurrentUser.setPublicKey(RSA.decrypt(protocol.getMessageBody().getMessage(), ServerPrivateKey));
                } catch (cn.hutool.crypto.CryptoException e)
                {
                    writer.write("Decryption Error");
                    writer.newLine();
                    writer.flush();
                    getServer().getLogger().warning("正在连接的客户端发送的信息无法被解密！");
                    getServer().getLogger().warning("即将断开对于此用户的连接");
                    CurrentUser.UserDisconnect();
                    return;
                }
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
                    if ("Alive".equals(json))
                    {
                        json = null;
                    }
                } while (json == null);
                json = getServer().unicodeToString(json);
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
                    if ("Alive".equals(json))
                    {
                        json = null;
                    }
                } while (json == null);
                json = getServer().unicodeToString(json);
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
                    if ("Alive".equals(json))
                    {
                        json = null;
                    }
                } while (json == null);
                json = getServer().unicodeToString(json);
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
                    if ("Alive".equals(json))
                    {
                        json = null;
                    }
                } while (json == null);
                json = getServer().unicodeToString(json);
                protocol = getServer().protocolRequest(json);
                if (protocol.getMessageHead().getVersion() != CodeDynamicConfig.getProtocolVersion() || !("UpdateProtocol".equals(protocol.getMessageHead().getType())))
                {
                    return;
                }
                if (!("ok".equals(CurrentUser.getUserAES().decryptStr(protocol.getMessageBody().getMessage()))))
                {
                    return;
                }
                //询问是否允许TransferProtocol
                protocol = new NormalProtocol();
                head = new NormalProtocol.MessageHead();
                head.setVersion(CodeDynamicConfig.getProtocolVersion());
                head.setType("options");
                protocol.setMessageHead(head);
                body = new NormalProtocol.MessageBody();
                body.setMessage("AllowTransferProtocol");
                body.setFileLong(0);
                protocol.setMessageBody(body);
                writer.write(CurrentUser.getUserAES().encryptBase64(gson.toJson(protocol)));
                writer.newLine();
                writer.flush();

                do {
                    json = reader.readLine();
                    if ("Alive".equals(json))
                    {
                        json = null;
                    }
                } while (json == null);
                json = getServer().unicodeToString(json);
                json = CurrentUser.getUserAES().decryptStr(json);
                protocol = getServer().protocolRequest(json);
                if (protocol.getMessageHead().getVersion() != CodeDynamicConfig.getProtocolVersion() || !("options".equals(protocol.getMessageHead().getType())))
                {
                    return;
                }
                CurrentUser.setAllowedTransferProtocol("Enable".equals(protocol.getMessageBody().getMessage()));
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
                //登录完毕，开始聊天
                ChatRequest chatRequest = new ChatRequest();
                while (true) {
                    String ChatMsg;
                    do {
                        ChatMsg = reader.readLine();
                        if ("Alive".equals(ChatMsg))
                        {
                            ChatMsg = null;
                        }
                    } while (ChatMsg == null);
                    ChatMsg = getServer().unicodeToString(ChatMsg);
                    ChatMsg = CurrentUser.getUserAES().decryptStr(ChatMsg);
                    protocol = getServer().protocolRequest(ChatMsg);
                    if (protocol.getMessageHead().getVersion() != CodeDynamicConfig.getProtocolVersion())
                    {
                        CurrentUser.UserDisconnect();
                        return;
                    }
                    if ("ChangePassword".equals(protocol.getMessageHead().getType()))
                    {
                        getServer().getServerAPI().ChangeUserPassword(CurrentUser,protocol.getMessageBody().getMessage());
                        continue;
                    }
                    if ("NextIsTransferProtocol".equals(protocol.getMessageHead().getType()))
                    {
                        do {
                            json = reader.readLine();
                            if ("Alive".equals(json))
                            {
                                json = null;
                            }
                        } while (json == null);
                        json = getServer().unicodeToString(json);
                        json = CurrentUser.getUserAES().decryptStr(json);
                        TransferProtocol transferProtocol = gson.fromJson(json, TransferProtocol.class);
                        if (transferProtocol.getTransferProtocolHead().getVersion() != CodeDynamicConfig.getProtocolVersion())
                        {
                            protocol = new NormalProtocol();
                            head = new NormalProtocol.MessageHead();
                            head.setVersion(CodeDynamicConfig.getProtocolVersion());
                            head.setType("Result");
                            protocol.setMessageHead(head);
                            body = new NormalProtocol.MessageBody();
                            body.setMessage("TransferProtocolVersionIsNotSupport");
                            protocol.setMessageBody(body);
                            writer.write(CurrentUser.getUserAES().encryptBase64(gson.toJson(protocol)));
                            writer.newLine();
                            writer.flush();
                            continue;
                        }
                        else if (!CodeDynamicConfig.AllowedTransferProtocol)
                        {
                            protocol = new NormalProtocol();
                            head = new NormalProtocol.MessageHead();
                            head.setVersion(CodeDynamicConfig.getProtocolVersion());
                            head.setType("Result");
                            protocol.setMessageHead(head);
                            body = new NormalProtocol.MessageBody();
                            body.setMessage("ThisServerDisallowedTransferProtocol");
                            protocol.setMessageBody(body);
                            writer.write(CurrentUser.getUserAES().encryptBase64(gson.toJson(protocol)));
                            writer.newLine();
                            writer.flush();
                            continue;
                        }
                        else {
                            try {
                                user TargetUser = getServer().getServerAPI().GetUserByUserName(transferProtocol.getTransferProtocolHead().getTargetUserName(),
                                        ServerMain.getServer());
                                if (TargetUser.isAllowedTransferProtocol())
                                {
                                    BufferedWriter writer1 = new BufferedWriter(new OutputStreamWriter(TargetUser
                                            .getUserSocket().getOutputStream()));

                                    protocol = new NormalProtocol();
                                    head = new NormalProtocol.MessageHead();
                                    head.setVersion(CodeDynamicConfig.getProtocolVersion());
                                    head.setType("NextIsTransferProtocol");
                                    protocol.setMessageHead(head);
                                    body = new NormalProtocol.MessageBody();
                                    body.setMessage(CurrentUser.getUserName());
                                    protocol.setMessageBody(body);
                                    writer1.write(TargetUser.getUserAES().encryptBase64(gson.toJson(protocol)));
                                    writer1.newLine();
                                    writer1.flush();

                                    writer1.write(TargetUser.getUserAES().encryptBase64(json));
                                    writer1.newLine();
                                    writer1.flush();

                                    protocol = new NormalProtocol();
                                    head = new NormalProtocol.MessageHead();
                                    head.setVersion(CodeDynamicConfig.getProtocolVersion());
                                    head.setType("Result");
                                    protocol.setMessageHead(head);
                                    body = new NormalProtocol.MessageBody();
                                    body.setMessage("Success");
                                }
                                else
                                {
                                    protocol = new NormalProtocol();
                                    head = new NormalProtocol.MessageHead();
                                    head.setVersion(CodeDynamicConfig.getProtocolVersion());
                                    head.setType("Result");
                                    protocol.setMessageHead(head);
                                    body = new NormalProtocol.MessageBody();
                                    body.setMessage("ThisUserDisallowedTransferProtocol");
                                }
                                protocol.setMessageBody(body);
                                writer.write(CurrentUser.getUserAES().encryptBase64(gson.toJson(protocol)));
                                writer.newLine();
                                writer.flush();
                                continue;
                            } catch (AccountNotFoundException e) {
                                protocol = new NormalProtocol();
                                head = new NormalProtocol.MessageHead();
                                head.setVersion(CodeDynamicConfig.getProtocolVersion());
                                head.setType("Result");
                                protocol.setMessageHead(head);
                                body = new NormalProtocol.MessageBody();
                                body.setMessage("ThisUserNotFound");
                                protocol.setMessageBody(body);
                                writer.write(CurrentUser.getUserAES().encryptBase64(gson.toJson(protocol)));
                                writer.newLine();
                                writer.flush();
                            }
                        }
                        continue;
                    }
                    if (!("Chat".equals(protocol.getMessageHead().getType())))
                    {
                        CurrentUser.UserDisconnect();
                        return;
                    }
                    ChatRequest.ChatRequestInput input = new ChatRequest.ChatRequestInput(CurrentUser,protocol);
                    if (chatRequest.UserChatRequests(input))
                    {
                        continue;
                    }
                    final String ChatMessage = input.getChatMessage();
                    logger.ChatMsg(ChatMessage);
                    API.SendMessageToAllClient(ChatMessage,getServer());
                }
            }
            catch (IOException e) {
                if (e instanceof SocketTimeoutException)
                {
                    logger.info("用户："+CurrentUser.getUserName()+"长时间无回应，已断开连接");
                }
                CurrentUser.UserDisconnect();
            }
        }

    }

    private PluginManager pluginManager;
    private static boolean started = false;
    protected ServerSocket socket;
    private static ServerMain server;
    private Logger logger;
    protected UserAuthThread authThread;
    private api API;

    @Override
    public PluginManager getPluginManager() {
        return pluginManager;
    }

    @Override
    public api getServerAPI() {
        return API;
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

    @Override
    public Logger getLogger() {
        return logger;
    }

    private static final Object lock = new Object();
    private static final List<Runnable> codelist = new ArrayList<>();

    @Override
    public void runOnMainThread(Runnable code) {
        codelist.add(code);
        synchronized (lock)
        {
            lock.notifyAll();
        }
    }

    //Logger init
    protected Logger initLogger()
    {
        return new Logger();
    }
    /**
     * 重置数据库中的用户登录状态，因为服务器重启后，所有用户都会被踢出
     */
    protected void UserLoginStatusReset() {
        Runnable SQLUpdateThread = () -> {
            try {
                Connection DatabaseConnection = Database.Init(CodeDynamicConfig.GetMySQLDataBaseHost(), CodeDynamicConfig.GetMySQLDataBasePort(), CodeDynamicConfig.GetMySQLDataBaseName(), CodeDynamicConfig.GetMySQLDataBaseUser(), CodeDynamicConfig.GetMySQLDataBasePasswd());
                String sql = "UPDATE UserData SET UserLogged = 0 where UserLogged = 1;";
                DatabaseConnection.createStatement().executeUpdate(sql);
                DatabaseConnection.close();
            } catch (Exception e) {
                SaveStackTrace.saveStackTrace(e);
            } finally {
                Database.close();
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
            RSA_KeyAutogenerate("Public.txt","Private.txt",logger);
            UserLoginStatusReset();
            API = new SingleAPI();
            authThread = new UserAuthThread();
            authThread.start();
            pluginManager = new SimplePluginManager();
            pluginManager.LoadPluginOnDirectory(new File("./plugins"));
            StartCommandSystem();
            //主线程执行代码
            while (true) {
                try {
                    synchronized (lock) {
                        lock.wait();
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                    break;
                }
                for (Runnable runnable : codelist) {
                    runnable.run();
                }
                codelist.clear();
            }
        }
        else
            throw new RuntimeException("Server is Already Started");
    }

    //命令系统
    protected void StartCommandSystem() {
        new Thread()
        {
            @Override
            public void run() {
                this.setName("Command Thread");
                this.setUncaughtExceptionHandler(new CrashReport());
                //服务端虚拟账户初始化
                user Console = new userImpl(null,0,true);
                Console.setAllowedTransferProtocol(false);
                Console.UserLogin("Server");
                Console.SetUserPermission(1,true);
                //IO初始化
                Scanner scanner = new Scanner(System.in);
                //ChatRequest初始化
                ChatRequest chatRequest = new ChatRequest();
                //命令系统
                while (true)
                {
                    String Command = "/"+scanner.nextLine();
                    if ("/ForceClose".equals(Command))
                    {
                        logger.info("这将会强制关闭服务端");
                        logger.info("输入ForceClose来确认，其他取消");
                        if ("ForceClose".equals(scanner.nextLine()))
                        {
                            try {
                                pluginManager.UnLoadAllPlugin();
                            } catch (IOException e) {
                                SaveStackTrace.saveStackTrace(e);
                            }
                            System.exit(0);
                        }
                        System.out.print(">");
                        continue;
                    }
                    chatRequest.CommandRequest(new ChatRequest.ChatRequestInput(Console,Command));
                }
            }
        }.start();
    }
}
