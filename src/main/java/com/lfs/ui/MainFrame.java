package com.lfs.ui;

import cn.hutool.json.JSONUtil;
import com.lfs.service.UserPreferencesService;
import com.lfs.util.NotificationUtil;

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

        // --- 创建“JSON”菜单 ---
        JMenu jsonMenu = new JMenu("<html><u>JSON</u></html>");
        JMenu trans2CodeMenu = new JMenu("JSON转代码..");
        JMenuItem json2java = new JMenuItem("转为Java对象");
        trans2CodeMenu.add(json2java);
        JMenu trans2JsonMenu = new JMenu("代码转JSON..");
        JMenuItem formatJsonMenuItem = new JMenuItem("格式化JSON");
        formatJsonMenuItem.addActionListener(e -> {
            Component selectedComponent = getActiveEditorPanel();
            if (selectedComponent instanceof EditorPanel) {
                EditorPanel editorPanel = (EditorPanel) selectedComponent;
                String content = editorPanel.getTextAreaContent();
                try {
                    String formattedJson = cn.hutool.json.JSONUtil.formatJsonStr(content);
                    editorPanel.setTextAreaContent(formattedJson);
                } catch (Exception ex) {
                    NotificationUtil.showErrorDialog(this,"JSON 格式不正确");
                }
            }
        });
        // --- 创建“账户”菜单 ---
        JMenu accountMenu = new JMenu("<html><u>云账户</u></html>");
        JMenuItem signupMenuItem = new JMenuItem("注册...");
        accountMenu.add(signupMenuItem);
        JMenuItem loginMenuItem = new JMenuItem("登入...");
        accountMenu.add(loginMenuItem);
        JMenuItem logoutMenuItem = new JMenuItem("登出...");
        accountMenu.add(logoutMenuItem);

        jsonMenu.add(formatJsonMenuItem);
        jsonMenu.add(trans2CodeMenu);
        jsonMenu.add(trans2JsonMenu);

        // --- 将菜单添加到菜单栏 ---
        menuBar.add(project);
        menuBar.add(saveMenu);
        menuBar.add(importMenu);
        menuBar.add(jsonMenu);
        menuBar.add(accountMenu);

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
        addTab(file.getName(), newEditorPanel);
    }

    public void openFileInTabReadOnly(File file) {
        // 检查是否已经打开
        for (int i = 0; i < tabbedPane.getTabCount(); i++) {
            Component tabComponent = tabbedPane.getComponentAt(i);
            if (tabComponent instanceof VirtualEditorPanel) {
                VirtualEditorPanel panel = (VirtualEditorPanel) tabComponent;
                if (file.equals(panel.getCurrentFile())) {
                    tabbedPane.setSelectedIndex(i); // 切换到已存在的选项卡
                    return;
                }
            }
        }

        // 创建新的 VirtualEditorPanel
        VirtualEditorPanel newEditorPanel = new VirtualEditorPanel();
        newEditorPanel.loadFile(file);

        // 添加到 tabbedPane
        addTab(file.getName(), newEditorPanel);
    }

    public void openBigFileInTab(File file) {
        // 检查是否已经打开
        for (int i = 0; i < tabbedPane.getTabCount(); i++) {
            Component tabComponent = tabbedPane.getComponentAt(i);
            // 一个更健壮的检查方法是为保存文件的面板提供一个通用接口
            if (tabComponent instanceof LargeFileEditorPanel) {
                String tabTitle = tabbedPane.getTitleAt(i);
                if (file.getName().equals(tabTitle)) {
                    tabbedPane.setSelectedIndex(i); // 切换到已存在的选项卡
                    return;
                }
            }
        }

        // 创建新的 LargeFileEditorPanel
        LargeFileEditorPanel newViewerPanel = new LargeFileEditorPanel();
        newViewerPanel.loadFile(file);

        // 添加到 tabbedPane
        addTab(file.getName(), newViewerPanel);
    }

    public void openFile(File file) {
        if (file != null && file.exists() && file.isFile()) {
            controller.onFileSelected(file);
        }
    }

    public Component getActiveEditorPanel() {
        return tabbedPane.getSelectedComponent();
    }

    private void addTab(String title, Component panel) {
        tabbedPane.addTab(title, panel);
        int newTabIndex = tabbedPane.getTabCount() - 1;
        tabbedPane.setTabComponentAt(newTabIndex, new ButtonTabComponent(tabbedPane));
        tabbedPane.setSelectedIndex(newTabIndex);
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
