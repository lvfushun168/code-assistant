package com.lfs;

import com.formdev.flatlaf.FlatDarculaLaf;
import com.lfs.ui.MainFrame;

import javax.swing.*;
import java.util.List;

public class CodeAssistant {

    public static void main(String[] args) {
        System.setProperty("sun.jnu.encoding", "UTF-8");
        // 设置 FlatLaf 深色主题
        FlatDarculaLaf.setup();

        // 主窗口实例
        final MainFrame mainFrame = new MainFrame();

        // 检查是否支持桌面API以及文件打开操作
        if (java.awt.Desktop.isDesktopSupported()) {
            java.awt.Desktop desktop = java.awt.Desktop.getDesktop();
            if (desktop.isSupported(java.awt.Desktop.Action.APP_OPEN_FILE)) {
                desktop.setOpenFileHandler(e -> {
                    List<java.io.File> files = e.getFiles();
                    for (java.io.File file : files) {
                        SwingUtilities.invokeLater(() -> mainFrame.openFile(file));
                    }
                });
            }
        }

        // 使用 SwingUtilities.invokeLater 来确保UI操作在事件调度线程中执行
        SwingUtilities.invokeLater(() -> {
            mainFrame.setVisible(true);
            // 处理通过命令行参数传递的文件
            if (args.length > 0) {
                mainFrame.openFile(new java.io.File(args[0]));
            }
        });
    }
}
