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
package org.yuezhikong;

import javafx.application.Application;
import org.yuezhikong.GUITest.MainGUI.GUI;
import org.yuezhikong.Server.Server;
import org.yuezhikong.utils.SaveStackTrace;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Proxy;
import java.net.Socket;
import java.util.Scanner;

import static org.yuezhikong.CodeDynamicConfig.*;

public class Main {
    private static final org.yuezhikong.utils.Logger logger = new org.yuezhikong.utils.Logger(false,false,null,null);
    private static Main instance;
    public static Main getInstance()
    {
        if (instance == null)
        {
            instance = new Main();
        }
        return instance;
    }
    /**
     * @apiNote 从jar中释放文件到jar文件夹下的某一子文件夹
     * @param name 需要从jar中释放的文件在jar中的路径路径
     * @param dir 需要释放到的文件夹
     */
    public void saveJarFiles(String name,String dir) {
        File JarOnDir = new File(this.getClass().getProtectionDomain().getCodeSource().getLocation().getFile()).getParentFile();
        File LibsDir = new File(JarOnDir.getPath()+dir);
        if (!LibsDir.exists()) {// 父目录不存在时先创建
            try {
                if (!LibsDir.mkdirs())
                {
                    System.err.println("创建文件夹失败");
                    System.exit(-1);
                }
            }
            catch (Exception e)
            {
                SaveStackTrace.saveStackTrace(e);
                System.exit(-1);
            }
        }
        File CheckFile = new File(LibsDir.getPath()+name);
        if (!CheckFile.exists())
        {
            try {
                if (!CheckFile.createNewFile())
                {
                    System.err.println("创建文件失败");
                    System.exit(-1);
                }
            }
            catch (Exception e)
            {
                System.err.println("创建文件时出现异常");
                SaveStackTrace.saveStackTrace(e);
                System.exit(-1);
            }
            InputStream inputStream = this.getClass().getResourceAsStream(name);
            try {
                assert inputStream != null;
                OutputStream os;
                os = new FileOutputStream(CheckFile);
                int index;
                byte[] bytes = new byte[10240];
                while ((index = inputStream.read(bytes)) != -1) {
                    os.write(bytes, 0, index);
                }
                os.flush();
                os.close();
                inputStream.close();
            }
            catch (Exception e)
            {
                System.err.println("创建文件时出现异常");
                SaveStackTrace.saveStackTrace(e);
                System.exit(-1);
            }
        }
    }

    public static byte[] subBytes(byte[]src,int begin,int count){
        byte[]bs=new byte[count];
        System.arraycopy(src, begin, bs, begin, count);
        return bs;
    }
    /**
     * 程序的入口点，程序从这里开始运行至结束
     */
    public static void main(String[] args) {
        try {
            if (isThisVersionIsExpVersion())
            {
                logger.info("此版本为实验性版本！不会保证稳定性");
                logger.info("本版本存在一些正在开发中的内容，可能存在一些问题");
                logger.info("本版本测试性内容列表：");
                logger.info(getExpVersionText());
                logger.info("是否要启动此GUI？");
                logger.info("1为启动，其他为正常启动");
                Scanner sc = new Scanner(System.in);
                int mode = sc.nextInt();
                if (mode == 1)
                {
                    if (isGUIMode())
                    {
                        Application.launch(GUI.class, args);
                        return;
                    }
                    else
                    {
                        logger.info("此版本不允许GUI模式");
                    }
                }
            }
            logger.info("欢迎来到JavaIM！版本："+getVersion());
            logger.info("使用客户端模式请输入1，服务端模式请输入2:");
            Scanner sc = new Scanner(System.in);
            int mode = sc.nextInt();
            if (mode == 1) {
                Scanner sc2 = new Scanner(System.in);
                Scanner sc3 = new Scanner(System.in);
                String serverName;
                logger.info("请输入要连接的主机:");
                serverName = sc2.nextLine();
                logger.info("请输入端口:");
                int port = Integer.parseInt(sc3.nextLine());
                new Client(serverName, port);
            } else if (mode == 2) {
                Scanner sc4 = new Scanner(System.in);
                logger.info("请输入监听端口:");
                int port = Integer.parseInt(sc4.nextLine());
                new Server(port);
            } else {
                logger.info("输入值错误，请重新运行程序");
            }
        }
        catch (Exception e)
        {
            SaveStackTrace.saveStackTrace(e);
        }
    }
}
