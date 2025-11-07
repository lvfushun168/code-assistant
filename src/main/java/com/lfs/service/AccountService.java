package com.lfs.service;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import com.lfs.domain.BackendResponse;
import com.lfs.domain.User;
import lombok.extern.slf4j.Slf4j;
import org.mindrot.jbcrypt.BCrypt;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;

/**
 * 账户服务类，处理注册、登录等业务逻辑
 */
@Slf4j
public class AccountService {

    private static final String REGISTER_URL = "http://8.148.146.195:8080/lfs-code-assistant/account/register";

    /**
     * 注册新用户
     *
     * @param username 用户名
     * @param password 原始密码
     * @return BackendResponse 包含注册结果
     */
    public BackendResponse<Object> register(String username, String password) {
        try {
            String hashedPassword = BCrypt.hashpw(password, BCrypt.gensalt());

            User user = new User();
            user.setUsername(username);
            user.setPassword(hashedPassword);
            user.setNickname(username);

            HttpResponse response = HttpRequest.post(REGISTER_URL)
                    .body(JSON.toJSONString(user))
                    .contentType("application/json")
                    .execute();

            String responseBody = response.body();
            return JSON.parseObject(responseBody, new TypeReference<>() {
            });

        } catch (Exception e) {
            log.error("注册请求失败", e);
            BackendResponse<Object> errorResponse = new BackendResponse<>();
            errorResponse.setCode(500);
            errorResponse.setMessage(e.getMessage());
            return errorResponse;
        }
    }
}
