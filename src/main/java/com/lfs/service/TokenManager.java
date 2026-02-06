package com.lfs.service;

import com.lfs.ui.MainFrame;

import javax.swing.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Token管理器，负责处理token过期事件
 */
public class TokenManager {

    private static final List<TokenExpiredListener> listeners = new ArrayList<>();

    /**
     * 注册Token过期监听器
     */
    public static void addTokenExpiredListener(TokenExpiredListener listener) {
        listeners.add(listener);
    }

    /**
     * 触发Token过期事件
     */
    public static void notifyTokenExpired() {
        // 清除过期的token
        UserPreferencesService prefsService = new UserPreferencesService();
        prefsService.clearToken();

        // 通知所有监听器
        SwingUtilities.invokeLater(() -> {
            for (TokenExpiredListener listener : listeners) {
                listener.onTokenExpired();
            }
        });
    }

    /**
     * Token过期监听器接口
     */
    public interface TokenExpiredListener {
        void onTokenExpired();
    }
}