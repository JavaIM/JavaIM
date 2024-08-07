package org.yuezhikong.Server.userData;

import org.apache.ibatis.session.SqlSession;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yuezhikong.Server.ServerTools;
import org.yuezhikong.Server.userData.auth.IUserAuthentication;
import org.yuezhikong.Server.plugin.event.events.User.auth.UserLoginEvent;
import org.yuezhikong.utils.database.dao.userInformationDao;

public abstract class JavaUser implements user {
    private userInformation userInformation;
    private IUserAuthentication authentication;
    private static final Logger logger = LoggerFactory.getLogger(JavaUser.class);

    @Override
    public String getUserName() {
        return (authentication == null) ? "" : authentication.getUserName();
    }

    @Override
    public user onUserLogin(String UserName) {
        UserLoginEvent event = new UserLoginEvent(UserName);
        ServerTools.getServerInstanceOrThrow().getPluginManager().callEvent(event);
        return this;
    }

    @Override
    public boolean isUserLogged() {
        return authentication != null && authentication.isLogin();
    }

    @Override
    public user UserDisconnect() {
        ServerTools.getServerInstanceOrThrow().UnRegisterUser(this);
        return this;
    }

    @Override
    public user SetUserPermission(int permissionLevel) {
        userInformation information = getUserInformation();
        information.setPermission(permissionLevel);
        setUserInformation(information);
        logger.info(String.format("用户：%s的权限发生更新，新权限等级：%s", getUserName(), permissionLevel));
        return this;
    }

    @Override
    public Permission getUserPermission() {
        return Permission.ToPermission(getUserInformation().getPermission());
    }

    @Override
    public user addLoginRecall(IUserAuthentication.UserRecall code) {
        authentication.RegisterLoginRecall(code);
        return this;
    }

    @Override
    public user addDisconnectRecall(IUserAuthentication.UserRecall code) {
        authentication.RegisterLogoutRecall(code);
        return this;
    }

    @Override
    public user setUserAuthentication(@Nullable IUserAuthentication Authentication) {
        authentication = Authentication;
        return this;
    }

    @Override
    public @Nullable IUserAuthentication getUserAuthentication() {
        return authentication;
    }

    @Override
    public void setUserInformation(userInformation userInformation) {
        this.userInformation = userInformation;

        //保存
        SqlSession sqlSession = ServerTools.getServerInstanceOrThrow().getSqlSession();
        userInformationDao mapper = sqlSession.getMapper(userInformationDao.class);
        mapper.updateUser(userInformation);
    }

    @Override
    public userInformation getUserInformation() {
        return userInformation;
    }
}
