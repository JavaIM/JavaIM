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

import lombok.Getter;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.jline.jansi.AnsiConsole;
import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.bridge.SLF4JBridgeHandler;
import org.yuezhikong.Server.Server;
import org.yuezhikong.Server.ServerTools;
import org.yuezhikong.utils.checkUpdate.CheckUpdate;
import org.yuezhikong.utils.ConfigFileManager;
import org.yuezhikong.utils.ConsoleCommandRequest;
import org.yuezhikong.utils.Notice;

import java.io.*;
import java.security.Security;
import java.util.Locale;
import java.util.Map;
import java.util.logging.Level;

public class Main {
    private final static Logger log;
    @Getter
    private final static Terminal terminal;
    static {
        System.out.println("正在初始化JavaIM...");
        // Slf4j Logger加载
        log = LoggerFactory.getLogger(Main.class);
        // 安装 JUL to slf4j
        SLF4JBridgeHandler.removeHandlersForRootLogger();
        SLF4JBridgeHandler.install();
        if (log.isTraceEnabled())
            java.util.logging.Logger.getLogger("").setLevel(Level.FINEST);
        // 初始化 JLine Terminal
        Terminal terminal1;
        try {
            if (System.console() != null) {
                AnsiConsole.systemInstall();
                terminal1 = AnsiConsole.getTerminal();
            }
            else
                terminal1 = TerminalBuilder.builder().system(true).exec(false).ffm(false).jna(false).dumb(true).build();
        } catch (IOException e) {
            terminal1 = null;
            AnsiConsole.sysErr().println("JavaIM 初始化失败");
            System.exit(1);
        }
        terminal = terminal1;
    }

    public static void ConsoleMain(Map<String,String> commandLineArgs, LineReader lineReader)
    {
        log.info("欢迎来到JavaIM！版本：{}", CodeDynamicConfig.getVersion());
        log.info("正在启动服务端...");

        int ServerPort;
        if (!commandLineArgs.containsKey("bindPort")) {
            log.info("请输入绑定的端口");
            ServerPort = Integer.parseInt(
                    lineReader.readLine(">")
            );
        } else
            ServerPort = Integer.parseInt(commandLineArgs.get("bindPort"));
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
            log.error("出现错误!",e);
        }
        System.exit(0);
    }
    /**
     * 程序的入口点，程序从这里开始运行至结束
     */
    public static void main(String[] args) {
        new Notice();
        // 初始化Shutdown Hook
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                if (ServerTools.getServerInstance() == null || !ServerTools.getServerInstance().isServerCompleateStart())
                    return;
                try {
                    ServerTools.getServerInstance().stop();
                } catch (IllegalStateException ignored) {
                }
            } catch (Throwable ignored) {}
        }));
        // 初始化BouncyCastle，设置为JCE Provider
        Security.addProvider(new BouncyCastleProvider());
        // 初始化主线程崩溃报告程序
        Thread.currentThread().setUncaughtExceptionHandler(CrashReport.getCrashReport());
        // 初始化 LineReader
        LineReader reader = LineReaderBuilder.builder().terminal(terminal).build();
        // 服务端配置文件初始化
        if (!(new File("server.properties").exists())){
            log.info("目录下没有检测到服务端配置文件，判断为第一次进入");
            ConfigFileManager.createServerConfig();
            firstStart(reader);
        } else
            ConfigFileManager.reloadServerConfig();
        // 命令行参数处理
        Map<String,String> commandLineArgs = ConsoleCommandRequest.commandLineRequest(args);
        // 自动更新
        boolean checkUpdate, installUpdate;
        if (!commandLineArgs.containsKey("checkUpdate")) {
            log.info("是否检查更新?(Y/N)");
            checkUpdate = "Y".equals(
                    reader.readLine(">").toUpperCase(Locale.ROOT)
            );
        } else {
            checkUpdate = Boolean.parseBoolean(commandLineArgs.get("checkUpdate"));
        }

        if (checkUpdate) {
            if (!commandLineArgs.containsKey("installUpdate")) {
                log.info("是否允许自动安装更新?(Y/N)");
                installUpdate = "Y".equals(
                        reader.readLine(">").toUpperCase(Locale.ROOT)
                );
            } else {
                installUpdate = Boolean.parseBoolean(commandLineArgs.get("installUpdate"));
            }

            CheckUpdate.checkUpdate(installUpdate, commandLineArgs.getOrDefault("githubAccessToken", ""));
        }
        // 启动JavaIM启动逻辑
        ConsoleMain(commandLineArgs, reader);
    }

    /**
     * 首次启动 JavaIM 时的向导
     * @param reader LineReader
     */
    private static void firstStart(LineReader reader) {
        log.info("检测到您疑似是首次启动 JavaIM, 是否进行配置?(Y/N)");
        if (!"Y".equals(reader.readLine(">").toUpperCase(Locale.ROOT)))
            return;
        log.info("请设置服务器名称");
        ConfigFileManager.setServerConfig("serverName",reader.readLine("服务器名称>"));
        log.info("是否使用sqlite(Y/N)");
        if ("Y".equals(reader.readLine("是否使用sqlite>").toUpperCase(Locale.ROOT))) {
            ConfigFileManager.setServerConfig("sqlite","true");
            ConfigFileManager.setServerConfig("mysqlHost","");
            ConfigFileManager.setServerConfig("mysqlPort","");
            ConfigFileManager.setServerConfig("mysqlDBName","");
            ConfigFileManager.setServerConfig("mysqlUser","");
            ConfigFileManager.setServerConfig("mysqlPasswd","");
            log.info("正在保存您的配置...");
            ConfigFileManager.rewriteServerConfig();
            log.info("设置向导成功完成!");
            return;
        }
        log.info("请设置 mysql 地址");
        ConfigFileManager.setServerConfig("mysqlHost",reader.readLine("mysql地址>"));
        log.info("请设置 mysql 端口");
        ConfigFileManager.setServerConfig("mysqlPort",reader.readLine("mysql端口>"));
        log.info("请设置 mysql 数据库名称");
        ConfigFileManager.setServerConfig("mysqlDBName",reader.readLine("mysql数据库名称>"));
        log.info("请设置 mysql 登录用户");
        ConfigFileManager.setServerConfig("mysqlUser",reader.readLine("mysql登录用户>"));
        log.info("请设置 mysql 登录密码");
        ConfigFileManager.setServerConfig("mysqlPasswd",reader.readLine("mysql登录密码>"));
        log.info("正在保存您的配置...");
        ConfigFileManager.rewriteServerConfig();
        log.info("设置向导成功完成!");
    }
}
