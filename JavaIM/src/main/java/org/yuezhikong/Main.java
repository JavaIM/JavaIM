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

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yuezhikong.Server.Server;
import org.yuezhikong.utils.ConfigFileManager;
import org.yuezhikong.utils.Notice;
import org.yuezhikong.utils.SaveStackTrace;

import java.io.File;
import java.io.PrintStream;
import java.security.Security;
import java.util.Scanner;

import static org.yuezhikong.CodeDynamicConfig.getVersion;

public class Main {
    static {
        // 初始化Slf4j Service Provider
        System.setProperty(LoggerFactory.PROVIDER_PROPERTY_KEY,"org.yuezhikong.utils.logging.SLF4JServiceProvider");
        // 暂时禁止sysOut与sysErr
        PrintStream err = System.err;
        PrintStream out = System.out;
        System.setOut(new PrintStream(System.out)
        {
            @Override
            public void println(@Nullable String x) {
                return;
            }
        });
        System.setErr(new PrintStream(System.err)
        {
            @Override
            public void println(@Nullable String x) {
            }
        });
        // 初始化 STDOUT Logger
        logger = LoggerFactory.getLogger(Main.class);
        // 恢复sysOut与sysErr
        System.setOut(out);
        System.setErr(err);
    }
    private static final Logger logger;

    public static void CreateServerProperties(){
        ConfigFileManager prop = new ConfigFileManager();
        prop.CreateServerprop();
    }

    /**
     * 重定向System.out与System.err
     */
    private static void stdoutRedistribution()
    {
        System.setOut(new PrintStream(System.out)
        {
            @Override
            public void print(boolean b) {
                logger.info("[STDOUT] {}", b);
            }

            @Override
            public void print(char c) {
                logger.info("[STDOUT] {}", c);
            }

            @Override
            public void print(int i) {
                logger.info("[STDOUT] {}", i);
            }

            @Override
            public void print(long l) {
                logger.info("[STDOUT] {}", l);
            }

            @Override
            public void print(float f) {
                logger.info("[STDOUT] {}", f);
            }

            @Override
            public void print(double d) {
                logger.info("[STDOUT] {}", d);
            }

            @Override
            public void print(char @NotNull [] s) {
                logger.info("[STDOUT] {}", new String(s));
            }

            @Override
            public void print(@Nullable String s) {
                logger.info("[STDOUT] {}", s);
            }

            @Override
            public void print(@Nullable Object obj) {
                logger.info("[STDOUT] {}", obj);
            }
            @Override
            public void println(boolean b) {
                logger.info("[STDOUT] {}", b);
            }

            @Override
            public void println(char c) {
                logger.info("[STDOUT] {}", c);
            }

            @Override
            public void println(int i) {
                logger.info("[STDOUT] {}", i);
            }

            @Override
            public void println(long l) {
                logger.info("[STDOUT] {}", l);
            }

            @Override
            public void println(float f) {
                logger.info("[STDOUT] {}", f);
            }

            @Override
            public void println(double d) {
                logger.info("[STDOUT] {}", d);
            }

            @Override
            public void println(char @NotNull [] s) {
                logger.info("[STDOUT] {}", new String(s));
            }

            @Override
            public void println(@Nullable String s) {
                logger.info("[STDOUT] {}", s);
            }

            @Override
            public void println(@Nullable Object obj) {
                logger.info("[STDOUT] {}", obj);
            }
        });
        System.setErr(new PrintStream(System.err)
        {
            @Override
            public void print(boolean b) {
                logger.error("[STDERR] {}", b);
            }

            @Override
            public void print(char c) {
                logger.error("[STDERR] {}", c);
            }

            @Override
            public void print(int i) {
                logger.error("[STDERR] {}", i);
            }

            @Override
            public void print(long l) {
                logger.error("[STDERR] {}", l);
            }

            @Override
            public void print(float f) {
                logger.error("[STDERR] {}", f);
            }

            @Override
            public void print(double d) {
                logger.error("[STDERR] {}", d);
            }

            @Override
            public void print(char @NotNull [] s) {
                logger.error("[STDERR] {}", new String(s));
            }

            @Override
            public void print(@Nullable String s) {
                logger.error("[STDERR] {}", s);
            }

            @Override
            public void print(@Nullable Object obj) {
                logger.error("[STDERR] {}", obj);
            }
            @Override
            public void println(boolean b) {
                logger.error("[STDERR] {}", b);
            }

            @Override
            public void println(char c) {
                logger.error("[STDERR] {}", c);
            }

            @Override
            public void println(int i) {
                logger.error("[STDERR] {}", i);
            }

            @Override
            public void println(long l) {
                logger.error("[STDERR] {}", l);
            }

            @Override
            public void println(float f) {
                logger.error("[STDERR] {}", f);
            }

            @Override
            public void println(double d) {
                logger.error("[STDERR] {}", d);
            }

            @Override
            public void println(char @NotNull [] s) {
                logger.error("[STDERR] {}", new String(s));
            }

            @Override
            public void println(@Nullable String s) {
                logger.error("[STDERR] {}", s);
            }

            @Override
            public void println(@Nullable Object obj) {
                logger.error("[STDERR] {}", obj);
            }
        });

    }
    public static void ConsoleMain()
    {
        logger.info("欢迎来到JavaIM！版本："+getVersion());
        logger.info("正在启动服务端...");

        Scanner scanner = new Scanner(System.in);
        logger.info("请输入绑定的端口");
        int ServerPort = scanner.nextInt();
        ThreadGroup ServerGroup = new ThreadGroup(Thread.currentThread().getThreadGroup(),"ServerGroup");
        try {
            new Thread(ServerGroup,"Server Thread")
            {
                @Override
                public void run() {
                    this.setUncaughtExceptionHandler(CrashReport.getCrashReport());
                    new Server().start(ServerPort);
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
    /**
     * 程序的入口点，程序从这里开始运行至结束
     */
    public static void main(String[] args) {
        new Notice();
        // 初始化BouncyCastle，设置为JCE Provider
        Security.addProvider(new BouncyCastleProvider());
        // 初始化Stdout重定向
        stdoutRedistribution();
        // 初始化主线程崩溃报告程序
        Thread.currentThread().setUncaughtExceptionHandler(CrashReport.getCrashReport());
        // 服务端配置文件初始化
        if (!(new File("server.properties").exists())){
            logger.info("目录下没有检测到服务端配置文件，正在创建");
            CreateServerProperties();
        }
        // 命令行参数处理
        ConsoleCommandRequest.Request(args);
        // 启动JavaIM启动逻辑
        ConsoleMain();
    }
}
