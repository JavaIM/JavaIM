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
package org.yuezhikong.newServer.UserData;

import cn.hutool.crypto.symmetric.AES;
import org.yuezhikong.CodeDynamicConfig;
import org.yuezhikong.newServer.ServerMain;
import org.yuezhikong.newServer.ServerMain.RecvMessageThread;
import org.yuezhikong.newServer.plugin.event.events.UserLoginEvent;
import org.yuezhikong.utils.DataBase.Database;
import org.yuezhikong.utils.SaveStackTrace;

import java.net.Socket;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

@SuppressWarnings("unused")
public class userImpl implements user{
    private String UserName;
    private String PublicKey;
    private final int ClientID;
    private Socket UserSocket;
    private boolean UserLogined;
    private RecvMessageThread recvMessageThread;
    private Permission PermissionLevel;
    private AES UserAES;
    private final boolean Server;
    public userImpl(Socket socket, int ClientID,boolean isServer)
    {
        UserSocket = socket;
        this.ClientID = ClientID;
        UserLogined = false;
        Server = isServer;
    }

    @Override
    public void setRecvMessageThread(RecvMessageThread thread)
    {
        recvMessageThread = thread;
    }

    @Override
    public RecvMessageThread getRecvMessageThread() {
        return recvMessageThread;
    }

    @Override
    public Socket getUserSocket() {
        return UserSocket;
    }

    @Override
    public void setPublicKey(String publicKey) {
        PublicKey = publicKey;
    }

    @Override
    public String getPublicKey() {
        return PublicKey;
    }

    @Override
    public void setUserAES(AES userAES) {
        UserAES = userAES;
    }

    @Override
    public AES getUserAES() {
        return UserAES;
    }

    @Override
    public int getClientID() {
        return ClientID;
    }
    @Override
    public String getUserName() {
        return UserName;
    }

    @Override
    public void UserLogin(String UserName)
    {
        UserLoginEvent userLoginEvent = new UserLoginEvent(UserName);
        ServerMain.getServer().getPluginManager().callEvent(userLoginEvent);
        this.UserName = UserName;
        UserLogined = true;
    }

    @Override
    public boolean isUserLogined() {
        return UserLogined;
    }

    @Override
    public void UserDisconnect() {
        if (UserName == null || UserName.equals(""))
        {
            ServerMain.getServer().getLogger().info("一个客户端已经断开连接");
        }
        else
        {
            ServerMain.getServer().getLogger().info("用户："+UserName+"已经断开连接");
        }
        UserSocket = null;
        UserName = null;
        PublicKey = null;
        UserLogined = false;
        UserAES = null;
        recvMessageThread.interrupt();
    }

    @Override
    public void SetUserPermission(int permissionLevel, boolean FlashPermission) {
        ServerMain.getServer().getLogger().info("权限发生更新，用户："+getUserName()+"获得了"+permissionLevel+"级别权限");
        if (!FlashPermission)
        {
            new Thread()
            {
                @Override
                public void run() {
                    this.setName("SQL Worker");
                    try
                    {
                        Connection DatabaseConnection = Database.Init(CodeDynamicConfig.GetMySQLDataBaseHost(), CodeDynamicConfig.GetMySQLDataBasePort(), CodeDynamicConfig.GetMySQLDataBaseName(), CodeDynamicConfig.GetMySQLDataBaseUser(), CodeDynamicConfig.GetMySQLDataBasePasswd());
                        String sql = "UPDATE UserData SET Permission = ? where UserName = ?";
                        PreparedStatement ps = DatabaseConnection.prepareStatement(sql);
                        ps.setInt(1,permissionLevel);
                        ps.setString(2,userImpl.this.getUserName());
                        ps.executeUpdate();
                    } catch (Database.DatabaseException | SQLException e)
                    {
                        SaveStackTrace.saveStackTrace(e);
                    }
                    finally {
                        Database.close();
                    }
                }
            }.start();
        }
        PermissionLevel = Permission.ToPermission(permissionLevel);
    }

    @Override
    public Permission getUserPermission() {
        return PermissionLevel;
    }

    @Override
    public boolean isServer() {
        return Server;
    }

    //被暂缓的服务端管理功能
    @Override
    public void setMuteTime(long muteTime) {
    }
    @Override
    public void setMuted(boolean Muted) {
    }

}
