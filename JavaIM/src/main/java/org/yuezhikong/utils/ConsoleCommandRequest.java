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
package org.yuezhikong.utils;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.cli.*;
import org.yuezhikong.SystemConfig;

import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@Slf4j
public class ConsoleCommandRequest {
    /**
     * 解析并处理控制台命令
     *
     * @param args 命令行参数
     */
    // Map为控制台命令行的option与参数
    public static Map<String, String> commandLineRequest(String[] args) {
        // 命令行解析器
        CommandLineParser parser = new DefaultParser();
        // 命令行选项声明
        Options options = new Options();
        options.addOption("h", "help", false, "显示帮助信息");
        options.addOption("v", "version", false, "显示版本信息");
        options.addOption("p", "bindPort", true, "设置服务器绑定的端口");
        options.addOption("cu", "checkUpdate", true, "是否允许检查更新");
        options.addOption("iu", "installUpdate", true, "是否允许自动安装更新");
        options.addOption("ga", "githubAccessToken", true, "设置GitHub的Access Token");

        HashMap<String, String> commandLineMap = new HashMap<>();
        String mainFileName = new File(ConsoleCommandRequest.class.getProtectionDomain().getCodeSource().getLocation().getFile()).getName();
        try {
            CommandLine cmd = parser.parse(options, args);

            if (cmd.hasOption("h")) {
                // 显示帮助信息
                HelpFormatter formatter = new HelpFormatter();
                StringWriter sw = new StringWriter();
                PrintWriter pw = new PrintWriter(sw);
                formatter.printHelp(
                        pw,
                        formatter.getWidth(),
                        "java -jar " + mainFileName,
                        null,
                        options, formatter.getLeftPadding(),
                        formatter.getDescPadding(),
                        null,
                        false
                );
                pw.flush();
                log.info(sw.toString());
                pw.close();
                System.exit(0);
            }

            if (cmd.hasOption("v")) {
                log.info("JavaIM v{}", SystemConfig.getVersion());
                System.exit(0);
            }

            if (cmd.hasOption("p")) {
                String optionValue = cmd.getOptionValue("p");
                try {
                    Integer.parseInt(optionValue);
                } catch (NumberFormatException e) {
                    commandLineCheckFailed();
                }
                commandLineMap.put("bindPort", optionValue);
            }

            if (cmd.hasOption("cu")) {
                String optionValue = cmd.getOptionValue("cu");
                if (!optionValue.equals("true") && !optionValue.equals("false"))
                    commandLineCheckFailed();
                commandLineMap.put("checkUpdate", optionValue);
            }

            if (cmd.hasOption("ga")) {
                String optionValue = cmd.getOptionValue("ga");
                commandLineMap.put("githubAccessToken", optionValue);
            }

            if (cmd.hasOption("iu")) {
                String optionValue = cmd.getOptionValue("iu");
                if (!optionValue.equals("true") && !optionValue.equals("false"))
                    commandLineCheckFailed();
                commandLineMap.put("installUpdate", optionValue);
            }
        } catch (ParseException e) {
            commandLineCheckFailed();
        }
        return Collections.unmodifiableMap(commandLineMap);
    }

    private static void commandLineCheckFailed() {
        log.error("命令行选项无法被解析");
        log.error("请使用 -h 选项查看帮助信息");
        System.exit(2);
    }
}
