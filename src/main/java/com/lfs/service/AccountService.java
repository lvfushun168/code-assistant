package com.lfs.service;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import com.lfs.config.AppConfig;
import com.lfs.domain.BackendResponse;
import com.lfs.domain.CaptchaResponse;
import com.lfs.domain.KeyPackageResponse;
import com.lfs.domain.LoginRequest;
import com.lfs.domain.User;
import lombok.extern.slf4j.Slf4j;
import cn.hutool.http.HttpResponse;
import cn.hutool.crypto.digest.DigestUtil;

import java.util.List;
import java.util.UUID;

/**
 * 账户服务类，处理注册、登录等业务逻辑
 */
@Slf4j
public class AccountService {

    /**
     * 获取密钥包 (Salt, Encrypted_DEK)
     * 用于设备首次登录时恢复 DEK
     *
     * @param username 用户名
     * @return 密钥包响应
     */
    public BackendResponse<KeyPackageResponse> getKeyPackage(String username) {
        try {
            // 假设后端接口为 /account/key-package?username=xxx
            String url = AppConfig.BASE_URL + AppConfig.API_PREFIX + "/account/key-package?username=" + username;

            HttpResponse response = HttpClientService.createGetRequest(url, false).execute();

            String responseBody = response.body();
            return JSON.parseObject(responseBody, new TypeReference<BackendResponse<KeyPackageResponse>>() {});
        } catch (Exception e) {
            log.error("获取密钥包失败", e);
            BackendResponse<KeyPackageResponse> errorResponse = new BackendResponse<>();
            errorResponse.setCode(500);
            errorResponse.setMessage(e.getMessage());
            return errorResponse;
        }
    }

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

            HttpResponse response = HttpClientService.createPostRequest(AppConfig.BASE_URL + AppConfig.REGISTER_URL, false)
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
            // 注意：这里仍然发送 Hash 后的密码用于身份认证（换取Token）
            // 在更严格的零知识架构中，身份认证可能使用 SRP 协议，但此处保持与原逻辑兼容
            String hashedPassword = DigestUtil.sha256Hex(password);

            LoginRequest loginRequest = new LoginRequest();
            loginRequest.setUsername(username);
            loginRequest.setPassword(hashedPassword);
            loginRequest.setCaptcha(captcha);
            loginRequest.setNonce(UUID.randomUUID().toString()); // 生成随机 nonce
            loginRequest.setTimestamp(String.valueOf(System.currentTimeMillis())); // 获取当前时间戳

            HttpResponse response = HttpClientService.createPostRequest(AppConfig.BASE_URL + AppConfig.LOGIN_URL, false)
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
            HttpResponse response = HttpClientService.createGetRequest(AppConfig.BASE_URL + AppConfig.CAPTCHA_URL, false).execute();
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