package com.lfs.service;

import cn.hutool.core.codec.Base64;
import lombok.extern.slf4j.Slf4j;
import org.bouncycastle.crypto.generators.Argon2BytesGenerator;
import org.bouncycastle.crypto.params.Argon2Parameters;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;

/**
 * 核心加密服务
 * 负责执行 Argon2 密钥派生和 AES 解密
 */
@Slf4j
public class CryptoService {

    private static final int ARGON2_SALT_LENGTH = 16;
    private static final int ARGON2_HASH_LENGTH = 32; // 生成 256-bit KEK
    private static final int ARGON2_ITERATIONS = 3;
    private static final int ARGON2_MEMORY = 65536; // 64MB
    private static final int ARGON2_PARALLELISM = 1;

    /**
     * 使用 Argon2id 算法从密码和盐派生 KEK (Key Encryption Key)
     *
     * @param password 用户输入的明文密码
     * @param saltBase64 服务端返回的 Salt (Base64)
     * @return 派生出的 KEK 字节数组
     */
    public byte[] deriveKek(String password, String saltBase64) {
        byte[] salt = Base64.decode(saltBase64);
        byte[] passwordBytes = password.getBytes(StandardCharsets.UTF_8);

        Argon2Parameters.Builder builder = new Argon2Parameters.Builder(Argon2Parameters.ARGON2_id)
                .withVersion(Argon2Parameters.ARGON2_VERSION_13)
                .withIterations(ARGON2_ITERATIONS)
                .withMemoryAsKB(ARGON2_MEMORY)
                .withParallelism(ARGON2_PARALLELISM)
                .withSalt(salt);

        Argon2BytesGenerator gen = new Argon2BytesGenerator();
        gen.init(builder.build());

        byte[] result = new byte[ARGON2_HASH_LENGTH];
        gen.generateBytes(passwordBytes, result, 0, result.length);
        
        return result;
    }

    /**
     * 使用 KEK 解密 DEK (Data Encryption Key)
     * 假设使用 AES/GCM/NoPadding
     *
     * @param encryptedDekBase64 加密的 DEK (Base64)
     * @param kek 派生出的 Key Encryption Key
     * @param nonceBase64 加密时使用的 IV/Nonce (Base64)
     * @return 解密后的 DEK 字节数组
     */
    public byte[] decryptDek(String encryptedDekBase64, byte[] kek, String nonceBase64) throws Exception {
        byte[] encryptedDek = Base64.decode(encryptedDekBase64);
        byte[] nonce = Base64.decode(nonceBase64);

        SecretKeySpec keySpec = new SecretKeySpec(kek, "AES");
        GCMParameterSpec gcmSpec = new GCMParameterSpec(128, nonce); // 128 bit auth tag length

        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.DECRYPT_MODE, keySpec, gcmSpec);

        return cipher.doFinal(encryptedDek);
    }
}