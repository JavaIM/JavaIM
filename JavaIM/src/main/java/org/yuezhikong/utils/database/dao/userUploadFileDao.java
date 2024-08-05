package org.yuezhikong.utils.database.dao;

import org.apache.ibatis.annotations.Param;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.yuezhikong.Server.userData.userUploadFile;

import java.util.List;

public interface userUploadFileDao {
    /**
     * 获取用户上传的文件
     *
     * @param fileId 文件Id
     * @return 用户拥有的文件的数据库信息实体类
     */
    @Nullable
    @Contract(pure = true)
    userUploadFile getUploadFileByFileId(@NotNull @Param("ownFile") String fileId);

    /**
     * 获取上传文件的列表
     *
     * @return 用户拥有的文件的数据库信息实体类
     */
    @Nullable
    @Contract(pure = true)
    List<userUploadFile> getUploadFiles();

    /**
     * 获取用户上传的文件
     *
     * @param userId 用户Id
     * @return 用户拥有的文件的数据库信息实体类
     */
    @Nullable
    @Contract(pure = true)
    List<userUploadFile> getUploadFilesByUserId(@NotNull @Param("userId") String userId);

    /**
     * 向数据库添加一个文件信息
     *
     * @param file 文件
     * @return 操作是否成功
     */
    Boolean addFile(userUploadFile file);

    /**
     * 更新数据库保存的文件信息
     *
     * @param file 文件
     * @return 操作是否成功
     */
    Boolean updateFile(userUploadFile file);

    /**
     * 更新数据库保存的文件信息
     *
     * @param file 文件
     * @return 操作是否成功
     */
    Boolean deleteFile(userUploadFile file);
}
