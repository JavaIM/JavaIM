package org.yuezhikong.Server.plugin;

import org.yuezhikong.Server.Server;
import org.yuezhikong.Server.UserData.user;
import org.yuezhikong.Server.plugin.load.CustomClassLoader.PluginJavaLoader;
import org.yuezhikong.utils.CustomExceptions.ModeDisabledException;
import org.yuezhikong.utils.CustomVar;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * 所有插件都应当继承此class
 * <p>否则，将无法被加载</p>
 * @author AlexLiuDev233
 * @Date 2023/05/08
 */
@SuppressWarnings("unused")
public abstract class Plugin{
    private final List<CustomVar.CommandInformation> ThisPluginRegisteredCommand = new ArrayList<>();
    private CustomVar.PluginInformation Information;

    /**
     * 插件基本信息注册
     * @param name 插件名字
     * @param author 插件作者
     * @param version 插件版本
     * @apiNote 请注意，所有项目都必须填写，如果字符串为空也会加载失败哦
     * @return true成功，false不成功，false一般为已注册了
     */
    protected final boolean RegisterPlugin(String name,String author, String version)
    {
        if (Information == null || !Information.Registered()) {
            Information = new CustomVar.PluginInformation(name, author, version, this, true);
            return true;
        }
        return false;
    }
    /**
     * 插件取消注册
     */
    public final void UnRegisterPlugin(Server serverInstance)
    {
        OnUnLoad(serverInstance);
        //开始清理资源
        //删除所有注册的命令
        for (CustomVar.CommandInformation CommandInformation : ThisPluginRegisteredCommand)
        {
            serverInstance.PluginSetCommands.remove(CommandInformation);
        }
        //释放插件列表
        try {
            PluginManager.getInstanceOrNull().PluginList.removeIf(this::equals);
        } catch (ModeDisabledException ignored) {
        }
        //释放插件Information
        Information = null;
        //插件数量 -1
        try {
            PluginManager.getInstanceOrNull().NumberOfPlugins = PluginManager.getInstanceOrNull().NumberOfPlugins - 1;
        } catch (ModeDisabledException ignored) {
        }
        //释放classloader
        try {
            for (PluginJavaLoader loader : PluginManager.getInstanceOrNull().classLoaderList)
            {
                if (this.equals(loader.getPlugin()))
                {
                    PluginManager.getInstanceOrNull().classLoaderList.remove(loader);
                    try {
                        loader.close();
                    } catch (IOException ignored) {
                    }
                }
            }

        } catch (ModeDisabledException ignored) {
        }
    }
    /**
     * 获取插件基本信息
     * @return 插件基本信息
     */
    public final CustomVar.PluginInformation getInformation() {
        return Information;
    }

    /**
     * 注册命令
     * @param Command 命令名
     * @param Help 帮助
     * @param ServerInstance 服务端实例
     * @return false失败true成功
     */
    protected final boolean RegisterCommand(String Command,String Help,Server ServerInstance)
    {
        for (CustomVar.CommandInformation CommandInformation : ServerInstance.PluginSetCommands)
        {
            if (CommandInformation.Command().equals(Command))
            {
                return false;
            }
        }
        CustomVar.CommandInformation commandInformation = new CustomVar.CommandInformation(Command,Help,this);
        ThisPluginRegisteredCommand.add(commandInformation);
        ServerInstance.PluginSetCommands.add(commandInformation);
        return true;
    }

    /**
     * 取消注册一个本插件注册的命令
     * @param Command 命令
     * @param ServerInstance 服务端实例
     * @return false失败true成功
     */
    protected final boolean UnRegisterCommand(String Command,Server ServerInstance)
    {
        for (CustomVar.CommandInformation CommandInformation : ThisPluginRegisteredCommand)
        {
            if (CommandInformation.Command().equals(Command))
            {
                ThisPluginRegisteredCommand.remove(CommandInformation);
                ServerInstance.PluginSetCommands.remove(CommandInformation);
                return true;
            }
        }
        return false;
    }


    /**
     * 插件的入口点，在服务端启动时调用
     * @param ServerInstance 服务端实例
     */
    public abstract void OnLoad(Server ServerInstance);

    /**
     * 当插件被卸载时
     * 请注意，当程序被强制退出，将不会触发！
     * 建议I/O操作每次执行都重新打开Stream，而不是被保存着，且每次执行完后都进行销毁
     * @param ServerInstance 服务端实例
     */
    protected abstract void OnUnLoad(Server ServerInstance);

    /**
     * 当发生聊天时
     * 如果要监听此消息，请重写本方法
     * @param RequestUser 发生此事件的用户
     * @param Message 聊天消息
     * @param ServerInstance 服务端实例
     * @return 如果返回true，则代表将会阻止此事件，如果返回false，则代表不会阻止此事件
     */
    public boolean OnChat(user RequestUser, String Message, Server ServerInstance)
    {
        return false;
    }

    /**
     * 此插件自定义命令的处理程序
     * <p>请注意，所有注册的命令都会调用此方法</p>
     * <p>请根据命令名判断指令</p>
     * @param Command 命令
     * @param argv 参数
     * @param RequestUser 用户（服务端模式时为一个被标记为服务端，用户名为Server且权限为管理员的虚拟用户，此用户的socket等为null，但调用SendMessageToUser是可以打印的（有单独处理））
     */
    public void OnCommand(String Command,String[] argv,user RequestUser)
    {

    }

    /**
     * 当用户权限发生改变时
     * @param RequestUser 发生此事件的用户
     * @param NewPermissionLevel 将会被改变到的新权限级别
     * @param ServerInstance 服务端实例
     * @return 如果返回true，则代表将会阻止此事件，如果返回false，则代表不会阻止此事件
     */
    public boolean OnUserPermissionEdit(user RequestUser,int NewPermissionLevel,Server ServerInstance)
    {
        return false;
    }

    /**
     * 当用户被禁言时
     * @param RequestUser 发生此事件的用户
     * @param ServerInstance 服务端实例
     * @return 如果返回true，则代表将会阻止此事件，如果返回false，则代表不会阻止此事件
     */
    public boolean OnUserMuted(user RequestUser,Server ServerInstance)
    {
        return false;
    }

    /**
     * 当用户被解除禁言时
     * @param RequestUser 发生此事件的用户
     * @param ServerInstance 服务端实例
     * @return 如果返回true，则代表将会阻止此事件，如果返回false，则代表不会阻止此事件
     */
    public boolean OnUserUnMuted(user RequestUser,Server ServerInstance)
    {
        return false;
    }
}
