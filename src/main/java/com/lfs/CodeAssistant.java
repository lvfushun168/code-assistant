package com.lfs;

import com.formdev.flatlaf.FlatDarculaLaf;
import com.lfs.ui.LoadingSpinner;
import com.lfs.ui.MainFrame;

import javax.swing.*;
import java.awt.*;
import java.util.List;

public class CodeAssistant {

    public static void main(String[] args) {
        // 1. 在创建任何Swing组件之前设置观感
        FlatDarculaLaf.setup();

        // 2. 创建并显示包含加载动画的启动窗口
        final JWindow splash = new JWindow();
        LoadingSpinner spinner = new LoadingSpinner();
        spinner.setPreferredSize(new Dimension(100, 100)); // 给加载动画一个合适的尺寸
        splash.getContentPane().add(spinner);
        splash.pack();
        splash.setLocationRelativeTo(null); // 屏幕居中
        splash.setVisible(true);

        // 3. 在事件调度线程中初始化主UI
        SwingUtilities.invokeLater(() -> {
            System.setProperty("sun.jnu.encoding", "UTF-8");

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

            // 4. 显示主窗口并关闭启动画面
            mainFrame.setVisible(true);
            splash.dispose();

            // 5. 懒加载文件浏览器
            mainFrame.getFileExplorerPanel().lazyLoadInitialDirectory(null);

            // 6. 处理通过命令行参数传递的文件
            if (args.length > 0) {
                mainFrame.openFile(new java.io.File(args[0]));
            }
        });
    }
}
