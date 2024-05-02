package org.yuezhikong.Server.UserData.dao;

import org.yuezhikong.Server.UserData.userInformation;

import java.util.List;

public interface userInformationDao {
    List<userInformation> getUserList();
    userInformation getUserByName(String userName);
    userInformation getUserByToken(String token);
    userInformation getUserBySalt(String salt);

    Boolean addUser(userInformation User);

    Boolean updateUser(userInformation User);
}
