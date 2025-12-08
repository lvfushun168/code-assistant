package com.lfs.domain;

import lombok.Data;

/**
 * 服务端返回的密钥包结构
 */
@Data
public class KeyPackageResponse {
    /**
     * 用户Salt (Base64编码), 用于Argon2派生KEK
     */
    private String salt;

    /**
     * 被KEK加密后的DEK (Base64编码)
     * DEK (Data Encryption Key) 是用于解密用户数据的核心密钥
     */
    private String encryptedDek;

    /**
     * 加密DEK时使用的Nonce/IV (Base64编码)
     */
    private String nonce;
    
    /**
     * Argon2 参数 (可选，如果服务端动态调整)
     */
    private Integer memoryCost;
    private Integer iterations;
    private Integer parallelism;
}