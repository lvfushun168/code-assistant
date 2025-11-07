package com.lfs.domain;

import lombok.Data;

@Data
public class LoginRequest {
    private String username;
    private String password;
    private String captcha;
    private String captchaId;
}
