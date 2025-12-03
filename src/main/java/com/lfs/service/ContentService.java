package com.lfs.service;

import cn.hutool.core.io.IORuntimeException;
import com.lfs.config.AppConfig;
import com.lfs.util.NotificationUtil;

import java.net.ConnectException;

public class ContentService {
    /**
     * 根据内容ID下载文件内容
     * @param id 内容ID
     * @return 文件内容字符串
     */
    public String downloadContent(Long id) {
        String url = AppConfig.BASE_URL + AppConfig.CONTENT_DOWNLOAD_URL + "/" + id;
        try {
            return HttpClientService.createGetRequest(url, true).execute().body();
        } catch (IORuntimeException e) {
            if (e.getCause() instanceof ConnectException) {
                NotificationUtil.showErrorDialog(null, "连接后端服务失败，请确认服务是否已启动。");
            } else {
                NotificationUtil.showErrorDialog(null, "下载文件时发生网络异常: " + e.getMessage());
            }
            e.printStackTrace();
            return null;
        } catch (Exception e) {
            NotificationUtil.showErrorDialog(null, "下载文件时发生未知异常: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }
}
