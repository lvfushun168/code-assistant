package com.lfs.ui;

import com.lfs.util.NotificationUtil;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

public class LargeFileEditorPanel extends JPanel {

    private final JTextArea textArea;
    private final JScrollPane scrollPane;
    private final JProgressBar progressBar;
    private final JLabel statusLabel;
    private final JPanel statusPanel;
    private File currentFile;

    public LargeFileEditorPanel() {
        super(new BorderLayout());
        textArea = new JTextArea();
        textArea.setLineWrap(false);
        textArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        textArea.setEditable(false); // 最初不可编辑，直到文件加载完成

        scrollPane = new JScrollPane(textArea);
        add(scrollPane, BorderLayout.CENTER);

        statusPanel = new JPanel(new BorderLayout());
        progressBar = new JProgressBar();
        progressBar.setVisible(false);
        statusLabel = new JLabel(" ");
        statusLabel.setBorder(BorderFactory.createEmptyBorder(2, 5, 2, 5));
        statusPanel.add(progressBar, BorderLayout.NORTH);
        statusPanel.add(statusLabel, BorderLayout.CENTER);
        add(statusPanel, BorderLayout.SOUTH);
    }

    public void loadFile(File file) {
        this.currentFile = file;
        textArea.setText(""); // 清除之前的内容
        statusLabel.setText("正在加载 " + file.getName() + "...");
        progressBar.setValue(0);
        progressBar.setVisible(true);
        setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));

        new SwingWorker<Void, ProgressChunk>() {
            @Override
            protected Void doInBackground() throws Exception {
                long fileLength = file.length();
                try (InputStream in = new FileInputStream(file)) {
                    byte[] buffer = new byte[65536]; // 64KB buffer
                    int bytesRead;
                    long totalBytesRead = 0;
                    while ((bytesRead = in.read(buffer)) != -1) {
                        String chunk = new String(buffer, 0, bytesRead, StandardCharsets.UTF_8);
                        totalBytesRead += bytesRead;
                        int progress = (int) ((totalBytesRead * 100) / fileLength);
                        publish(new ProgressChunk(chunk, progress));
                        // 节流，防止UI线程被过多事件淹没
                        Thread.sleep(3);
                    }
                }
                return null;
            }

            @Override
            protected void process(List<ProgressChunk> chunks) {
                for (ProgressChunk chunk : chunks) {
                    textArea.append(chunk.getText());
                    progressBar.setValue(chunk.getProgress());
                }
            }

            @Override
            protected void done() {
                try {
                    get(); // 检查异常
                    textArea.setEditable(true);
                    textArea.setCaretPosition(0); // 将光标移动到开头
                    statusLabel.setText("加载完成: " + file.getName());
                } catch (Exception e) {
                    e.printStackTrace();
                    NotificationUtil.showErrorDialog(LargeFileEditorPanel.this, "加载文件失败: " + e.getMessage());
                    statusLabel.setText("加载失败: " + e.getMessage());
                } finally {
                    setCursor(Cursor.getDefaultCursor());
                    progressBar.setVisible(false);
                }
            }
        }.execute();
    }

    public String getTextAreaContent() {
        return textArea.getText();
    }

    public File getCurrentFile() {
        return currentFile;
    }

    public void setCurrentFile(File currentFile) {
        this.currentFile = currentFile;
    }
}

// 用于发布文本块和进度的辅助类
class ProgressChunk {
    private final String text;
    private final int progress;

    public ProgressChunk(String text, int progress) {
        this.text = text;
        this.progress = progress;
    }

    public String getText() {
        return text;
    }

    public int getProgress() {
        return progress;
    }
}