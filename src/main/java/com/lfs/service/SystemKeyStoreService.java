package com.lfs.service;

import cn.hutool.core.io.FileUtil;
import cn.hutool.system.SystemUtil;
import com.sun.jna.platform.win32.Crypt32Util;
import lombok.extern.slf4j.Slf4j;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

/**
 * 系统级密钥存储服务
 * 负责将 DEK 存储到操作系统的安全容器中 (Windows DPAPI, Mac Keychain等)
 */
@Slf4j
public class SystemKeyStoreService {

    private static final String APP_DATA_DIR = System.getProperty("user.home") + "/.code-assistant/keystore/";
    private static final String SERVICE_NAME = "CodeAssistant";

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
            } else if (SystemUtil.getOsInfo().isMac()) {
                return storeMacKeychain(dek, accountId);
            } else {
                // 对于 Linux，实际生产环境应使用 SecretService (libsecret)
                // 这里暂时使用简单文件存储模拟
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
     * 使用 macOS Keychain 存储 (调用 security 命令行工具)
     * 存储服务名为 "CodeAssistant"，账户名为 accountId
     */
    private boolean storeMacKeychain(byte[] dek, String accountId) {
        try {
            // 将二进制 DEK 转换为 Base64 字符串以便作为密码字段存储
            String dekBase64 = cn.hutool.core.codec.Base64.encode(dek);

            // 构建命令: security add-generic-password -a <account> -s <service> -w <password> -U
            // -a: 账户名
            // -s: 服务名
            // -w: 密码内容 (这里存的是 Base64 后的 DEK)
            // -U: 如果存在则更新
            ProcessBuilder pb = new ProcessBuilder(
                    "security", "add-generic-password",
                    "-a", accountId,
                    "-s", SERVICE_NAME,
                    "-w", dekBase64,
                    "-U"
            );

            Process p = pb.start();
            int exitCode = p.waitFor();

            if (exitCode == 0) {
                log.info("已将 DEK 安全存储至 macOS Keychain (Account: {})", accountId);
                return true;
            } else {
                // 读取错误输出以便调试
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(p.getErrorStream()))) {
                    String errorLine = reader.readLine();
                    log.error("macOS Keychain 存储失败，exit code: {}, error: {}", exitCode, errorLine);
                }
                return false;
            }
        } catch (Exception e) {
            log.error("Mac Keychain 存储异常", e);
            return false;
        }
    }

    /**
     * 简单的回退存储 (模拟其他系统的存储，实际应加密)
     */
    private boolean storeFallback(byte[] dek, String accountId) {
        File keyFile = new File(APP_DATA_DIR + accountId + ".key");
        FileUtil.writeBytes(dek, keyFile);
        log.warn("非Windows/Mac系统，正在使用非安全存储模拟: {}", keyFile.getAbsolutePath());
        return true;
    }

    /**
     * 读取并解密 DEK (用于后续自动登录)
     */
    public byte[] loadDek(String accountId) {
        // macOS 走 Keychain 逻辑
        if (SystemUtil.getOsInfo().isMac()) {
            return loadMacKeychain(accountId);
        }

        // Windows 和 Fallback 走文件读取逻辑
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
            // Linux Fallback
            return data;
        }
    }

    /**
     * 从 macOS Keychain 读取 DEK
     */
    private byte[] loadMacKeychain(String accountId) {
        try {
            // 构建命令: security find-generic-password -a <account> -s <service> -w
            // -w: 仅输出密码内容 (text)
            ProcessBuilder pb = new ProcessBuilder(
                    "security", "find-generic-password",
                    "-a", accountId,
                    "-s", SERVICE_NAME,
                    "-w"
            );

            Process p = pb.start();

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream(), StandardCharsets.UTF_8))) {
                String line = reader.readLine();
                int exitCode = p.waitFor();

                if (exitCode == 0 && line != null && !line.isEmpty()) {
                    log.info("成功从 macOS Keychain 读取 DEK");
                    return cn.hutool.core.codec.Base64.decode(line.trim());
                } else {
                    // exitCode 44 通常表示 "Item not found" (密钥不存在)，这是正常的（例如首次登录）
                    if (exitCode != 44) {
                        log.warn("macOS Keychain 读取未找到或失败，exit code: {}", exitCode);
                    }
                }
            }
        } catch (Exception e) {
            log.error("Mac Keychain 读取异常", e);
        }
        return null;
    }
}