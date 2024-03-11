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
import org.yuezhikong.utils.Logger;

public class ConsoleCommandRequest {
    /**
     * 解析并处理控制台命令
     * @param AllowExit 是/否允许解析退出
     * @param args 命令行参数
     */
    public static void Request(boolean AllowExit, String @NotNull [] args)
    {
        for (String arg : args)
        {
            switch (arg) {
                case "-help" -> {
                    Logger logger = new Logger();
                    logger.info("-help 显示帮助");
                    logger.info("-nogui 无需gui");
                    logger.info("-usegui 需gui");
                    logger.info("命令行输入将会覆盖配置文件配置，并且顺序越靠后，优先级越高");
                    if (AllowExit) {
                        System.exit(0);
                    }
                }
                case "-nogui" -> CodeDynamicConfig.GUIMode = false;
                case "-usegui" -> CodeDynamicConfig.GUIMode = true;
                default -> {
                    Logger logger = new Logger();
                    logger.warning("警告，参数：" + arg + "与任何命令行输入不符");
                }
            }
        }
    }
}
