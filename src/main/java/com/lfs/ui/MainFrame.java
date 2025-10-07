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
    private JTabbedPane tabbedPane;
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
        this.controller = new MainFrameController(this);
        this.fileExplorerPanel = new FileExplorerPanel(this.controller);
        this.tabbedPane = new JTabbedPane();

        // --- 创建右侧面板，包含菜单栏和选项卡面板 ---
        JPanel rightPanel = new JPanel(new BorderLayout());

        // --- 创建菜单栏 ---
        JMenuBar menuBar = new JMenuBar();
        menuBar.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, Color.LIGHT_GRAY));

        // --- 创建“项目”菜单 ---
        JMenu project = new JMenu("<html><u>项目</u></html>");
        JMenuItem getContentMenuItem = new JMenuItem("获取代码内容");
        getContentMenuItem.addActionListener(e -> controller.onProcessDirectory(true));
        project.add(getContentMenuItem);
        JMenuItem getStructureMenuItem = new JMenuItem("获取项目结构");
        getStructureMenuItem.addActionListener(e -> controller.onProcessDirectory(false));
        project.add(getStructureMenuItem);

        // --- 创建“保存”菜单 ---
        JMenu saveMenu = new JMenu("<html><u>保存</u></html>");
        JMenuItem saveAsMenuItem = new JMenuItem("另存为...");
        saveAsMenuItem.addActionListener(e -> controller.onSaveAs());
        saveMenu.add(saveAsMenuItem);
        JMenuItem saveMenuItem = new JMenuItem("保存");
        saveMenuItem.addActionListener(e -> controller.saveCurrentFile());
        saveMenu.add(saveMenuItem);

        // --- 创建“导入”菜单 ---
        JMenu importMenu = new JMenu("<html><u>导入</u></html>");

        // --- 将菜单添加到菜单栏 ---
        menuBar.add(project);
        menuBar.add(saveMenu);
        menuBar.add(importMenu);

        rightPanel.add(menuBar, BorderLayout.NORTH);
        rightPanel.add(tabbedPane, BorderLayout.CENTER);

        // --- 创建JSplitPane ---
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, fileExplorerPanel, rightPanel);
        splitPane.setOneTouchExpandable(true);
        splitPane.setDividerLocation(250); // 初始分割位置

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

    public void openFileInTab(File file, String content) {
        // 检查是否已经打开
        for (int i = 0; i < tabbedPane.getTabCount(); i++) {
            Component tabComponent = tabbedPane.getComponentAt(i);
            if (tabComponent instanceof EditorPanel) {
                EditorPanel panel = (EditorPanel) tabComponent;
                if (file.equals(panel.getCurrentFile())) {
                    tabbedPane.setSelectedIndex(i); // 切换到已存在的选项卡
                    return;
                }
            }
        }

        // 创建新的 EditorPanel
        EditorPanel newEditorPanel = new EditorPanel(controller);
        newEditorPanel.setCurrentFile(file);
        newEditorPanel.setTextAreaContent(content);

        // 添加到 tabbedPane
        tabbedPane.addTab(file.getName(), newEditorPanel);
        int newTabIndex = tabbedPane.getTabCount() - 1;
        tabbedPane.setTabComponentAt(newTabIndex, new ButtonTabComponent(tabbedPane));
        tabbedPane.setSelectedIndex(newTabIndex);
    }

    public EditorPanel getActiveEditorPanel() {
        Component selectedComponent = tabbedPane.getSelectedComponent();
        if (selectedComponent instanceof EditorPanel) {
            return (EditorPanel) selectedComponent;
        }
        return null;
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
