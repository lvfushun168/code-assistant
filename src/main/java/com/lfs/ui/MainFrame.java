package com.lfs.ui;

import cn.hutool.db.sql.SqlUtil;
import cn.hutool.json.JSONUtil;
import com.lfs.service.JavaToJsonService;
import com.lfs.service.JsonToJavaService;
import com.lfs.service.UserPreferencesService;
import com.lfs.util.NotificationUtil;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;
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

    private JMenuItem loginMenuItem;
    private JMenuItem logoutMenuItem;
    private JMenuItem signupMenuItem;
    private JCheckBoxMenuItem lineWrapMenuItem;

    public MainFrame() {
        this.preferencesService = new UserPreferencesService();
        initUI();
        updateAccountMenu();
    }

    public void updateAccountMenu() {
        boolean isLoggedIn = preferencesService.getToken() != null;
        loginMenuItem.setVisible(!isLoggedIn);
        logoutMenuItem.setVisible(isLoggedIn);
        signupMenuItem.setVisible(!isLoggedIn);
    }

    private void initUI() {
        // --- 窗口基础设置 ---
        setTitle("代码协作助手");

        // 设置任务栏和窗口图标
        try {
            // 使用PNG格式，因为Java对ICO格式的读取兼容性较差
            java.net.URL iconURL = getClass().getResource("/icons/app.png");
            if (iconURL != null) {
                java.awt.Image image = javax.imageio.ImageIO.read(iconURL);

                // 尝试使用Taskbar API (Java 9+)设置任务栏图标
                try {
                    final java.awt.Taskbar taskbar = java.awt.Taskbar.getTaskbar();
                    if (taskbar.isSupported(java.awt.Taskbar.Feature.ICON_IMAGE)) {
                        taskbar.setIconImage(image);
                    }
                } catch (UnsupportedOperationException e) {
                    // Taskbar API不受支持时，下面的 setIconImage 会作为备用方案
                }

                // 同时设置JFrame的图标，用于窗口左上角及Taskbar API不支持时的备用
                setIconImage(image);
            }
        } catch (Exception e) {
            // 图标加载失败, 打印错误但程序应继续运行
            e.printStackTrace();
        }
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        // --- 加载上次的窗口位置和大小 ---
        loadWindowPreferences();

        // --- 初始化 MVC 组件 ---
        this.controller = new MainFrameController(this);
        this.fileExplorerPanel = new FileExplorerPanel(this.controller);
        this.tabbedPane = new JTabbedPane();

        // 添加选项卡切换监听器
        this.tabbedPane.addChangeListener(e -> {
            Component selectedComponent = tabbedPane.getSelectedComponent();
            if (selectedComponent instanceof EditorPanel) {
                EditorPanel editorPanel = (EditorPanel) selectedComponent;
                lineWrapMenuItem.setSelected(editorPanel.getLineWrap());
                lineWrapMenuItem.setEnabled(true);
            } else {
                // 如果不是 EditorPanel，则禁用菜单项
                lineWrapMenuItem.setEnabled(false);
            }
        });

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

        // --- 创建“编辑”菜单 ---
        JMenu editMenu = new JMenu("<html><u>编辑</u></html>");
        JMenuItem saveAsMenuItem = new JMenuItem("另存为...");
        saveAsMenuItem.addActionListener(e -> controller.onSaveAs());
        editMenu.add(saveAsMenuItem);
        JMenuItem saveMenuItem = new JMenuItem("保存");
        saveMenuItem.addActionListener(e -> controller.saveCurrentFile());
        editMenu.add(saveMenuItem);

        editMenu.addSeparator();

        JMenuItem zoomInMenuItem = new JMenuItem("放大");
        zoomInMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_EQUALS, Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx()));
        zoomInMenuItem.addActionListener(e -> {
            Component activePanel = getActiveEditorPanel();
            if (activePanel instanceof EditorPanel) {
                ((EditorPanel) activePanel).zoomIn();
            }
        });
        editMenu.add(zoomInMenuItem);

        JMenuItem zoomOutMenuItem = new JMenuItem("缩小");
        zoomOutMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_MINUS, Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx()));
        zoomOutMenuItem.addActionListener(e -> {
            Component activePanel = getActiveEditorPanel();
            if (activePanel instanceof EditorPanel) {
                ((EditorPanel) activePanel).zoomOut();
            }
        });
        editMenu.add(zoomOutMenuItem);

        editMenu.addSeparator();

        lineWrapMenuItem = new JCheckBoxMenuItem("自动换行");
        lineWrapMenuItem.addActionListener(e -> {
            Component activePanel = getActiveEditorPanel();
            if (activePanel instanceof EditorPanel) {
                ((EditorPanel) activePanel).setLineWrap(lineWrapMenuItem.isSelected());
            }
        });
        editMenu.add(lineWrapMenuItem);

        // --- 创建“JSON”菜单 ---
        JMenu jsonMenu = new JMenu("<html><u>JSON</u></html>");
        JMenu trans2CodeMenu = new JMenu("JSON转代码..");
        JMenuItem json2java = new JMenuItem("转为Java对象");
        json2java.addActionListener(e -> {
            Component selectedComponent = getActiveEditorPanel();
            if (selectedComponent instanceof EditorPanel) {
                EditorPanel editorPanel = (EditorPanel) selectedComponent;
                String content = editorPanel.getTextArea().getSelectedText();
                if (content == null || content.isEmpty()) {
                    content = editorPanel.getTextAreaContent();
                }
                try {
                    JsonToJavaService jsonToJavaService = new JsonToJavaService();
                    String javaCode = jsonToJavaService.convert(content);
                    if (javaCode.startsWith("Error:")) {
                        NotificationUtil.showErrorDialog(this, javaCode.replace("Error: ", ""));
                        return;
                    }
                    if (editorPanel.getTextArea().getSelectedText() != null) {
                        editorPanel.getTextArea().replaceSelection(javaCode);
                    } else {
                        editorPanel.setTextAreaContent(javaCode);
                    }
                } catch (Exception ex) {
                    NotificationUtil.showErrorDialog(this, "转换失败: " + ex.getMessage());
                }
            }
        });
        trans2CodeMenu.add(json2java);
        JMenu trans2JsonMenu = new JMenu("代码转JSON..");
        JMenuItem bean2JSon = new JMenuItem("Java对象转为JSON");
        bean2JSon.addActionListener(e -> {
            Component selectedComponent = getActiveEditorPanel();
            if (selectedComponent instanceof EditorPanel) {
                EditorPanel editorPanel = (EditorPanel) selectedComponent;
                String content = editorPanel.getTextArea().getSelectedText();
                if (content == null || content.isEmpty()) {
                    content = editorPanel.getTextAreaContent();
                }
                try {
                    JavaToJsonService javaToJsonService = new JavaToJsonService();
                    String json = javaToJsonService.transformJavaBean(content);
                    if (editorPanel.getTextArea().getSelectedText() != null) {
                        editorPanel.getTextArea().replaceSelection(json);
                    } else {
                        editorPanel.setTextAreaContent(json);
                    }
                } catch (Exception ex) {
                    NotificationUtil.showErrorDialog(this, "转换失败: " + ex.getMessage());
                }
            }
        });
        trans2JsonMenu.add(bean2JSon);
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
        // --- 创建“SQL”菜单 ---
        JMenu sqlMenu = new JMenu("<html><u>SQL</u></html>");
        JMenuItem sqlFormatMenuItem = new JMenuItem("美化SQL");
        sqlFormatMenuItem.addActionListener(e -> {
            Component selectedComponent = getActiveEditorPanel();
            if (selectedComponent instanceof EditorPanel) {
                EditorPanel editorPanel = (EditorPanel) selectedComponent;
                String sql = editorPanel.getTextAreaContent();
                try {
                    String formattedSql = SqlUtil.formatSql(sql);
                    editorPanel.setTextAreaContent(formattedSql);
                } catch (Exception ex) {
                    NotificationUtil.showErrorDialog(this,"SQL 格式不正确");
                }
            }
        });
        sqlMenu.add(sqlFormatMenuItem);

        // --- 创建“账户”菜单 ---
        JMenu accountMenu = new JMenu("<html><u>账户</u></html>");
        signupMenuItem = new JMenuItem("注册...");
        signupMenuItem.addActionListener(e -> {
            RegisterDialog registerDialog = new RegisterDialog(this);
            registerDialog.setVisible(true);
        });
        accountMenu.add(signupMenuItem);
        loginMenuItem = new JMenuItem("登入...");
        loginMenuItem.addActionListener(e -> {
            LoginDialog loginDialog = new LoginDialog(this);
            loginDialog.setVisible(true);
        });
        accountMenu.add(loginMenuItem);
        logoutMenuItem = new JMenuItem("登出...");
        logoutMenuItem.addActionListener(e -> {
            int response = JOptionPane.showConfirmDialog(this, "您确定要登出吗？", "确认", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
            if (response == JOptionPane.YES_OPTION) {
                preferencesService.clearToken();
                updateAccountMenu();
            }
        });
        accountMenu.add(logoutMenuItem);

        jsonMenu.add(formatJsonMenuItem);
        jsonMenu.add(trans2CodeMenu);
        jsonMenu.add(trans2JsonMenu);

        // --- 将菜单添加到菜单栏 ---
        menuBar.add(project);
        menuBar.add(editMenu);
        menuBar.add(jsonMenu);
        menuBar.add(sqlMenu);
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

    public FileExplorerPanel getFileExplorerPanel() {
        return fileExplorerPanel;
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
        newEditorPanel.getTextArea().setCaretPosition(0);

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
