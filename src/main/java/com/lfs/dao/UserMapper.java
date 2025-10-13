package com.lfs.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lfs.domain.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

/**
 * 用户 Mapper 接口
 */
@Mapper
public interface UserMapper extends BaseMapper<User> {

    /**
     * 根据用户名查找用户
     *
     * @param username 用户名
     * @return 用户
     */
    @Select("SELECT * FROM user WHERE username = #{username}")
    User findByUsername(String username);
}
