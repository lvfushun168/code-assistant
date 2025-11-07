package com.lfs.domain;

import lombok.Data;

@Data
public class CaptchaResponse {
    private byte[] imageData;
    private String captchaId;
}
