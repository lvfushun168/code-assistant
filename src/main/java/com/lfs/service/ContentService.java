package com.lfs.service;

import cn.hutool.core.io.IORuntimeException;
import cn.hutool.core.io.resource.BytesResource;
import cn.hutool.http.HttpResponse;
import cn.hutool.json.JSONUtil;
import com.lfs.config.AppConfig;
import com.lfs.domain.ApiResponse;
import com.lfs.domain.ContentResponse;
import com.lfs.util.NotificationUtil;

import java.net.ConnectException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class ContentService {
    /**
     * 根据内容ID下载文件内容
     *
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

    /**
     * 创建新文档
     *
     * @param dirId   目录ID
     * @param title   文档标题
     * @param content 文档内容
     * @return 创建成功后的文档信息
     */
    public ContentResponse createContent(Long dirId, String title, String content) {
        String url = AppConfig.BASE_URL + AppConfig.CONTENT_URL;

        try {
            // 1. 构建meta部分
            Map<String, Object> meta = new HashMap<>();
            meta.put("dirId", dirId);
            meta.put("title", title);
            String metaJson = JSONUtil.toJsonStr(meta);
            // 将JSON字符串包装成BytesResource，并提供一个.json后缀的文件名，Hutool会据此设置Content-Type
            byte[] metaBytes = metaJson.getBytes(StandardCharsets.UTF_8);
            BytesResource metaResource = new BytesResource(metaBytes, "meta.json");

            // 2. 构建文件部分
            byte[] contentBytes = content.getBytes(StandardCharsets.UTF_8);
            BytesResource fileResource = new BytesResource(contentBytes, title + ".txt");

            // 3. 发送multipart/form-data请求
            HttpResponse response = HttpClientService.createPostRequest(url, true)
                    .form("meta", metaResource)
                    .form("file", fileResource)
                    .execute();

            String body = response.body();
            ApiResponse apiResponse = JSONUtil.toBean(body, ApiResponse.class);

            if (apiResponse.isSuccess()) {
                Object data = apiResponse.getData();
                if (data instanceof Integer) { // 后端可能返回Integer类型
                    Long id = ((Integer) data).longValue();
                    ContentResponse newContent = new ContentResponse();
                    newContent.setId(id);
                    newContent.setDirId(dirId);
                    newContent.setTitle(title);
                    return newContent;
                } else if (data instanceof Long) {
                    Long id = (Long) data;
                    ContentResponse newContent = new ContentResponse();
                    newContent.setId(id);
                    newContent.setDirId(dirId);
                    newContent.setTitle(title);
                    return newContent;
                } else if (data != null) {
                    // 如果data不是Long/Integer，但也不是null，尝试解析成ContentResponse对象
                    return JSONUtil.toBean(JSONUtil.toJsonStr(data), ContentResponse.class);
                } else {
                    // data为null，返回一个临时的ContentResponse（无ID），以便UI能够显示新文件
                    ContentResponse tempResponse = new ContentResponse();
                    tempResponse.setTitle(title);
                    tempResponse.setDirId(dirId);
                    return tempResponse;
                }
            } else {
                NotificationUtil.showErrorDialog(null, "创建文件失败: " + apiResponse.getMessage());
                return null;
            }
        } catch (Exception e) {
            NotificationUtil.showErrorDialog(null, "创建文件时发生异常: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }
}
