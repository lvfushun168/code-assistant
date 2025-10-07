package com.lfs.ui;

import com.lfs.util.NotificationUtil;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.List;
import javax.swing.AbstractAction;
import javax.swing.ActionMap;
import javax.swing.InputMap;
import javax.swing.JComponent;
import javax.swing.KeyStroke;
import java.awt.Window;
import javax.swing.SwingUtilities;


public class LargeFileEditorPanel extends JPanel {

    private final JTextArea textArea;
    private final JScrollPane scrollPane;
    private final JProgressBar progressBar;
    private final JLabel statusLabel;
    private final JPanel statusPanel;
    private File currentFile;
    private FindReplaceDialog findReplaceDialog;

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

        // 添加键盘监听器以处理保存快捷键
        textArea.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if ((e.getKeyCode() == KeyEvent.VK_S) && (e.getModifiersEx() & Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx()) != 0) {
                    saveFile();
                    e.consume(); // 消费事件，防止其他监听器处理
                }
            }
        });

        setupFindShortcut();
    }

    private void setupFindShortcut() {
        InputMap inputMap = textArea.getInputMap(JComponent.WHEN_FOCUSED);
        ActionMap actionMap = textArea.getActionMap();

        KeyStroke keyStroke = KeyStroke.getKeyStroke(KeyEvent.VK_F, Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx());
        String findActionKey = "findAction";

        inputMap.put(keyStroke, findActionKey);
        actionMap.put(findActionKey, new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (findReplaceDialog == null) {
                    // Find the top-level window (Frame) to be the owner of the dialog
                    Window owner = SwingUtilities.getWindowAncestor(LargeFileEditorPanel.this);
                    if (owner instanceof Frame) {
                        findReplaceDialog = new FindReplaceDialog((Frame) owner, textArea);
                    }
                }
                findReplaceDialog.setVisible(true);
            }
        });
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

    public JTextArea getTextArea() {
        return textArea;
    }

    public void saveFile() {
        if (currentFile == null) {
            NotificationUtil.showErrorDialog(this, "没有要保存的文件。");
            return;
        }

        // 在保存操作期间显示加载光标
        setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        statusLabel.setText("正在保存 " + currentFile.getName() + "...");

        // 使用 SwingWorker 在后台线程中执行文件写入操作
        new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() throws Exception {
                try (OutputStreamWriter writer = new OutputStreamWriter(new FileOutputStream(currentFile), StandardCharsets.UTF_8)) {
                    writer.write(textArea.getText());
                }
                return null;
            }

            @Override
            protected void done() {
                try {
                    get(); // 检查在 doInBackground 中是否有异常抛出
                    statusLabel.setText("文件已保存: " + currentFile.getName());
                    NotificationUtil.showSaveSuccess(LargeFileEditorPanel.this);
                } catch (Exception e) {
                    e.printStackTrace();
                    statusLabel.setText("保存失败: " + e.getMessage());
                    NotificationUtil.showErrorDialog(LargeFileEditorPanel.this, "保存文件失败: " + e.getMessage());
                } finally {
                    // 恢复默认光标
                    setCursor(Cursor.getDefaultCursor());
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