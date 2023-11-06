package com.zzh.luntan.mapper;

import com.zzh.luntan.entity.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface UserMapper {
    User selectById(@Param("id") int id);

    User selectByUsername(@Param("username") String username);

    User selectByEmail(@Param("email") String email);

    int insertUser(@Param("user") User user);

    int updateStatus(@Param("id") int id, @Param("status") int status);

    int updateHeaderUrl(@Param("id") int id, @Param("headerUrl") String headerUrl);

    int updatePassword(@Param("id") int id, @Param("password") String password);

    int updateProfile(@Param("id") int id, @Param("newUserName") String newUserName, @Param("newContact") String newContact, @Param("newDescription") String newDescription);

    User selectByName(@Param("userName") String userName);

    int updateProfileImg(@Param("userId") int userId, @Param("fileNames") String fileNames);

    int updateProfileMusic(@Param("userId") int userId, @Param("fileName") String fileName);

    int updateProfileVideo(@Param("userId") int userId, @Param("fileName") String fileName);

    int updateProfileBack(@Param("userId") int userId, @Param("fileName") String fileName);
}
