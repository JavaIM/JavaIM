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
package org.yuezhikong.Server.LoginSystem;

import cn.hutool.crypto.SecureUtil;
import com.google.gson.Gson;
import org.jetbrains.annotations.NotNull;
import org.yuezhikong.CodeDynamicConfig;
import org.yuezhikong.Server.UserData.user;
import org.yuezhikong.Server.api.ServerAPI;
import org.yuezhikong.utils.CustomExceptions.ModeDisabledException;
import org.yuezhikong.utils.DataBase.Database;
import org.yuezhikong.utils.Protocol.NormalProtocol;
import org.yuezhikong.utils.RSA;
import org.yuezhikong.utils.SaveStackTrace;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;

import static org.yuezhikong.CodeDynamicConfig.GetRSA_Mode;
import static org.yuezhikong.CodeDynamicConfig.isAES_Mode;
import static org.yuezhikong.Server.Server.DEBUG;
import static org.yuezhikong.Server.api.ServerAPI.SendMessageToUser;

public class LoginORRegisterRequestThread extends Thread{
    private final String UserName;
    private final String Passwd;
    private boolean Success;
    private final user RequestUser;
    public LoginORRegisterRequestThread(@NotNull String UserName, @NotNull String Passwd, @NotNull user RequestUser)
    {
        this.UserName = UserName;
        this.Passwd = Passwd;
        this.Success = false;
        this.RequestUser = RequestUser;
    }
    @Override
    public void run() {
        this.setUncaughtExceptionHandler((t, e) -> SaveStackTrace.saveStackTrace(e));
        this.setName("SQL Request Thread");
        if ("Server".equals(UserName))
        {
            Success = false;
            ServerAPI.SendMessageToUser(RequestUser,"不得使用被禁止的用户名：Server");
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
                    ServerAPI.SendMessageToUser(RequestUser,"此用户已经登录了!");
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
                                SendMessageToUser(RequestUser,"您的账户已被永久封禁！");
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
                        BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(RequestUser.GetUserSocket().getOutputStream()));
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
                        if (GetRSA_Mode()) {
                            String UserPublicKey = RequestUser.GetUserPublicKey();
                            if (UserPublicKey == null) {
                                throw new NullPointerException();
                            }
                            data = java.net.URLEncoder.encode(data, StandardCharsets.UTF_8);
                            if (isAES_Mode())
                            {
                                data = RequestUser.GetUserAES().encryptBase64(data);
                            }
                            else {
                                data = RSA.encrypt(data, UserPublicKey);
                            }
                        }
                        writer.write(data);
                        writer.newLine();
                        writer.flush();
                    } catch (IOException e) {
                        SaveStackTrace.saveStackTrace(e);
                    } catch (ModeDisabledException ignored) {
                    }
                }
                public Thread start2()
                {
                    super.start();
                    return this;
                }
            }.start2().join();
        } catch (Database.DatabaseException | SQLException e)
        {
            RequestUser.UserDisconnect();
            SaveStackTrace.saveStackTrace(e);
            Success = false;
        }
        catch (InterruptedException e) {
            SaveStackTrace.saveStackTrace(e);
        } finally {
            Database.close();
        }
    }

    public boolean isSuccess() {
        return Success;
    }
}
