package com.lfs.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.lfs.dao.UserMapper;
import com.lfs.domain.User;
import com.lfs.util.MyBatisUtil;
import org.apache.ibatis.session.SqlSession;
import org.mindrot.jbcrypt.BCrypt;

/**
 * 账户服务类，处理注册、登录等业务逻辑
 */
public class AccountService {

    /**
     * 注册新用户
     *
     * @param username 用户名
     * @param password 原始密码
     * @return 如果注册成功返回 true, 如果用户已存在或发生错误则返回 false
     */
    public boolean register(String username, String password) {
        try (SqlSession sqlSession = MyBatisUtil.getSqlSession()) {
            UserMapper userMapper = sqlSession.getMapper(UserMapper.class);

            // 1. 检查用户是否已存在
            User existingUser = userMapper.selectOne(new QueryWrapper<User>().eq("username", username));
            if (existingUser != null) {
                System.err.println("注册失败：用户名 " + username + " 已存在。");
                return false;
            }

            // 2. 密码加密
            String hashedPassword = BCrypt.hashpw(password, BCrypt.gensalt());

            // 3. 创建用户对象
            User user = new User();
            user.setUsername(username);
            user.setPassword(hashedPassword);
            // 可以在这里设置默认的昵称等
            user.setNickname(username);

            // 4. 添加用户到数据库
            int result = userMapper.insert(user);
            sqlSession.commit(); // 提交事务
            return result > 0;
        } catch (Exception e) {
            e.printStackTrace(); // 实际项目中应使用日志框架
            return false;
        }
    }
}
