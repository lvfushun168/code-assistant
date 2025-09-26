package reader;

import com.formdev.flatlaf.FlatLightLaf;

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
        setTitle("代码协作助手");
        setSize(400, 300); // 设置一个初始尺寸
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        // 设置窗口在屏幕上居中
        setLocationRelativeTo(null);

        // --- UI 组件创建 ---
        JButton selectButton = new JButton("选择内容");
        selectButton.setFont(new Font("SansSerif", Font.PLAIN, 16)); // 设置一个更舒适的字体

        // 添加按钮点击事件监听器
        selectButton.addActionListener(e -> onSelectButtonClick());

        // --- 布局设置 ---
        // 使用 GridBagLayout 来轻松实现单个组件的完美居中
        setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.anchor = GridBagConstraints.CENTER; // 锚点居中
        gbc.fill = GridBagConstraints.NONE;     // 不填充
        add(selectButton, gbc);
    }

    /**
     * "选择内容"按钮点击后执行的逻辑
     */
    private void onSelectButtonClick() {
        JFileChooser fileChooser = new JFileChooser();
        // 设置文件选择器只允许选择文件夹
        fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        fileChooser.setDialogTitle("请选择一个项目文件夹");

        // 显示文件选择对话框，并使其相对于主窗口居中
        int result = fileChooser.showOpenDialog(this);

        // 如果用户点击了"打开"或"确定"
        if (result == JFileChooser.APPROVE_OPTION) {
            File selectedDirectory = fileChooser.getSelectedFile();
            // 使用后台线程处理文件读取，避免UI卡顿 (对于大项目很重要)
            new Thread(() -> processDirectory(selectedDirectory)).start();
        }
    }

    /**
     * 处理选定的文件夹：递归遍历、读取文件并复制到剪切板
     * @param directory 选定的文件夹
     */
    private void processDirectory(File directory) {
        StringBuilder contentBuilder = new StringBuilder();
        try {
            // 递归遍历文件夹
            traverseDirectory(directory, contentBuilder);

            // 将拼接好的内容复制到系统剪切板
            copyToClipboard(contentBuilder.toString());

            // 在UI线程中显示成功提示
            SwingUtilities.invokeLater(() ->
                    JOptionPane.showMessageDialog(
                            this,
                            "已粘贴到剪切板",
                            "操作成功",
                            JOptionPane.INFORMATION_MESSAGE
                    )
            );

        } catch (IOException ex) {
            // 在UI线程中显示错误提示
            SwingUtilities.invokeLater(() ->
                    JOptionPane.showMessageDialog(
                            this,
                            "读取文件时发生错误: " + ex.getMessage(),
                            "错误",
                            JOptionPane.ERROR_MESSAGE
                    )
            );
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
            return; // 如果目录为空或无法访问，则直接返回
        }

        Arrays.sort(files); // 对文件/文件夹进行排序，保证顺序基本一致

        for (File file : files) {
            if (file.isDirectory()) {
                // 如果是目录，则递归调用
                traverseDirectory(file, builder);
            } else {
                // 如果是文件，检查扩展名是否符合要求
                String fileName = file.getName();
                int lastDotIndex = fileName.lastIndexOf('.');
                if (lastDotIndex > 0) {
                    String extension = fileName.substring(lastDotIndex + 1).toLowerCase();
                    if (ALLOWED_EXTENSIONS.contains(extension)) {
                        // 如果扩展名在允许列表中，读取内容并追加
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
}
