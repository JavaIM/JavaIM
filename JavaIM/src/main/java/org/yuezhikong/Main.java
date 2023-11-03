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

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.yuezhikong.newClient.ClientMain;
import org.yuezhikong.newServer.ServerMain;
import org.yuezhikong.utils.ConfigFileManager;
import org.yuezhikong.utils.Logger;
import org.yuezhikong.utils.Notice;
import org.yuezhikong.utils.SaveStackTrace;

import java.io.File;
import java.io.PrintStream;
import java.util.Scanner;

import static org.yuezhikong.CodeDynamicConfig.*;

public class Main {
    private static final org.yuezhikong.utils.Logger logger = new org.yuezhikong.utils.Logger(null);

    public static void CreateServerProperties(){
        ConfigFileManager prop = new ConfigFileManager();
        prop.CreateServerprop();
    }
    public static void CreateClientProperties(){
        ConfigFileManager prop = new ConfigFileManager();
        prop.CreateClientprop();
    }

    /**
     * 重定向System.out与System.err
     */
    private static void stdoutRedistribution()
    {
        Logger stdOut = new Logger(true);
        System.setOut(new PrintStream(System.out)
        {
            @Override
            public void print(boolean b) {
                stdOut.info(String.valueOf(b));
            }

            @Override
            public void print(char c) {
                stdOut.info(String.valueOf(c));
            }

            @Override
            public void print(int i) {
                stdOut.info(String.valueOf(i));
            }

            @Override
            public void print(long l) {
                stdOut.info(String.valueOf(l));
            }

            @Override
            public void print(float f) {
                stdOut.info(String.valueOf(f));
            }

            @Override
            public void print(double d) {
                stdOut.info(String.valueOf(d));
            }

            @Override
            public void print(char @NotNull [] s) {
                stdOut.info(new String(s));
            }

            @Override
            public void print(@Nullable String s) {
                stdOut.info(s);
            }

            @Override
            public void print(@Nullable Object obj) {
                stdOut.info(String.valueOf(obj));
            }
            @Override
            public void println(boolean b) {
                stdOut.info(String.valueOf(b));
            }

            @Override
            public void println(char c) {
                stdOut.info(String.valueOf(c));
            }

            @Override
            public void println(int i) {
                stdOut.info(String.valueOf(i));
            }

            @Override
            public void println(long l) {
                stdOut.info(String.valueOf(l));
            }

            @Override
            public void println(float f) {
                stdOut.info(String.valueOf(f));
            }

            @Override
            public void println(double d) {
                stdOut.info(String.valueOf(d));
            }

            @Override
            public void println(char @NotNull [] s) {
                stdOut.info(new String(s));
            }

            @Override
            public void println(@Nullable String s) {
                stdOut.info(s);
            }

            @Override
            public void println(@Nullable Object obj) {
                stdOut.info(String.valueOf(obj));
            }
        });
        System.setErr(new PrintStream(System.err)
        {
            @Override
            public void print(boolean b) {
                stdOut.error(String.valueOf(b));
            }

            @Override
            public void print(char c) {
                stdOut.error(String.valueOf(c));
            }

            @Override
            public void print(int i) {
                stdOut.error(String.valueOf(i));
            }

            @Override
            public void print(long l) {
                stdOut.error(String.valueOf(l));
            }

            @Override
            public void print(float f) {
                stdOut.error(String.valueOf(f));
            }

            @Override
            public void print(double d) {
                stdOut.error(String.valueOf(d));
            }

            @Override
            public void print(char @NotNull [] s) {
                stdOut.error(new String(s));
            }

            @Override
            public void print(@Nullable String s) {
                stdOut.error(s);
            }

            @Override
            public void print(@Nullable Object obj) {
                stdOut.error(String.valueOf(obj));
            }
            @Override
            public void println(boolean b) {
                stdOut.error(String.valueOf(b));
            }

            @Override
            public void println(char c) {
                stdOut.error(String.valueOf(c));
            }

            @Override
            public void println(int i) {
                stdOut.error(String.valueOf(i));
            }

            @Override
            public void println(long l) {
                stdOut.error(String.valueOf(l));
            }

            @Override
            public void println(float f) {
                stdOut.error(String.valueOf(f));
            }

            @Override
            public void println(double d) {
                stdOut.error(String.valueOf(d));
            }

            @Override
            public void println(char @NotNull [] s) {
                stdOut.error(new String(s));
            }

            @Override
            public void println(@Nullable String s) {
                stdOut.error(s);
            }

            @Override
            public void println(@Nullable Object obj) {
                stdOut.error(String.valueOf(obj));
            }
        });

    }
    public static void ConsoleMain()
    {
        logger.info("欢迎来到JavaIM！版本："+getVersion());
        logger.info("使用客户端模式请输入1，服务端模式请输入2:");
        logger.info("请输入想选择的模式");
        logger.info("1:服务端");
        logger.info("2:客户端");
        Scanner scanner = new Scanner(System.in);
        int UserInput = scanner.nextInt();
        if (UserInput == 1)
        {
            logger.info("请输入绑定的端口");
            ThreadGroup ServerGroup = new ThreadGroup(Thread.currentThread().getThreadGroup(),"ServerGroup");
            try {
                new Thread(ServerGroup,"Server Thread")
                {
                    @Override
                    public void run() {
                        this.setUncaughtExceptionHandler(CrashReport.getCrashReport());
                        new ServerMain().start(scanner.nextInt());
                    }
                    public Thread start2()
                    {
                        start();
                        return this;
                    }
                }.start2().join();
            } catch (InterruptedException e) {
                SaveStackTrace.saveStackTrace(e);
            }
            System.exit(0);
        }
        else if (UserInput == 2)
        {
            logger.info("请输入ip");
            scanner.nextLine();
            String Address = scanner.nextLine();
            logger.info("请输入端口");
            new ClientMain().start(Address,scanner.nextInt());
            System.exit(0);
        }
    }
    /**
     * 程序的入口点，程序从这里开始运行至结束
     */
    public static void main(String[] args) {
        new Notice();
        stdoutRedistribution();
        Thread.currentThread().setUncaughtExceptionHandler(CrashReport.getCrashReport());
        //服务端与客户端配置文件初始化
        if (!(new File("server.properties").exists())){
            logger.info("目录下没有检测到服务端配置文件，正在创建");
            CreateServerProperties();
        }
        if (!(new File("client.properties").exists())){
            logger.info("目录下没有检测到客户端配置文件，正在创建");
            CreateClientProperties();
        }
        //命令行参数处理
        ConsoleCommandRequest.Request(true,args);
        //启动JavaIM启动逻辑
        if (isThisVersionIsExpVersion())
        {
            logger.info("欢迎来到JavaIM！版本："+getVersion());
            logger.info("此版本为实验性版本！不会保证稳定性");
            logger.info("本版本存在一些正在开发中的内容，可能存在一些问题");
            logger.info("本版本测试性内容：");
            logger.info(getExpVersionText());
            ExpVersionCode code = new ExpVersionCode();
            code.run(logger);
        }
        else if (isGUIMode())
        {
            logger.info("GUI功能由于服务端与客户端底层重构，导致已被暂时关闭");
            logger.info("正在为您使用控制台版本");
            ConsoleMain();
        }
        else
            ConsoleMain();
    }
}
