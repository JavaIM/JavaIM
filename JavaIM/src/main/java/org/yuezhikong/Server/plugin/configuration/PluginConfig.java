package org.yuezhikong.Server.plugin.configuration;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.Nullable;
import org.yuezhikong.Server.plugin.Plugin.Plugin;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

@Slf4j
@SuppressWarnings("unused")
public class PluginConfig {
    private PluginConfig() {}

    /**
     * 保存默认配置文件
     * @param plugin 插件实例
     */
    public static void saveDefaultConfiguration(Plugin plugin)
    {
        saveDefaultConfiguration(plugin,"config.properties");
    }

    /**
     * 保存默认配置文件
     * @param plugin 插件实例
     * @param FileName 文件名
     */
    public static void saveDefaultConfiguration(Plugin plugin, String FileName)
    {
        File PluginDirectory = new File("./plugins/"+plugin.getPluginData().getStaticData().PluginName());
        if (PluginDirectory.exists() && PluginDirectory.isFile())
        {
            if ((!PluginDirectory.delete()) || (!PluginDirectory.mkdirs()))
                return;
        }
        else if (!PluginDirectory.exists())
            if (!PluginDirectory.mkdirs())
                return;
        File ConfigurationFile = new File(PluginDirectory.getPath()+"/"+FileName);
        if (ConfigurationFile.exists())
            return;
        try {
            if (!ConfigurationFile.createNewFile())
                return;
            InputStream inputStream = plugin.getPluginData().getStaticData().PluginClassLoader().getResourceAsStream(FileName);
            if (inputStream == null)
            {
                return;
            }
            FileUtils.copyInputStreamToFile(inputStream,ConfigurationFile);

        } catch (IOException e) {
            log.error("出现错误!",e);
        }
    }

    /**
     * 强制保存配置文件
     * @param plugin 插件实例
     */
    public static void saveConfiguration(Plugin plugin)
    {
        saveConfiguration(plugin,"config.properties");
    }

    /**
     * 强制保存配置文件
     * @param plugin 插件实例
     * @param FileName 文件名
     */
    public static void saveConfiguration(Plugin plugin, String FileName)
    {
        File PluginDirectory = new File("./plugins/"+plugin.getPluginData().getStaticData().PluginName());
        if (PluginDirectory.exists() && PluginDirectory.isFile())
        {
            if ((!PluginDirectory.delete()) || (!PluginDirectory.mkdirs()))
            {
                return;
            }
        }
        else if (!PluginDirectory.exists())
            if (!PluginDirectory.mkdirs())
                return;
        File ConfigurationFile = new File(PluginDirectory.getPath()+"/"+FileName);
        try {
            if (!ConfigurationFile.createNewFile())
                return;
            InputStream inputStream = plugin.getPluginData().getStaticData().PluginClassLoader().getResourceAsStream(FileName);
            if (inputStream == null)
            {
                return;
            }
            FileUtils.copyInputStreamToFile(inputStream,ConfigurationFile);

        } catch (IOException e) {
            log.error("出现错误!",e);
        }
    }

    /**
     * 获取配置文件文件操作类
     * @param plugin 插件实例
     * @return 配置文件操作类
     */
    @Contract(pure = true,value = "null -> fail")
    public static @Nullable Properties getConfiguration(Plugin plugin)
    {
        return getConfiguration(plugin,"config.properties");
    }


    /**
     * 获取配置文件文件操作类
     * @param plugin 插件实例
     * @param FileName 文件名
     * @return 配置文件操作类
     */
    @Contract(pure = true,value = "null,null -> fail; null,_ -> fail; _,null -> fail")
    public static @Nullable Properties getConfiguration(Plugin plugin, String FileName)
    {
        File ConfigurationFile = new File("./plugins/"+plugin.getPluginData().getStaticData().PluginName()+"/"+FileName);
        if (!ConfigurationFile.exists())
            return null;
        if (!ConfigurationFile.isFile())
            return null;
        try {
            Properties properties = new Properties();
            properties.load(new FileInputStream(ConfigurationFile));
            return properties;
        } catch (IOException e) {
            log.error("出现错误!",e);
        }
        return null;
    }
}
