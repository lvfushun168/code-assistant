package com.lfs.service;

import cn.hutool.core.lang.TypeReference;
import cn.hutool.json.JSONUtil;
import com.lfs.config.AppConfig;
import com.lfs.domain.ApiResponse;
import com.lfs.domain.DirTreeResponse;
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
            String responseBody = httpClientService.createGetRequest(url, true).execute().body();
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
}
