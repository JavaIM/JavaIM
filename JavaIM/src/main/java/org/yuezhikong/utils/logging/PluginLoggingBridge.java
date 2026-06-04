package org.yuezhikong.utils.logging;

import org.yuezhikong.Server.plugin.plugin.Plugin;
import org.yuezhikong.utils.checks;

import java.util.concurrent.ConcurrentHashMap;

public class PluginLoggingBridge {
    private PluginLoggingBridge() {
    }

    // 插件logger映射
    private static final ConcurrentHashMap<Plugin, Logger> pluginLoggerPair = new ConcurrentHashMap<>();

    /**
     * 注册logger
     *
     * @param plugin 插件
     * @param logger logger
     * @throws IllegalStateException 当插件已经有一个logger处理器时
     * @apiNote 一个插件只能有一个logger处理器！
     */
    @SuppressWarnings("unused")
    public static void registerLogger(Plugin plugin, Logger logger) {
        checks.checkState(pluginLoggerPair.get(plugin) != null, "Plugin already has a logger!");
        pluginLoggerPair.put(plugin, logger);
        JavaIMLogger.addLogger(logger);
    }

    /**
     * 移除logger
     *
     * @param plugin 插件
     * @throws IllegalStateException 当插件没有logger处理器时
     */
    public static void unregisterLogger(Plugin plugin) {
        checks.checkState(pluginLoggerPair.get(plugin) == null, "Plugin doesn't have a logger!");
        Logger value = pluginLoggerPair.remove(plugin);
        JavaIMLogger.removeLogger(value);
    }

    /**
     * 重置
     */
    public static void reset() {
        pluginLoggerPair.clear();
        JavaIMLogger.clearLoggers();
    }
}
