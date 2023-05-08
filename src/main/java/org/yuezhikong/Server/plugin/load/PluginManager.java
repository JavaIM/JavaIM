package org.yuezhikong.Server.plugin.load;

import org.yuezhikong.CodeDynamicConfig;
import org.yuezhikong.Server.Server;
import org.yuezhikong.Server.UserData.user;
import org.yuezhikong.Server.plugin.Plugin;
import org.yuezhikong.Server.plugin.load.CustomClassLoader.PluginJavaLoader;
import org.yuezhikong.utils.CustomExceptions.ModeDisabledException;
import org.yuezhikong.utils.SaveStackTrace;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * 插件管理器
 * @author AlexLiuDev233
 * @Date 2023/02/27
 * @apiNote 测试性class
 */
public class PluginManager {
    private final List<Plugin> PluginList = new ArrayList<>();
    private final List<PluginJavaLoader> classLoaderList = new ArrayList<>();
    private int NumberOfPlugins = 0;
    private static PluginManager Instance;

    /**
     * 获取插件列表
     * @return 插件列表
     */
    public List<Plugin> getPluginList() {
        return PluginList;
    }

    /**
     * 获取插件管理器实例
     * @param DirName 如果创建新实例，那么插件文件夹的位置在哪里
     * @return 插件管理器实例
     * @throws ModeDisabledException 插件系统已经被禁用了
     */
    public static PluginManager getInstance(String DirName) throws ModeDisabledException {
        if (CodeDynamicConfig.GetPluginSystemMode())
        {
            if (Instance == null)
            {
                Instance = new PluginManager(DirName);
            }
            return Instance;
        }
        else {
            throw new ModeDisabledException("Error! Plugin System Is Disabled!");
        }
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
    private PluginManager(String DirName)
    {
        if (!(new File(DirName).exists()))
        {
            try {
                if (!(new File(DirName).mkdir())) {
                    org.yuezhikong.utils.Logger logger = Server.GetInstance().logger;
                    logger.error("无法新建文件夹" + DirName + "，可能是由于权限问题");
                }
            }
            catch (Exception e)
            {
                org.yuezhikong.utils.Logger logger = Server.GetInstance().logger;
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
            PluginJavaLoader classLoader = null;
            try {
                classLoader = new PluginJavaLoader(ClassLoader.getSystemClassLoader(),s);
                if (classLoader.ThisPlugin.getInformation().PluginName().isEmpty() || classLoader.ThisPlugin.getInformation().plugin() == null || !classLoader.ThisPlugin.getInformation().Registered() || classLoader.ThisPlugin.getInformation().PluginAuthor().isEmpty() || classLoader.ThisPlugin.getInformation().PluginVersion().isEmpty())
                {
                    Server.GetInstance().logger.info("加载插件文件"+s.getName()+"失败！原因，未按照要求填写基本信息");
                    throw new Exception("Run ClassLoader Close");
                }
                else
                {
                    classLoaderList.add(classLoader);
                    classLoader.ThisPlugin.OnLoad(Server.GetInstance());
                    PluginList.add(classLoader.ThisPlugin);
                    NumberOfPlugins = NumberOfPlugins + 1;
                }
            }
            catch (Throwable e)
            {
                if (classLoader != null)
                {
                    try {
                        classLoader.close();
                    } catch (IOException ex) {
                        SaveStackTrace.saveStackTrace(e);
                    }
                }
                org.yuezhikong.utils.Logger logger = Server.GetInstance().logger;
                logger.error("加载插件文件"+s.getName()+"失败！请检查此插件！");
                SaveStackTrace.saveStackTrace(e);
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
        if (NumberOfPlugins <= 0)
        {
            System.exit(ProgramExitCode);
        }
        else {
            for (Plugin plugin : PluginList) {
                plugin.OnUnLoad(Server.GetInstance());
            }
            for (PluginJavaLoader ClassLoader : classLoaderList) {
                try {
                    ClassLoader.close();
                } catch (IOException e) {
                    SaveStackTrace.saveStackTrace(e);
                }
            }
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
        for (Plugin plugin : PluginList) {
            if (plugin.OnChat(ChatUser,Message,Server.GetInstance()))
            {
                Block = true;
            }
        }
        return Block;
    }

    /**
     * 解除禁言时调用
     * @param UnMuteUser 被解除禁言的用户
     * @return 是否取消
     */
    public boolean OnUserUnMute(user UnMuteUser)
    {
        boolean Block = false;
        if (NumberOfPlugins == 0)
        {
            return false;
        }
        for (Plugin plugin : PluginList) {
            if (plugin.OnUserUnMuted(UnMuteUser,Server.GetInstance()))
            {
                Block = true;
            }
        }
        return Block;
    }

    /**
     * 发生权限更改时调用
     * @param PermissionChangeUser 被修改权限的用户
     * @return 是否取消
     */
    public boolean OnUserPermissionChange(user PermissionChangeUser,int NewPermissionLevel)
    {
        boolean Block = false;
        if (NumberOfPlugins == 0)
        {
            return false;
        }
        for (Plugin plugin : PluginList) {
            if (plugin.OnUserPermissionEdit(PermissionChangeUser,NewPermissionLevel,Server.GetInstance()))
            {
                Block = true;
            }
        }
        return Block;
    }

    /**
     * 禁言时调用
     * @param MuteUser 被禁言的用户
     * @return 是否取消
     */
    public boolean OnUserMute(user MuteUser)
    {
        boolean Block = false;
        if (NumberOfPlugins == 0)
        {
            return false;
        }
        for (Plugin plugin : PluginList) {
            if (plugin.OnUserMuted(MuteUser,Server.GetInstance()))
            {
                Block = true;
            }
        }
        return Block;
    }
}
