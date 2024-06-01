package org.yuezhikong.utils.database.dao;

import org.apache.ibatis.annotations.Param;
import org.jetbrains.annotations.Nullable;
import org.yuezhikong.Server.UserData.userInformation;

import java.util.List;

public interface userInformationDao {
    /**
     * 从数据库获取用户列表
     * @return 用户数据库信息实体类
     */
    @Nullable
    List<userInformation> getUserList();

    /**
     * 获取用户
     *
     * @param userId    用户Id
     * @param userName  用户名
     * @param token     Token
     * @param salt      Salt
     * @return  用户数据库信息实体类
     * @apiNote 只需要有一个条件即可查询,无需全部满足
     */
    @Nullable
    userInformation getUser(@Nullable @Param("userId") String userId,
                            @Nullable @Param("userName") String userName,
                            @Nullable @Param("token") String token,
                            @Nullable @Param("salt") String salt
    );

    /**
     * 根据用户名从数据库中获取用户
     * @param userName 用户名
     * @return 用户数据库信息实体类
     */
    @Nullable
    userInformation getUserByName(String userName);

    /**
     * 根据Token从数据库中获取用户
     * @param token Token
     * @return 用户数据库信息实体类
     */
    @Nullable
    userInformation getUserByToken(String token);

    /**
     * 根据密码盐从数据库中获取用户
     * @param salt 盐
     * @return 用户数据库信息实体类
     */
    @Nullable
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
