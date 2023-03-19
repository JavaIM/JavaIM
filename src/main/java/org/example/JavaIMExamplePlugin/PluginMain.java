package org.example.JavaIMExamplePlugin;

import org.yuezhikong.Server.Server;
import org.yuezhikong.Server.UserData.user;
import org.yuezhikong.Server.plugin.Plugin;
import org.yuezhikong.utils.Logger;

public class PluginMain implements Plugin {
    private final Logger logger = new Logger();
    /**
     * 当插件加载时
     * @param server 服务器实例
     */
    @Override
    public void OnLoad(Server server) {
        logger.info("插件加载成功！");
    }
    /**
     * 当插件卸载时
     * @param server 服务器实例
     */
    @Override
    public void OnUnLoad(Server server) {
        logger.info("插件卸载成功！");
    }

    /**
     * 当发生聊天事件时
     * 请注意，请不要调用父类的OnChat方法，那个方法将会直接返回false，无意义
     * @param RequestUser 发生此事件的用户
     * @param Message 用户的聊天信息
     * @param ServerInstance 服务器实例
     * @return true为阻止转发到其他客户端，false为允许转发
     * @apiNote 返回false不会影响其他插件返回true，返回true也不会阻止其他插件被调用OnChat
     */
    @Override
    public boolean OnChat(user RequestUser, String Message, Server ServerInstance) {
        logger.info(RequestUser.GetUserName()+"正在试图发言，他的聊天信息为："+Message);
        return false;
    }

    /**
     * 当用户的权限发生变化
     * 请注意，请不要调用父类的OnUserPermissionEdit方法，那个方法将会直接返回false，无意义
     * @param RequestUser 发生此事件的用户
     * @param NewPermissionLevel 他将要变化到的新权限等级
     * @param ServerInstance 服务器实例
     * @return true为阻止执行，false为允许执行
     * @apiNote 返回false不会影响其他插件返回true，返回true也不会阻止其他插件被调用OnUserPermissionEdit
     */
    @Override
    public boolean OnUserPermissionEdit(user RequestUser, int NewPermissionLevel, Server ServerInstance) {
        logger.info(RequestUser.GetUserName()+"正在被更改权限等级，他的原权限等级为："+RequestUser.GetUserPermission()+"，他的新权限等级为："+NewPermissionLevel);
        return false;
    }

    /**
     * 当用户被禁言
     * 请注意，请不要调用父类的OnUserMuted方法，那个方法将会直接返回false，无意义
     * @param RequestUser 发生此事件的用户
     * @param ServerInstance 服务器实例
     * @return true为阻止执行，false为允许执行
     * @apiNote 返回false不会影响其他插件返回true，返回true也不会阻止其他插件被调用OnUserMuted
     */
    @Override
    public boolean OnUserMuted(user RequestUser, Server ServerInstance) {
        logger.info(RequestUser.GetUserName()+"正在被禁言");
        return false;
    }

    /**
     * 当用户被解除禁言
     * 请注意，请不要调用父类的OnUserUnMuted方法，那个方法将会直接返回false，无意义
     * @param RequestUser 发生此事件的用户
     * @param ServerInstance 服务器实例
     * @return true为阻止执行，false为允许执行
     * @apiNote 返回false不会影响其他插件返回true，返回true也不会阻止其他插件被调用OnUserUnMuted
     */
    @Override
    public boolean OnUserUnMuted(user RequestUser, Server ServerInstance) {
        logger.info(RequestUser.GetUserName()+"正在被解除禁言");
        return false;
    }
}
