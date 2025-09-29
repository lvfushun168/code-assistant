package com.lfs.ui;

import com.lfs.service.ClipboardService;
import com.lfs.service.FileProcessorService;
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

    private JTextArea rightTextArea;
    private final FileProcessorService fileProcessorService;
    private final UserPreferencesService preferencesService;

    public MainFrame() {
        this.fileProcessorService = new FileProcessorService();
        this.preferencesService = new UserPreferencesService();
        initUI();
    }

    private void initUI() {
        // --- 窗口基础设置 ---
        setTitle("代码协作助手");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        // --- 加载上次的窗口位置和大小 ---
        loadWindowPreferences();

        // --- UI 组件创建 ---
        JButton selectContentButton = new JButton("获取代码内容");
        selectContentButton.setFont(new Font("SansSerif", Font.PLAIN, 16));

        JButton getStructureButton = new JButton("获取项目结构");
        getStructureButton.setFont(new Font("SansSerif", Font.PLAIN, 16));

        // --- 竖向布局 ---
        selectContentButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        getStructureButton.setAlignmentX(Component.CENTER_ALIGNMENT);

        // --- 事件监听 ---
        selectContentButton.addActionListener(e -> onProcessDirectory(true));
        getStructureButton.addActionListener(e -> onProcessDirectory(false));

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                preferencesService.saveWindowBounds(getBounds());
            }
        });

        // --- 布局设置 ---
        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.Y_AXIS));
        buttonPanel.add(selectContentButton);
        buttonPanel.add(Box.createRigidArea(new Dimension(0, 20)));
        buttonPanel.add(getStructureButton);

        // --- 创建左侧面板用于居中按钮 ---
        JPanel leftPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.anchor = GridBagConstraints.CENTER;
        gbc.fill = GridBagConstraints.NONE;
        leftPanel.add(buttonPanel, gbc);

        // --- 创建右侧文本区域 ---
        JTextArea rightTextArea = new JTextArea();
        rightTextArea.setEditable(true);
        rightTextArea.setLineWrap(true);
        rightTextArea.setWrapStyleWord(true);
        JScrollPane rightScrollPane = new JScrollPane(rightTextArea);
        this.rightTextArea = rightTextArea;

        // --- 创建右侧面板 ---
        JPanel rightPanel = new JPanel(new BorderLayout());

        // --- 创建菜单栏 ---
        JMenuBar menuBar = new JMenuBar();

        // --- 创建“保存”菜单 ---
        JMenu saveMenu = new JMenu("保存");
        JMenuItem saveAsItem = new JMenuItem("另存为...");
        saveAsItem.addActionListener(e -> onSaveAs());
        saveMenu.add(saveAsItem);

        // --- 创建“导入”菜单 ---
        JMenu importMenu = new JMenu("导入");

        // --- 将菜单添加到菜单栏 ---
        menuBar.add(saveMenu);
        menuBar.add(importMenu);

        // --- 将菜单栏和文本区域添加到右侧面板 ---
        rightPanel.add(menuBar, BorderLayout.NORTH);
        rightPanel.add(rightScrollPane, BorderLayout.CENTER);

        // --- 创建JSplitPane ---
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, leftPanel, rightPanel);
        splitPane.setOneTouchExpandable(true);
        splitPane.setDividerLocation(200); // 初始分割位置

        // --- 主窗口布局 ---
        setLayout(new BorderLayout());
        add(splitPane, BorderLayout.CENTER);
    }

    private void onSaveAs() {
        String[] options = {"云端", "本地"};
        int choice = JOptionPane.showOptionDialog(this, "请选择保存方式", "保存",
                JOptionPane.YES_NO_OPTION, JOptionPane.PLAIN_MESSAGE, null, options, options[1]);

        if (choice == 1) { // 本地
            JFileChooser fileChooser = new JFileChooser();
            fileChooser.setDialogTitle("选择保存目录");
            fileChooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("Text Files (*.txt)", "txt"));
            fileChooser.addChoosableFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("Markdown Files (*.md)", "md"));
            fileChooser.addChoosableFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("YAML Files (*.yml)", "yml"));
            fileChooser.addChoosableFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("XML Files (*.xml)", "xml"));

            int userSelection = fileChooser.showSaveDialog(this);

            if (userSelection == JFileChooser.APPROVE_OPTION) {
                File fileToSave = fileChooser.getSelectedFile();
                String filePath = fileToSave.getAbsolutePath();
                if (!filePath.endsWith("." + ((javax.swing.filechooser.FileNameExtensionFilter) fileChooser.getFileFilter()).getExtensions()[0])) {
                    filePath += "." + ((javax.swing.filechooser.FileNameExtensionFilter) fileChooser.getFileFilter()).getExtensions()[0];
                    fileToSave = new File(filePath);
                }

                try {
                    fileProcessorService.saveStringToFile(rightTextArea.getText(), fileToSave);
                    NotificationUtil.showSuccessDialog(this, "文件已保存到: " + fileToSave.getAbsolutePath());
                } catch (Exception ex) {
                    NotificationUtil.showErrorDialog(this, "保存文件时出错: " + ex.getMessage());
                    ex.printStackTrace();
                }
            }
        }
    }

    /**
     * 处理“获取内容”和“获取结构”按钮点击的逻辑。
     * @param isContentMode 获取内容时为true，获取结构时为false。
     */
    private void onProcessDirectory(boolean isContentMode) {
        String dialogTitle = isContentMode ? "请选择一个项目文件夹以读取内容" : "请选择一个项目文件夹以获取结构";
        JFileChooser fileChooser = createConfiguredFileChooser(dialogTitle);
        int result = fileChooser.showOpenDialog(this);

        if (result == JFileChooser.APPROVE_OPTION) {
            File selectedDirectory = fileChooser.getSelectedFile();
            preferencesService.saveLastDirectory(selectedDirectory);

            // 显示加载指示器（可选，但有利于用户体验）
            setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));

            // 在后台线程中执行处理以保持UI的响应性。
            new Thread(() -> {
                try {
                    final String processedResult;
                    if (isContentMode) {
                        processedResult = fileProcessorService.generateContentFromDirectory(selectedDirectory);
                    } else {
                        processedResult = fileProcessorService.generateStructureFromDirectory(selectedDirectory);
                    }

                    // 返回UI线程进行用户交互
                    SwingUtilities.invokeLater(() -> {
                        int choice = JOptionPane.showOptionDialog(this, "是否生成文件？", "确认",
                                JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE, null, new String[]{" 是 ", " 否 "}, "是");

                        if (choice == JOptionPane.YES_OPTION) {
                            // 用户选择保存文件
                            JFileChooser fileChooserSave = new JFileChooser();
                            fileChooserSave.setDialogTitle("选择保存目录");
                            fileChooserSave.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
                            fileChooserSave.setCurrentDirectory(preferencesService.getLastDirectory()); // 使用上次的目录

                            int saveResult = fileChooserSave.showSaveDialog(this);
                            if (saveResult == JFileChooser.APPROVE_OPTION) {
                                File saveDir = fileChooserSave.getSelectedFile();
                                String fileName = isContentMode ? "code.txt" : "structure.txt";
                                File outputFile = new File(saveDir, fileName);

                                // 在工作线程中执行文件写入
                                new Thread(() -> {
                                    try {
                                        fileProcessorService.saveStringToFile(processedResult, outputFile);
                                        SwingUtilities.invokeLater(() -> NotificationUtil.showSuccessDialog(this, "文件已保存到: " + outputFile.getAbsolutePath()));
                                    } catch (Exception ex) {
                                        SwingUtilities.invokeLater(() -> NotificationUtil.showErrorDialog(this, "保存文件时出错: " + ex.getMessage()));
                                        ex.printStackTrace();
                                    } finally {
                                        setCursor(Cursor.getDefaultCursor());
                                    }
                                }).start();
                            } else {
                                // 用户取消了保存
                                setCursor(Cursor.getDefaultCursor());
                            }
                        } else {
                            // 用户选择不保存文件，则复制到剪贴板
                            ClipboardService.copyToClipboard(processedResult);
                            String successMessage = isContentMode ? "内容已粘贴到剪切板" : "项目结构已粘贴到剪切板";
                            NotificationUtil.showSuccessDialog(this, successMessage);
                            setCursor(Cursor.getDefaultCursor());
                        }
                    });

                } catch (Exception ex) {
                    SwingUtilities.invokeLater(() -> {
                        NotificationUtil.showErrorDialog(this, "处理时发生错误: " + ex.getMessage());
                        setCursor(Cursor.getDefaultCursor());
                    });
                    ex.printStackTrace();
                }
                // finally 块被移到各个分支内部，以确保光标在正确的时间恢复
            }).start();
        }
    }

    /**
     * 创建并配置一个JFileChooser实例。
     * @param dialogTitle 对话框标题
     * @return 配置好的 JFileChooser 实例
     */
    private JFileChooser createConfiguredFileChooser(String dialogTitle) {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        fileChooser.setDialogTitle(dialogTitle);
        fileChooser.setFileHidingEnabled(false); // Show hidden files

        File lastDir = preferencesService.getLastDirectory();
        if (lastDir != null) {
            fileChooser.setCurrentDirectory(lastDir);
        }
        return fileChooser;
    }

    /**
     * 从 Preferences 加载窗口的大小和位置
     */
    private void loadWindowPreferences() {
        Rectangle bounds = preferencesService.loadWindowBounds(650, 400);
        setSize(bounds.width, bounds.height);

        if (bounds.x != -1 && bounds.y != -1) {
            setLocation(bounds.x, bounds.y);
        } else {
            setLocationRelativeTo(null); // 如果没有保存的位置，则居中
        }
    }
}
