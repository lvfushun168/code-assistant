package com.lfs.util;

import javax.swing.*;
import java.awt.*;
import java.util.Timer;
import java.util.TimerTask;

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

    /**
     * 显示保存成功的对话框
     * @param parentComponent 父组件
     */
    public static void showSaveSuccess(Component parentComponent) {
        showToast(parentComponent, "文件保存成功！");
    }

    /**
     * 显示一个短暂的提示消息 (浮动提示)
     * @param parentComponent 父组件
     * @param message 消息内容
     */
    public static void showToast(Component parentComponent, String message) {
        final JDialog dialog = new JDialog();
        dialog.setUndecorated(true);
        dialog.setAlwaysOnTop(true);
        dialog.setFocusableWindowState(false);

        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));
        panel.setBackground(new Color(0, 0, 0, 180));
        JLabel label = new JLabel(message);
        label.setForeground(Color.WHITE);
        label.setFont(new Font("Arial", Font.BOLD, 14));
        panel.add(label, BorderLayout.CENTER);
        dialog.add(panel);

        dialog.pack();

        if (parentComponent != null) {
            int parentX = parentComponent.getX();
            int parentY = parentComponent.getY();
            int parentWidth = parentComponent.getWidth();
            int dialogWidth = dialog.getWidth();
            dialog.setLocation(parentX + (parentWidth - dialogWidth) / 2, parentY + 30);
        } else {
            Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
            int screenWidth = screenSize.width;
            int dialogWidth = dialog.getWidth();
            dialog.setLocation((screenWidth - dialogWidth) / 2, 50);
        }

        dialog.setVisible(true);

        Timer timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                dialog.setVisible(false);
                dialog.dispose();
            }
        }, 2000); // 2秒后自动关闭
    }
}