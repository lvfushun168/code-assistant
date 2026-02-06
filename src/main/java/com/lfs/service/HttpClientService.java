package com.lfs.service;

import cn.hutool.http.HttpRequest;

import javax.net.ssl.*;
import java.net.Proxy;
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
            SSLContext sc = SSLContext.getInstance("TLSv1.2");
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
     * 强制无代理并信任所有SSL
     * @param request HttpRequest
     */
    private static void configureRequest(HttpRequest request) {
        // 1. 强制不使用任何代理，直接连接
        request.setProxy(Proxy.NO_PROXY);

        // 2. 为HTTPS请求设置信任所有证书和主机名
        if (request.getUrl().startsWith("https")) {
            request.setSSLSocketFactory(TRUST_ALL_SSL_FACTORY);
            request.setHostnameVerifier(TRUST_ALL_HOSTNAME_VERIFIER);
        }
    }

    public static HttpRequest createGetRequest(String url, Boolean carryToken) {
        HttpRequest request = HttpRequest.get(url);
        configureRequest(request);
        return applyAuth(request, carryToken);
    }

    public static HttpRequest createPostRequest(String url) {
        HttpRequest request = HttpRequest.post(url);
        configureRequest(request);
        return applyAuth(request, true);
    }

    public static HttpRequest createPostRequest(String url, Boolean carryToken) {
        HttpRequest request = HttpRequest.post(url);
        configureRequest(request);
        return applyAuth(request, carryToken);
    }

    public static HttpRequest createPutRequest(String url, Boolean carryToken) {
        HttpRequest request = HttpRequest.put(url);
        configureRequest(request);
        return applyAuth(request, carryToken);
    }

    public static HttpRequest createDeleteRequest(String url, Boolean carryToken) {
        HttpRequest request = HttpRequest.delete(url);
        configureRequest(request);
        return applyAuth(request, carryToken);
    }

    /**
     * 检查响应状态，如果是401则抛出TokenExpiredException
     * 此方法需要在execute()之后调用
     */
    public static void checkResponseStatus(cn.hutool.http.HttpResponse response) {
        int status = response.getStatus();
        if (status == 401) {
            throw new TokenExpiredException("登陆已过期，请重新登录");
        }
    }
}


