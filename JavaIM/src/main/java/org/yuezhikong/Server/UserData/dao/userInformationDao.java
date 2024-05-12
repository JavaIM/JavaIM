package org.yuezhikong.Server.UserData.dao;

import org.yuezhikong.Server.UserData.userInformation;

import java.util.List;

public interface userInformationDao {
    /**
     * 从数据库获取用户列表
     * @return 用户数据库信息实体类
     */
    List<userInformation> getUserList();

    /**
     * 根据用户名从数据库中获取用户
     * @param userName 用户名
     * @return 用户数据库信息实体类
     */
    userInformation getUserByName(String userName);

    /**
     * 根据Token从数据库中获取用户
     * @param token Token
     * @return 用户数据库信息实体类
     */
    userInformation getUserByToken(String token);

    /**
     * 根据密码盐从数据库中获取用户
     * @param salt 盐
     * @return 用户数据库信息实体类
     */
    userInformation getUserBySalt(String salt);

    /**
     * 向数据库添加一个用户
     * @param User 用户
     * @return 操作是否成功
     */
    Boolean addUser(userInformation User);

    /**
     * 更新数据库保存的用户信息
     * @param User 用户
     * @return 操作是否成功
     */
    Boolean updateUser(userInformation User);
}
