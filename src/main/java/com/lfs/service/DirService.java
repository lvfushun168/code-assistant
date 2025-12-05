package com.lfs.service;

import cn.hutool.core.lang.TypeReference;
import cn.hutool.http.HttpResponse;
import cn.hutool.json.JSONUtil;
import com.lfs.config.AppConfig;
import com.lfs.domain.ApiResponse;
import com.lfs.domain.BackendResponse;
import com.lfs.domain.DirTreeResponse;
import com.lfs.domain.dto.CreateDirRequest;
import com.lfs.domain.dto.UpdateDirRequest;
import com.lfs.util.NotificationUtil;

public class DirService {

    private final HttpClientService httpClientService = new HttpClientService();

    /**
     * 获取云端目录树
     * @return 目录树根节点
     */
    public DirTreeResponse getDirTree() {
        String url = AppConfig.BASE_URL + AppConfig.DIR_TREE_URL;
        try {
            String responseBody = HttpClientService.createGetRequest(url, true).execute().body();
            if (responseBody == null) {
                NotificationUtil.showErrorDialog(null, "获取云端目录失败，响应为空");
                return null;
            }

            // 使用Hutool的TypeReference来处理泛型
            TypeReference<ApiResponse<DirTreeResponse>> typeRef = new TypeReference<ApiResponse<DirTreeResponse>>() {};
            ApiResponse<DirTreeResponse> apiResponse = JSONUtil.toBean(responseBody, typeRef, false);


            if (apiResponse != null && apiResponse.getCode() == 200) {
                return apiResponse.getData();
            } else {
                String errorMessage = apiResponse != null ? apiResponse.getMessage() : "未知错误";
                NotificationUtil.showErrorDialog(null, "获取云端目录失败: " + errorMessage);
                return null;
            }
        } catch (Exception e) {
            NotificationUtil.showErrorDialog(null, "请求云端目录时发生异常: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    public DirTreeResponse createDir(Long parentId, String name) {
        String url = AppConfig.BASE_URL + AppConfig.DIR_URL;
        try {
            CreateDirRequest request = new CreateDirRequest(parentId, name);
            HttpResponse response = HttpClientService.createPostRequest(url, true)
                    .body(JSONUtil.toJsonStr(request))
                    .contentType("application/json")
                    .execute();

            TypeReference<BackendResponse<DirTreeResponse>> typeRef = new TypeReference<>() {};
            BackendResponse<DirTreeResponse> apiResponse = JSONUtil.toBean(response.body(), typeRef, false);

            if (apiResponse.getCode() != 200) {
                NotificationUtil.showErrorDialog(null, "创建失败: " + apiResponse.getMessage());
                return null;
            }
            return apiResponse.getData();
        } catch (Exception e) {
            NotificationUtil.showErrorDialog(null, "创建目录时发生异常: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    /**
     * 更新目录 (重命名或移动)
     */
    public Long updateDir(Long id, Long parentId, String name) {
        String url = AppConfig.BASE_URL + AppConfig.DIR_URL;
        try {
            UpdateDirRequest request = new UpdateDirRequest(id, parentId, name);
            HttpResponse response = HttpClientService.createPutRequest(url, true)
                    .body(JSONUtil.toJsonStr(request))
                    .contentType("application/json")
                    .execute();

            TypeReference<BackendResponse<Long>> typeRef = new TypeReference<>() {};
            BackendResponse<Long> apiResponse = JSONUtil.toBean(response.body(), typeRef, false);

            if (apiResponse.getCode() != 200) {
                NotificationUtil.showErrorDialog(null, "更新失败: " + apiResponse.getMessage());
                return null;
            }
            return apiResponse.getData();
        } catch (Exception e) {
            NotificationUtil.showErrorDialog(null, "更新目录时发生异常: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    public boolean deleteDir(Long id) {
        String url = AppConfig.BASE_URL + AppConfig.DIR_URL + "/" + id;
        try {
            HttpResponse response = HttpClientService.createDeleteRequest(url, true).execute();
            BackendResponse<?> apiResponse = JSONUtil.toBean(response.body(), BackendResponse.class);
            if (apiResponse.getCode() != 200) {
                NotificationUtil.showErrorDialog(null, "删除失败: " + apiResponse.getMessage());
                return false;
            }
            return true;
        } catch (Exception e) {
            NotificationUtil.showErrorDialog(null, "删除目录时发生异常: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
}