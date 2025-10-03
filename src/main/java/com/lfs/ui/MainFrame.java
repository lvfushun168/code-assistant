package com.lfs.ui;

import com.lfs.service.UserPreferencesService;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import java.io.File;

/**
 * 主框架显示UI
 */
public class MainFrame extends JFrame {

    private final UserPreferencesService preferencesService;
    private MainFrameController controller;
    private EditorPanel editorPanel;
    private FileExplorerPanel fileExplorerPanel;


    public MainFrame() {
        this.preferencesService = new UserPreferencesService();
        initUI();
    }

    private void initUI() {
        // --- 窗口基础设置 ---
        setTitle("代码协作助手");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        // --- 加载上次的窗口位置和大小 ---
        loadWindowPreferences();

        // --- 初始化 MVC 组件 ---
        this.controller = new MainFrameController(this, null);
        this.editorPanel = new EditorPanel(this.controller);
        this.controller.setEditorPanel(this.editorPanel); // Set the correct panel instance on the controller
        this.fileExplorerPanel = new FileExplorerPanel(this.controller);


        // --- 创建JSplitPane ---
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, fileExplorerPanel, editorPanel);
        splitPane.setOneTouchExpandable(true);
        splitPane.setDividerLocation(200); // 初始分割位置

        // --- 主窗口布局 ---
        setLayout(new BorderLayout());
        add(splitPane, BorderLayout.CENTER);

        // --- 窗口事件监听 ---
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                preferencesService.saveWindowBounds(getBounds());
            }
        });
    }

    /**
     * 从 Preferences 加载窗口的大小和位置
     */
    private void loadWindowPreferences() {
        Rectangle bounds = preferencesService.loadWindowBounds();
        if (bounds != null && bounds.width > 0 && bounds.height > 0) {
            setBounds(bounds);
        } else {
            // 首次打开时，设置窗口大小为屏幕的3/5，并居中
            Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
            int width = screenSize.width * 3 / 5;
            int height = screenSize.height * 3 / 5;
            setSize(width, height);
            setLocationRelativeTo(null); // 居中
        }
    }
}
