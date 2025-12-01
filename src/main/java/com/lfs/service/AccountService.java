package com.lfs.service;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import com.lfs.domain.BackendResponse;
import com.lfs.domain.CaptchaResponse;
import com.lfs.domain.LoginRequest;
import com.lfs.domain.User;
import lombok.extern.slf4j.Slf4j;
import org.mindrot.jbcrypt.BCrypt;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.crypto.digest.DigestUtil;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 账户服务类，处理注册、登录等业务逻辑
 */
@Slf4j
public class AccountService {

//    private static final String REGISTER_URL = "https://8.148.146.195/lfs-code-assistant/account/register";
//    private static final String LOGIN_URL = "https://8.148.146.195/lfs-code-assistant/account/login";
//    private static final String CAPTCHA_URL = "https://8.148.146.195/lfs-code-assistant/captcha/generate";
    private static final String REGISTER_URL = "http://localhost:6324/lfs-code-assistant/account/register";
    private static final String LOGIN_URL = "http://localhost:6324/lfs-code-assistant/account/login";
    private static final String CAPTCHA_URL = "http://localhost:6324/lfs-code-assistant/captcha/generate";

    /**
     * 注册新用户
     *
     * @param username 用户名
     * @param password 原始密码
     * @return BackendResponse 包含注册结果
     */
    public BackendResponse<Object> register(String username, String password, String captcha, String captchaId) {
        try {
            String hashedPassword = DigestUtil.sha256Hex(password);

            User user = new User();
            user.setUsername(username);
            user.setPassword(hashedPassword);
            user.setNickname(username);
            user.setCaptcha(captcha);

            HttpResponse response = HttpClientService.createPostRequest(REGISTER_URL, false)
                    .cookie("captchaCode=" + captchaId)
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

    public BackendResponse<String> login(String username, String password, String captcha, String captchaId) {
        try {
            String hashedPassword = DigestUtil.sha256Hex(password);

            LoginRequest loginRequest = new LoginRequest();
            loginRequest.setUsername(username);
            loginRequest.setPassword(hashedPassword);
            loginRequest.setCaptcha(captcha);
            loginRequest.setNonce(UUID.randomUUID().toString()); // 生成随机 nonce
            loginRequest.setTimestamp(String.valueOf(System.currentTimeMillis())); // 获取当前时间戳

            HttpResponse response = HttpClientService.createPostRequest(LOGIN_URL, false)
                    .cookie("captchaCode=" + captchaId)
                    .body(JSON.toJSONString(loginRequest))
                    .contentType("application/json")
                    .execute();

            String responseBody = response.body();
            return JSON.parseObject(responseBody, new TypeReference<BackendResponse<String>>() {});

        } catch (Exception e) {
            log.error("登录请求失败", e);
            BackendResponse<String> errorResponse = new BackendResponse<>();
            errorResponse.setCode(500);
            errorResponse.setMessage(e.getMessage());
            return errorResponse;
        }
    }

    public CaptchaResponse getCaptcha() {
        try {
            HttpResponse response = HttpClientService.createGetRequest(CAPTCHA_URL, false).execute();
            byte[] imageData = response.bodyBytes();

            // 从Cookie中获取captchaId
            String captchaId = null;
            List<String> cookies = response.headers().get("Set-Cookie");
            if (cookies != null) {
                for (String cookie : cookies) {
                    if (cookie.startsWith("captchaCode=")) {
                        captchaId = cookie.split("=")[1].split(";")[0];
                        break;
                    }
                }
            }

            if (captchaId == null) {
                throw new RuntimeException("获取验证码失败，无法找到 captchaCode");
            }

            CaptchaResponse captchaResponse = new CaptchaResponse();
            captchaResponse.setImageData(imageData);
            captchaResponse.setCaptchaId(captchaId);
            return captchaResponse;
        } catch (Exception e) {
            log.error("获取验证码失败", e);
            throw new RuntimeException("获取验证码时发生错误: " + e.getMessage(), e);
        }
    }
}
