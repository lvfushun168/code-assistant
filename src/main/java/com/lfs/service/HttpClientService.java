package com.lfs.service;

import cn.hutool.http.HttpRequest;

public class HttpClientService {

    private static final UserPreferencesService prefsService = new UserPreferencesService();

    private static HttpRequest applyAuth(HttpRequest request, Boolean carryToken) {
        String token = prefsService.getToken();
        if (carryToken && token != null) {
            return request.header("Authorization", "Bearer " + token);
        }
        return request;
    }

    public static HttpRequest createGetRequest(String url, Boolean carryToken) {
        return applyAuth(HttpRequest.get(url), carryToken);
    }

    public static HttpRequest createPostRequest(String url) {
        return applyAuth(HttpRequest.post(url), true);
    }

    public static HttpRequest createPostRequest(String url, Boolean carryToken) {
        return applyAuth(HttpRequest.post(url), carryToken);
    }

}
