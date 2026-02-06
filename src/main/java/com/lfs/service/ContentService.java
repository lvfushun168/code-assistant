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
            HttpResponse response = HttpClientService.createGetRequest(url, true).execute();
            HttpClientService.checkResponseStatus(response);
            return response.body();
        } catch (IORuntimeException e) {
            if (e.getCause() instanceof ConnectException) {
                NotificationUtil.showErrorDialog(null, "连接后端服务失败，请确认服务是否已启动。");
            } else {
                NotificationUtil.showErrorDialog(null, "下载文件时发生网络异常: " + e.getMessage());
            }
            e.printStackTrace();
            return null;
        } catch (TokenExpiredException e) {
            TokenManager.notifyTokenExpired();
            NotificationUtil.showErrorDialog(null, e.getMessage());
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
    public ContentResponse createContent(Long dirId, String title, String content, String type) {
        String url = AppConfig.BASE_URL + AppConfig.CONTENT_URL;

        try {
            // 1. 构建meta部分
            Map<String, Object> meta = new HashMap<>();
            meta.put("dirId", dirId);
            meta.put("title", title);
            meta.put("type", type); // 添加类型
            String metaJson = JSONUtil.toJsonStr(meta);
            // 将JSON字符串包装成BytesResource，并提供一个.json后缀的文件名，Hutool会据此设置Content-Type
            byte[] metaBytes = metaJson.getBytes(StandardCharsets.UTF_8);
            BytesResource metaResource = new BytesResource(metaBytes, "meta.json");

            // 2. 构建文件部分
            byte[] contentBytes = content.getBytes(StandardCharsets.UTF_8);
            // 使用传入的type作为文件扩展名
            BytesResource fileResource = new BytesResource(contentBytes, title + "." + type);

            // 3. 发送multipart/form-data请求
            HttpResponse response = HttpClientService.createPostRequest(url, true)
                    .form("meta", metaResource)
                    .form("file", fileResource)
                    .execute();

            HttpClientService.checkResponseStatus(response);

            String body = response.body();
            ApiResponse apiResponse = JSONUtil.toBean(body, ApiResponse.class);

            if (apiResponse.isSuccess()) {
                Object data = apiResponse.getData();
                if (data instanceof Integer) {
                    ContentResponse newContent = new ContentResponse();
                    newContent.setId(Long.valueOf((Integer) data));
                    newContent.setDirId(dirId);
                    newContent.setTitle(title);
                    return newContent;
                } else if (data != null) {
                    // 如果data不是Long，但也不是null，尝试解析成ContentResponse对象
                    return JSONUtil.toBean(JSONUtil.toJsonStr(data), ContentResponse.class);
                } else {
                    // data为null，返回一个临时的ContentResponse（无ID），以便UI能够显示新文件
                    ContentResponse tempResponse = new ContentResponse();
                    tempResponse.setTitle(title);
                    tempResponse.setDirId(dirId);
                    tempResponse.setType(type);
                    return tempResponse;
                }
            } else {
                NotificationUtil.showErrorDialog(null, "创建文件失败: " + apiResponse.getMessage());
                return null;
            }
        } catch (TokenExpiredException e) {
            TokenManager.notifyTokenExpired();
            NotificationUtil.showErrorDialog(null, e.getMessage());
            return null;
        } catch (Exception e) {
            NotificationUtil.showErrorDialog(null, "创建文件时发生异常: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    /**
     * 更新文档
     * @param id 文档ID
     * @param dirId 目录ID
     * @param title 文档标题
     * @param content 文档内容 (可选, 为null则不更新)
     * @return 更新成功后的文档信息
     */
    public ContentResponse updateContent(Long id, Long dirId, String title, String content, String type) {
        String url = AppConfig.BASE_URL + AppConfig.CONTENT_URL;

        try {
            // 1. 构建meta部分
            Map<String, Object> meta = new HashMap<>();
            meta.put("id", id);
            meta.put("dirId", dirId);
            meta.put("title", title);
            if (type != null && !type.isEmpty()) {
                meta.put("type", type);
            }
            String metaJson = JSONUtil.toJsonStr(meta);
            byte[] metaBytes = metaJson.getBytes(StandardCharsets.UTF_8);
            BytesResource metaResource = new BytesResource(metaBytes, "meta.json");

            var request = HttpClientService.createPutRequest(url, true)
                    .form("meta", metaResource);

            // 2. 如果提供了内容，则添加 file part
            if (content != null) {
                byte[] contentBytes = content.getBytes(StandardCharsets.UTF_8);
                String extension = (type != null && !type.isEmpty()) ? type : "txt";
                BytesResource fileResource = new BytesResource(contentBytes, title + "." + extension);
                request.form("file", fileResource);
            }


            // 3. 发送multipart/form-data请求
            HttpResponse response = request.execute();

            HttpClientService.checkResponseStatus(response);

            String body = response.body();
            ApiResponse apiResponse = JSONUtil.toBean(body, ApiResponse.class);

            if (apiResponse.isSuccess()) {
                // 更新操作成功后，返回一个包含更新后信息的新对象，以便UI刷新
                ContentResponse updatedContent = new ContentResponse();
                updatedContent.setId(id);
                updatedContent.setDirId(dirId);
                updatedContent.setTitle(title);
                updatedContent.setType(type);
                return updatedContent;
            } else {
                NotificationUtil.showErrorDialog(null, apiResponse.getMessage());
                return null;
            }
        } catch (TokenExpiredException e) {
            TokenManager.notifyTokenExpired();
            NotificationUtil.showErrorDialog(null, e.getMessage());
            return null;
        } catch (Exception e) {
            NotificationUtil.showErrorDialog(null, "更新文件时发生异常: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    /**
     * 删除文档
     * @param id 文档ID
     * @return 操作是否成功
     */
    public boolean deleteContent(Long id) {
        String url = AppConfig.BASE_URL + AppConfig.CONTENT_URL + "/" + id;
        try {
            HttpResponse response = HttpClientService.createDeleteRequest(url, true).execute();
            HttpClientService.checkResponseStatus(response);
            String body = response.body();
            ApiResponse apiResponse = JSONUtil.toBean(body, ApiResponse.class);

            if (!apiResponse.isSuccess()) {
                NotificationUtil.showErrorDialog(null, "删除文件失败: " + apiResponse.getMessage());
            }
            return apiResponse.isSuccess();
        } catch (TokenExpiredException e) {
            TokenManager.notifyTokenExpired();
            NotificationUtil.showErrorDialog(null, e.getMessage());
            return false;
        } catch (Exception e) {
            NotificationUtil.showErrorDialog(null, "删除文件时发生异常: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
}
