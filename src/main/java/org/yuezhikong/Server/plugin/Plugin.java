package org.yuezhikong.Server.plugin;

import org.yuezhikong.Server.Server;
import org.yuezhikong.Server.UserData.user;

/**
 * 插件父级interface
 * 建议插件不要直接调用内部class，而是使用API进行修改
 * 目前API仍不完善，后续会添加API
 * @author AlexLiuDev233
 * @Date 2023/02/27
 */
@SuppressWarnings("unused")
public interface Plugin {
    /**
     * 插件的入口点，请继承本class后通过@Override注解重写本方法！
     * 否则，您的插件将不会发生任何行为！
     * @param ServerInstance 服务端实例
     */
    void OnLoad(Server ServerInstance);

    /**
     * 当插件被卸载时
     * 请注意，当程序被强制退出，将不会触发！
     * 建议I/O操作每次执行都重新打开Stream，而不是被保存着，且每次执行完后都进行销毁
     * @param ServerInstance 服务端实例
     */
    void OnUnLoad(Server ServerInstance);

    /**
     * 当发生聊天时
     * 如果要监听此消息，请重写本方法
     * @param RequestUser 发生此事件的用户
     * @param Message 聊天消息
     * @param ServerInstance 服务端实例
     * @return 如果返回true，则代表将会阻止此事件，如果返回false，则代表不会阻止此事件
     */
    default boolean OnChat(user RequestUser, String Message, Server ServerInstance)
    {
        return false;
    }

    /**
     * 当用户权限发生改变时
     * @param RequestUser 发生此事件的用户
     * @param NewPermissionLevel 将会被改变到的新权限级别
     * @param ServerInstance 服务端实例
     * @return 如果返回true，则代表将会阻止此事件，如果返回false，则代表不会阻止此事件
     */
    default boolean OnUserPermissionEdit(user RequestUser,int NewPermissionLevel,Server ServerInstance)
    {
        return false;
    }

    /**
     * 当用户被禁言时
     * @param RequestUser 发生此事件的用户
     * @param ServerInstance 服务端实例
     * @return 如果返回true，则代表将会阻止此事件，如果返回false，则代表不会阻止此事件
     */
    default boolean OnUserMuted(user RequestUser,Server ServerInstance)
    {
        return false;
    }

    /**
     * 当用户被解除禁言时
     * @param RequestUser 发生此事件的用户
     * @param ServerInstance 服务端实例
     * @return 如果返回true，则代表将会阻止此事件，如果返回false，则代表不会阻止此事件
     */
    default boolean OnUserUnMuted(user RequestUser,Server ServerInstance)
    {
        return false;
    }
}
