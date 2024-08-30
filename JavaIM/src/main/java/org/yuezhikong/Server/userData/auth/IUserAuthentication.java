package org.yuezhikong.Server.userData.auth;

import org.yuezhikong.Server.userData.user;

public interface IUserAuthentication {

    interface UserRecall {
        void run(user User);
    }

    /**
     * 尝试通过 Token 进行登录
     *
     * @param Token Token令牌
     * @return True为成功登录，False为登录失败
     */
    boolean doLogin(String Token);

    /**
     * 尝试通过 用户名与密码 进行登录
     *
     * @param UserName 用户名
     * @param Password 密码
     * @return True为成功登录，False为登录失败
     */
    boolean doLogin(String UserName, String Password);


    /**
     * 获取登录状态
     *
     * @return 是否登录
     */
    boolean isLogin();

    /**
     * 注册一个新的登录后回调
     *
     * @param runnable 回调函数
     * @apiNote 如果用户已经登录，则代码将会被立刻执行
     */
    void registerLoginRecall(UserRecall runnable);

    /**
     * 获取用户的用户名
     *
     * @return 用户名
     */
    String getUserName();

    /**
     * 注册一个新的退出登录回调
     *
     * @param runnable 回调函数
     * @apiNote 如果用户已经退出登录，则代码将会被立刻执行
     */
    void registerLogoutRecall(UserRecall runnable);

    /**
     * 将客户端退出登录
     *
     * @return True说明已经成功退出，False说明本身就未登录或已经退出登录
     */
    boolean doLogout();

    /**
     * 扩展安全性
     * @param totpCode TOTP一次性代码
     * @throws IllegalStateException 已经登录/未要求增强安全
     */
    void extendSecurity(String totpCode) throws IllegalStateException;
}
