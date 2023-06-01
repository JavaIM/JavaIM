package org.yuezhikong;

import org.jetbrains.annotations.NotNull;
import org.yuezhikong.utils.Logger;

public class ConsoleCommandRequest {
    /**
     * 解析并处理控制台命令
     * @param AllowExit 是/否允许解析退出
     * @param args 命令行参数
     */
    public static void Request(boolean AllowExit, @NotNull String @NotNull [] args)
    {
        for (String arg : args)
        {
            switch (arg) {
                case "-help" -> {
                    Logger logger = new Logger(false, false, null, null);
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
                    Logger logger = new Logger(false, false, null, null);
                    logger.warning("警告，参数：" + arg + "与任何命令行输入不符");
                }
            }
        }
    }
}
