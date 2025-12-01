package com.lfs.service;

import cn.hutool.http.HttpRequest;

import javax.net.ssl.*;
import java.security.cert.X509Certificate;

public class HttpClientService {

    private static final UserPreferencesService prefsService = new UserPreferencesService();
    private static final SSLSocketFactory TRUST_ALL_SSL_FACTORY;
    private static final HostnameVerifier TRUST_ALL_HOSTNAME_VERIFIER;

    static {
        try {
            // 创建一个信任所有证书的 TrustManager
            TrustManager[] trustAllCerts = new TrustManager[]{
                    new X509TrustManager() {
                        public X509Certificate[] getAcceptedIssuers() {
                            return null;
                        }

                        public void checkClientTrusted(X509Certificate[] certs, String authType) {
                        }

                        public void checkServerTrusted(X509Certificate[] certs, String authType) {
                        }
                    }
            };

            // 安装这个 all-trusting trust manager
            SSLContext sc = SSLContext.getInstance("SSL");
            sc.init(null, trustAllCerts, new java.security.SecureRandom());
            TRUST_ALL_SSL_FACTORY = sc.getSocketFactory();

            // 创建一个信任所有主机的 HostnameVerifier
            TRUST_ALL_HOSTNAME_VERIFIER = (hostname, session) -> true;

        } catch (Exception e) {
            throw new RuntimeException("创建信任所有SSL工厂或主机名验证器失败", e);
        }
    }


    private static HttpRequest applyAuth(HttpRequest request, Boolean carryToken) {
        String token = prefsService.getToken();
        if (carryToken && token != null) {
            return request.header("Authorization", "Bearer " + token);
        }
        return request;
    }

    /**
     * 为HTTPS请求设置信任所有证书和主机名
     *
     * @param request HttpRequest
     */
    private static void applySsl(HttpRequest request) {
        if (request.getUrl().startsWith("https")) {
            request.setSSLSocketFactory(TRUST_ALL_SSL_FACTORY);
            request.setHostnameVerifier(TRUST_ALL_HOSTNAME_VERIFIER);
        }
    }

    public static HttpRequest createGetRequest(String url, Boolean carryToken) {
        HttpRequest request = HttpRequest.get(url);
        applySsl(request);
        return applyAuth(request, carryToken);
    }

    public static HttpRequest createPostRequest(String url) {
        HttpRequest request = HttpRequest.post(url);
        applySsl(request);
        return applyAuth(request, true);
    }

    public static HttpRequest createPostRequest(String url, Boolean carryToken) {
        HttpRequest request = HttpRequest.post(url);
        applySsl(request);
        return applyAuth(request, carryToken);
    }

    public static HttpRequest createPutRequest(String url, Boolean carryToken) {
        HttpRequest request = HttpRequest.put(url);
        applySsl(request);
        return applyAuth(request, carryToken);
    }

    public static HttpRequest createDeleteRequest(String url, Boolean carryToken) {
        HttpRequest request = HttpRequest.delete(url);
        applySsl(request);
        return applyAuth(request, carryToken);
    }
}

