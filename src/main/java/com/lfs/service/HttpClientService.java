package com.lfs.service;

import cn.hutool.http.HttpRequest;

public class HttpClientService {

    private static final UserPreferencesService prefsService = new UserPreferencesService();

    private static HttpRequest applyAuth(HttpRequest request) {
        String token = prefsService.getToken();
        if (token != null) {
            return request.header("Authorization", "Bearer " + token);
        }
        return request;
    }

    public static HttpRequest createGetRequest(String url) {
        return applyAuth(HttpRequest.get(url));
    }

    public static HttpRequest createPostRequest(String url) {
        return applyAuth(HttpRequest.post(url));
    }

    // You can add other methods like put, delete etc. as needed
}
