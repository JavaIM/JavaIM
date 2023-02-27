package org.yuezhikong.Server.plugin;

import org.yuezhikong.Server.UserData.user;
import org.yuezhikong.Server.api.UnMuteType;
import org.yuezhikong.utils.SaveStackTrace;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Properties;

/**
 * 插件管理器
 * @author AlexLiuDev233
 * @Date 2023/02/27
 * @apiNote 测试性class
 */
public class PluginManager {
    private final List<Plugin> PluginList = new ArrayList<>();
    private int NumberOfPlugins = 0;
    private static PluginManager Instance;

    public static PluginManager getInstance(String DirName) {
        if (Instance == null)
        {
            Instance = new PluginManager(DirName);
        }
        return Instance;
    }

    /**
     * 获取文件夹下所有.jar结尾的文件
     * 一般是插件文件
     * @param DirName 文件夹路径
     * @return 文件列表
     */
    private List<File> GetPluginFileList(String DirName)
    {
        File file = new File(DirName);
        if (file.isDirectory())
        {
            String[] list = file.list();
            if (list == null)
            {
                return null;
            }
            List<File> PluginList = new ArrayList<>();
            for (String s : list) {
                if (s.toLowerCase(Locale.ROOT).endsWith(".jar")) {
                    File file1 = new File(DirName + "\\" + s);
                    PluginList.add(file1);
                }
            }
            return PluginList;
        }
        else
            return null;
    }

    /**
     * 加载一个文件夹下的所有插件
     * @param DirName 文件夹路径
     */
    public PluginManager(String DirName)
    {
        if (!new File(DirName).exists())
        {
            try {
                if (!new File(DirName).mkdir()) {
                    org.yuezhikong.utils.Logger logger = new org.yuezhikong.utils.Logger();
                    logger.error("无法新建文件夹" + DirName + "，可能是由于权限问题");
                }
            }
            catch (Exception e)
            {
                org.yuezhikong.utils.Logger logger = new org.yuezhikong.utils.Logger();
                logger.error("无法新建文件夹"+DirName+"，可能是由于权限问题");
                org.yuezhikong.utils.SaveStackTrace.saveStackTrace(e);
            }
        }
        List<File> PluginFileList = GetPluginFileList(DirName);
        if (PluginFileList == null)
        {
            return;
        }
        for (File s : PluginFileList) {
            try {
                URL JarURL = s.toURI().toURL();
                URLClassLoader classLoader = new URLClassLoader(new URL[]{JarURL}, null);
                try {
                    InputStream propertiesStream = classLoader.getResourceAsStream("PluginManifest.properties");
                    Properties properties = new Properties();
                    properties.load(propertiesStream);
                    String mainClass = properties.getProperty("Main-Class");
                    Class<?> clazz = classLoader.loadClass(mainClass);
                    if (Plugin.class.isAssignableFrom(clazz))
                    {
                        Plugin ThePlugin = Plugin.class.cast(clazz);
                        ThePlugin.OnLoad();
                        PluginList.add(ThePlugin);
                        NumberOfPlugins = NumberOfPlugins + 1;
                    }
                    else
                    {
                        org.yuezhikong.utils.Logger logger = new org.yuezhikong.utils.Logger();
                        logger.error("加载插件文件"+s.getName()+"失败！请检查此插件！");
                        logger.error("发生此错误是由于此插件主类未继承标准插件类，请联系此插件开发者！");
                        logger.error("本报错来源于检查！");
                    }
                }
                catch (ClassNotFoundException | IOException | NullPointerException e)
                {
                    org.yuezhikong.utils.Logger logger = new org.yuezhikong.utils.Logger();
                    logger.error("加载插件文件"+s.getName()+"失败！请检查此插件！");
                    logger.error("发生此错误可能的原因是");
                    logger.error("1：插件内没有清单文件PluginManifest.properties");
                    logger.error("2：输入流InputStream异常，一般如果是此原因，是您文件权限导致的");
                    logger.error("3：插件内清单文件注册的主类无效");
                    logger.error("具体原因请看logs/debug.log内内容，看最近信息，如果是");
                    logger.error("ClassNotFoundException则代表主类无效");
                    logger.error("IOException代表输入流InputStream异常");
                    logger.error("NullPointerException代表无清单文件或输入流InputStream无效");
                    logger.error("当前原因为："+e.getClass().getName()+" "+e.getMessage());
                    logger.error("请自行分辨原因");
                    SaveStackTrace.saveStackTrace(e);
                }
            } catch (MalformedURLException e) {
                org.yuezhikong.utils.Logger logger = new org.yuezhikong.utils.Logger();
                logger.error("插件加载系统出现异常MalformedURLException，无法继续加载插件，请联系开发者进行调试");
                logger.error("联系开发者时，请附带logs下的latest.log和debug.log");
                SaveStackTrace.saveStackTrace(e);
                return;
            }
        }
    }

    /**
     * 慎用！
     * 执行此方法，会导致程序退出！请保证他在程序的退出流程最后
     * @param ProgramExitCode 程序退出时的代码
     */
    public void OnProgramExit(int ProgramExitCode)
    {
        if (NumberOfPlugins == 0)
        {
            System.exit(ProgramExitCode);
        }
        for (Plugin s : PluginList) {
            s.OnUnLoad();
        }
        System.exit(ProgramExitCode);
    }

    /**
     * 用于调用插件事件处理程序
     * @param ChatUser 用户信息
     * @param Message 消息
     * @return true为阻止消息，false为正常操作
     */
    public boolean OnUserChat(user ChatUser,String Message)
    {
        boolean Block = false;
        if (NumberOfPlugins == 0)
        {
            return false;
        }
        for (Plugin s : PluginList) {
            if (s.OnChat(ChatUser,Message))
            {
                Block = true;
            }
        }
        return Block;
    }

    /**
     * 解除禁言时调用
     * @param UnMuteUser 被解除禁言的用户
     * @param unMuteType 解除禁言的类型
     * @return 是否取消
     */
    public boolean OnUserUnMute(user UnMuteUser, UnMuteType unMuteType)
    {
        boolean Block = false;
        if (NumberOfPlugins == 0)
        {
            return false;
        }
        for (Plugin s : PluginList) {
            if (s.OnUserUnMuted(UnMuteUser,unMuteType))
            {
                Block = true;
            }
        }
        return Block;
    }
}
