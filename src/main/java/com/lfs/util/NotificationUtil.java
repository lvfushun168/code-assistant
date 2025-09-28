package com.lfs.util;

import javax.swing.*;
import java.awt.Component;

/**
 * 用于显示标准化对话框消息的工具类。
 */
public class NotificationUtil {

    private NotificationUtil() {

    }

    /**
     * 居中显示的成功对话框
     * @param message 提示信息
     */
    public static void showSuccessDialog(Component parentComponent, String message) {
        JOptionPane.showMessageDialog(
                parentComponent,
                message,
                "操作成功",
                JOptionPane.INFORMATION_MESSAGE
        );
    }

    /**
     * 居中显示的错误对话框
     * @param message 错误信息
     */
    public static void showErrorDialog(Component parentComponent, String message) {
        JOptionPane.showMessageDialog(
                parentComponent,
                message,
                "错误",
                JOptionPane.ERROR_MESSAGE
        );
    }
}
