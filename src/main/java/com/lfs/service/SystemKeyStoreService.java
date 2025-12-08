package com.lfs.service;

import cn.hutool.core.io.FileUtil;
import cn.hutool.system.SystemUtil;
import com.sun.jna.platform.win32.Crypt32Util;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.nio.charset.StandardCharsets;

/**
 * 系统级密钥存储服务
 * 负责将 DEK 存储到操作系统的安全容器中 (Windows DPAPI, Mac Keychain等)
 */
@Slf4j
public class SystemKeyStoreService {

    private static final String APP_DATA_DIR = System.getProperty("user.home") + "/.code-assistant/keystore/";

    /**
     * 安全存储 DEK
     *
     * @param dek 原始 DEK 字节数组
     * @param accountId 关联的账号ID
     * @return 是否存储成功
     */
    public boolean storeDek(byte[] dek, String accountId) {
        try {
            if (SystemUtil.getOsInfo().isWindows()) {
                return storeWindowsDPAPI(dek, accountId);
            } else {
                // 对于 macOS/Linux，实际生产环境应使用 Keychain/SecretService
                // 这里为演示目的，使用简单文件存储模拟
                return storeFallback(dek, accountId);
            }
        } catch (Exception e) {
            log.error("存储密钥失败", e);
            return false;
        }
    }

    /**
     * 使用 Windows DPAPI 存储 (调用 CryptProtectData)
     * 只有当前登录的 Windows 用户才能解密
     */
    private boolean storeWindowsDPAPI(byte[] dek, String accountId) {
        try {
            // 使用 JNA 提供的 Crypt32Util 进行 DPAPI 加密
            byte[] protectedData = Crypt32Util.cryptProtectData(dek);
            
            // 将加密后的 blob 写入本地文件
            File keyFile = new File(APP_DATA_DIR + accountId + ".key");
            FileUtil.writeBytes(protectedData, keyFile);
            
            log.info("已使用 Windows DPAPI 保护 DEK 并存储至: {}", keyFile.getAbsolutePath());
            return true;
        } catch (Exception e) {
            log.error("DPAPI 加密失败", e);
            return false;
        }
    }

    /**
     * 简单的回退存储 (模拟其他系统的存储，实际应加密)
     */
    private boolean storeFallback(byte[] dek, String accountId) {
        File keyFile = new File(APP_DATA_DIR + accountId + ".key");
        FileUtil.writeBytes(dek, keyFile);
        log.warn("非Windows系统，正在使用非安全存储模拟 (生产环境请对接Keychain): {}", keyFile.getAbsolutePath());
        return true;
    }
    
    /**
     * 读取并解密 DEK (用于后续自动登录)
     */
    public byte[] loadDek(String accountId) {
        File keyFile = new File(APP_DATA_DIR + accountId + ".key");
        if (!keyFile.exists()) return null;
        
        byte[] data = FileUtil.readBytes(keyFile);
        
        if (SystemUtil.getOsInfo().isWindows()) {
            try {
                return Crypt32Util.cryptUnprotectData(data);
            } catch (Exception e) {
                log.error("DPAPI 解密失败", e);
                return null;
            }
        } else {
            return data;
        }
    }
}