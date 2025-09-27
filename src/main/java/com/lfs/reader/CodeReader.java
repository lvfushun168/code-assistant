package com.lfs.reader;

import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * 一个现代UI风格的GUI小工具，用于读取代码项目文件夹内容到剪切板。
 */
public class CodeReader extends JFrame {

    // 定义需要读取内容的文件扩展名集合
    private static final Set<String> ALLOWED_EXTENSIONS = new HashSet<>(Arrays.asList(
            "java", "py", "python", "html", "htm", "css", "js", "ts",
            "yml", "yaml", "properties", "conf", "config", "application",
            "txt", "text", "md", "sql", "xml", "json", "sh", "bat"
    ));

    public CodeReader() {
        initUI();
    }

    private void initUI() {
        // --- 窗口基础设置 ---
        setTitle("代码内容读取工具");
        setSize(450, 300); // 稍微加宽窗口以容纳两个按钮
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null); // 设置窗口在屏幕上居中

        // --- UI 组件创建 ---
        JButton selectContentButton = new JButton("选择内容");
        selectContentButton.setFont(new Font("SansSerif", Font.PLAIN, 16));

        JButton getStructureButton = new JButton("获取项目结构");
        getStructureButton.setFont(new Font("SansSerif", Font.PLAIN, 16));


        // --- 事件监听 ---
        selectContentButton.addActionListener(e -> onSelectContentButtonClick());
        getStructureButton.addActionListener(e -> onGetStructureButtonClick());

        // --- 布局设置 ---
        // 使用一个 JPanel 来容纳两个按钮，并让它们水平排列
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 0)); // 居中对齐，按钮间距20像素
        buttonPanel.add(selectContentButton);
        buttonPanel.add(getStructureButton);

        // 使用 GridBagLayout 将 JPanel 整体居中
        setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.anchor = GridBagConstraints.CENTER;
        gbc.fill = GridBagConstraints.NONE;
        add(buttonPanel, gbc);
    }

    /**
     * "选择内容"按钮点击后执行的逻辑
     */
    private void onSelectContentButtonClick() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        fileChooser.setDialogTitle("请选择一个项目文件夹以读取内容");

        int result = fileChooser.showOpenDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            File selectedDirectory = fileChooser.getSelectedFile();
            // 使用后台线程处理文件读取，避免UI卡顿
            new Thread(() -> processDirectoryForContent(selectedDirectory)).start();
        }
    }

    /**
     * "获取项目结构"按钮点击后执行的逻辑
     */
    private void onGetStructureButtonClick() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        fileChooser.setDialogTitle("请选择一个项目文件夹以获取结构");

        int result = fileChooser.showOpenDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            File selectedDirectory = fileChooser.getSelectedFile();
            // 使用后台线程处理，避免UI卡顿
            new Thread(() -> processDirectoryForStructure(selectedDirectory)).start();
        }
    }

    /**
     * 处理选定的文件夹：递归遍历、读取文件内容并复制到剪切板
     * @param directory 选定的文件夹
     */
    private void processDirectoryForContent(File directory) {
        StringBuilder contentBuilder = new StringBuilder();
        try {
            traverseDirectory(directory, contentBuilder);
            copyToClipboard(contentBuilder.toString());
            SwingUtilities.invokeLater(() -> showSuccessDialog("内容已粘贴到剪切板"));
        } catch (IOException ex) {
            SwingUtilities.invokeLater(() -> showErrorDialog("读取文件时发生错误: " + ex.getMessage()));
        }
    }

    /**
     * 处理选定的文件夹：生成树状结构并复制到剪切板
     * @param directory 选定的文件夹
     */
    private void processDirectoryForStructure(File directory) {
        StringBuilder structureBuilder = new StringBuilder();
        structureBuilder.append(directory.getName()).append("\n"); // 添加根目录
        buildTreeStructure(directory, structureBuilder, "");
        copyToClipboard(structureBuilder.toString());
        SwingUtilities.invokeLater(() -> showSuccessDialog("项目结构已粘贴到剪切板"));
    }

    /**
     * 递归生成目录的树状结构字符串
     * @param dir 当前目录
     * @param builder 用于拼接的StringBuilder
     * @param prefix 前缀，用于绘制树状线条
     */
    private void buildTreeStructure(File dir, StringBuilder builder, String prefix) {
        File[] files = dir.listFiles();
        if (files == null) {
            return;
        }
        Arrays.sort(files); // 排序以保证顺序

        for (int i = 0; i < files.length; i++) {
            File file = files[i];
            boolean isLast = (i == files.length - 1);
            builder.append(prefix);
            builder.append(isLast ? "└── " : "├── ");
            builder.append(file.getName()).append("\n");

            if (file.isDirectory()) {
                String newPrefix = prefix + (isLast ? "    " : "│   ");
                buildTreeStructure(file, builder, newPrefix);
            }
        }
    }

    /**
     * 递归遍历目录并读取符合条件的文件内容
     * @param dir 当前遍历的目录
     * @param builder 用于拼接文件内容的 StringBuilder
     * @throws IOException 如果文件读取失败
     */
    private void traverseDirectory(File dir, StringBuilder builder) throws IOException {
        File[] files = dir.listFiles();
        if (files == null) {
            return;
        }
        Arrays.sort(files);

        for (File file : files) {
            if (file.isDirectory()) {
                traverseDirectory(file, builder);
            } else {
                String fileName = file.getName();
                int lastDotIndex = fileName.lastIndexOf('.');
                if (lastDotIndex > 0) {
                    String extension = fileName.substring(lastDotIndex + 1).toLowerCase();
                    if (ALLOWED_EXTENSIONS.contains(extension)) {
                        String content = Files.readString(file.toPath());
                        builder.append("--- 文件路径: ").append(file.getAbsolutePath()).append(" ---\n\n");
                        builder.append(content).append("\n\n");
                    }
                }
            }
        }
    }

    /**
     * 将字符串内容复制到系统剪切板
     * @param content 要复制的文本
     */
    private void copyToClipboard(String content) {
        StringSelection stringSelection = new StringSelection(content);
        Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
        clipboard.setContents(stringSelection, null);
    }

    /**
     * 封装一个居中显示的成功对话框
     * @param message 提示信息
     */
    private void showSuccessDialog(String message) {
        JOptionPane.showMessageDialog(
                this,
                message,
                "操作成功",
                JOptionPane.INFORMATION_MESSAGE
        );
    }

    /**
     * 封装一个居中显示的错误对话框
     * @param message 错误信息
     */
    private void showErrorDialog(String message) {
        JOptionPane.showMessageDialog(
                this,
                message,
                "错误",
                JOptionPane.ERROR_MESSAGE
        );
    }
}
