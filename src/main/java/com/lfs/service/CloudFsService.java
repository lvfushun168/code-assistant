package com.lfs.service;

import cn.hutool.http.HttpResponse;
import cn.hutool.json.JSONUtil;
import com.lfs.config.AppConfig;
import com.lfs.domain.ApiResponse;
import com.lfs.util.NotificationUtil;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * 云端文件同步服务 - 支持文件夹上传下载
 */
public class CloudFsService {

    private static final int BUFFER_SIZE = 4096;

    /**
     * 上传 ZIP 文件到云端
     * @param localZipFile 本地 ZIP 文件
     * @param cloudPath 云端目标路径
     * @param override 是否覆盖
     * @return 是否成功
     */
    public boolean uploadZip(File localZipFile, String cloudPath, boolean override) {
        String url = AppConfig.BASE_URL + AppConfig.CLOUD_FS_UPLOAD_ZIP_URL 
                + "?destDir=" + cloudPath 
                + "&override=" + override;
        
        try {
            HttpResponse response = HttpClientService.createPostRequest(url, true)
                    .form("file", localZipFile)
                    .execute();
            
            HttpClientService.checkResponseStatus(response);
            
            ApiResponse apiResponse = JSONUtil.toBean(response.body(), ApiResponse.class);
            if (!apiResponse.isSuccess()) {
                NotificationUtil.showErrorDialog(null, "上传失败: " + apiResponse.getMessage());
                return false;
            }
            return true;
        } catch (TokenExpiredException e) {
            TokenManager.notifyTokenExpired();
            NotificationUtil.showToast(null, e.getMessage());
            return false;
        } catch (Exception e) {
            NotificationUtil.showErrorDialog(null, "上传ZIP失败: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * 从云端下载目录为 ZIP 文件
     * @param cloudPath 云端目录路径
     * @param destFile 本地保存路径
     * @return 是否成功
     */
    public boolean downloadZip(String cloudPath, File destFile) {
        String url = AppConfig.BASE_URL + AppConfig.CLOUD_FS_DOWNLOAD_ZIP_URL 
                + "?path=" + cloudPath;
        
        try {
            HttpResponse response = HttpClientService.createGetRequest(url, true).execute();
            HttpClientService.checkResponseStatus(response);
            
            // 检查响应状态
            int status = response.getStatus();
            if (status != 200) {
                NotificationUtil.showErrorDialog(null, "下载失败，状态码: " + status);
                return false;
            }
            
            // 获取响应字节并写入文件
            byte[] bodyBytes = response.bodyBytes();
            try (FileOutputStream fos = new FileOutputStream(destFile)) {
                fos.write(bodyBytes);
                fos.flush();
            }
            return true;
        } catch (TokenExpiredException e) {
            TokenManager.notifyTokenExpired();
            NotificationUtil.showToast(null, e.getMessage());
            return false;
        } catch (Exception e) {
            NotificationUtil.showErrorDialog(null, "下载ZIP失败: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * 将本地文件夹压缩为 ZIP 文件
     * @param sourceDir 源文件夹
     * @param destZipFile 目标 ZIP 文件
     */
    public void compressDirectoryToZip(File sourceDir, File destZipFile) throws IOException {
        try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(destZipFile), StandardCharsets.UTF_8)) {
            addDirToZip(zos, sourceDir, sourceDir.getName());
        }
    }

    /**
     * 递归添加目录到 ZIP
     */
    private void addDirToZip(ZipOutputStream zos, File dir, String baseName) throws IOException {
        File[] files = dir.listFiles();
        if (files == null) return;
        
        for (File file : files) {
            String entryName = baseName + "/" + file.getName();
            
            if (file.isDirectory()) {
                addDirToZip(zos, file, entryName);
            } else {
                ZipEntry zipEntry = new ZipEntry(entryName);
                zos.putNextEntry(zipEntry);
                
                try (FileInputStream fis = new FileInputStream(file)) {
                    byte[] buffer = new byte[BUFFER_SIZE];
                    int len;
                    while ((len = fis.read(buffer)) > 0) {
                        zos.write(buffer, 0, len);
                    }
                }
                zos.closeEntry();
            }
        }
    }

    /**
     * 解压 ZIP 文件到指定目录
     * @param zipFile ZIP 文件
     * @param destDir 目标目录
     */
    public void extractZipToDirectory(File zipFile, File destDir) throws IOException {
        java.util.zip.ZipFile zip = new java.util.zip.ZipFile(zipFile, StandardCharsets.UTF_8);
        try {
            java.util.Enumeration<? extends ZipEntry> entries = zip.entries();
            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                File entryFile = new File(destDir, entry.getName());

                // 安全检查：防止路径穿越
                String destCanonical = destDir.getCanonicalPath();
                String entryCanonical = entryFile.getCanonicalPath();
                if (!entryCanonical.startsWith(destCanonical + File.separator)) {
                    throw new IOException("禁止路径穿越: " + entry.getName());
                }

                if (entry.isDirectory()) {
                    entryFile.mkdirs();
                } else {
                    entryFile.getParentFile().mkdirs();
                    try (InputStream is = zip.getInputStream(entry);
                         FileOutputStream fos = new FileOutputStream(entryFile)) {
                        byte[] buffer = new byte[BUFFER_SIZE];
                        int len;
                        while ((len = is.read(buffer)) > 0) {
                            fos.write(buffer, 0, len);
                        }
                    }
                }
            }
        } finally {
            zip.close();
        }
    }
}
