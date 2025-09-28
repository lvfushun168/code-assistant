package com.lfs;

import com.formdev.flatlaf.FlatDarculaLaf;
import com.lfs.ui.MainFrame;

import javax.swing.*;

public class CodeAssistant {

    public static void main(String[] args) {
        // 设置 FlatLaf 深色主题
        FlatDarculaLaf.setup();

        // 使用 SwingUtilities.invokeLater 来确保UI操作在事件调度线程中执行
        SwingUtilities.invokeLater(() -> {
            MainFrame mainFrame = new MainFrame();
            mainFrame.setVisible(true);
        });
    }
}
