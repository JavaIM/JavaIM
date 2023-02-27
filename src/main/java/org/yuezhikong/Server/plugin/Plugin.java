package org.yuezhikong.Server.plugin;

import org.yuezhikong.Server.UserData.user;
import org.yuezhikong.Server.api.UnMuteType;

/**
 * 所有插件都应当实现本接口！
 * 建议插件不要直接调用内部class，而是使用API进行修改
 * 目前API仍不完善，后续会添加API
 * @author AlexLiuDev233
 * @Date 2023/02/27
 */
public interface Plugin {
    /**
     * 插件的入口点，请继承本class后通过@Override注解重写本方法！
     * 否则，您的插件将不会发生任何行为！
     */
    void OnLoad();

    /**
     * 当插件被卸载时
     * 请注意，当程序被强制退出，将不会触发！
     * 建议I/O操作每次执行都重新打开Stream，而不是被保存着，且每次执行完后都进行销毁
     */
    void OnUnLoad();

    /**
     * 当发生聊天时
     * 如果要监听此消息，请重写本方法
     * @param RequestUser 发生此事件的用户
     * @return 如果返回true，则代表将会阻止此事件，如果返回false，则代表不会阻止此事件
     */
    boolean OnChat(user RequestUser,String Message);

    /**
     * 当用户权限发生改变时
     * @param RequestUser 发生此事件的用户
     * @param NewPermissionLevel 将会被改变到的新权限级别
     * @return 如果返回true，则代表将会阻止此事件，如果返回false，则代表不会阻止此事件
     */
    boolean OnUserPermissionEdit(user RequestUser,int NewPermissionLevel);

    /**
     * 当用户被禁言时
     * @param RequestUser 发生此事件的用户
     * @param MuteTime 被禁言的时长
     * @return 如果返回true，则代表将会阻止此事件，如果返回false，则代表不会阻止此事件
     */
    boolean OnUserMuted(user RequestUser,int MuteTime);

    /**
     * 当用户被解除禁言时
     * @param RequestUser 发生此事件的用户
     * @return 如果返回true，则代表将会阻止此事件，如果返回false，则代表不会阻止此事件
     */
    boolean OnUserUnMuted(user RequestUser, UnMuteType unMuteType);
}
