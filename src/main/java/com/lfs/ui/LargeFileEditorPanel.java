package com.lfs.ui;

import com.lfs.util.NotificationUtil;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.SyntaxConstants;
import org.fife.ui.rtextarea.RTextScrollPane;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.List;

public class LargeFileEditorPanel extends JPanel {

    private final RSyntaxTextArea textArea;
    private final JProgressBar progressBar;
    private final JLabel statusLabel;
    private File currentFile;
    private FindReplaceDialog findReplaceDialog;

    public LargeFileEditorPanel() {
        super(new BorderLayout());
        textArea = new RSyntaxTextArea();
        textArea.setEditable(false); // 最初不可编辑
        textArea.setCodeFoldingEnabled(true);
        textArea.setAntiAliasingEnabled(true);

        try {
            java.io.InputStream in = getClass().getResourceAsStream("/org/fife/ui/rsyntaxtextarea/themes/dark.xml");
            org.fife.ui.rsyntaxtextarea.Theme theme = org.fife.ui.rsyntaxtextarea.Theme.load(in);
            theme.apply(textArea);
        } catch (java.io.IOException e) {
            e.printStackTrace();
        }

        RTextScrollPane scrollPane = new RTextScrollPane(textArea);
        scrollPane.setLineNumbersEnabled(true);
        add(scrollPane, BorderLayout.CENTER);

        JPanel statusPanel = new JPanel(new BorderLayout());
        progressBar = new JProgressBar();
        progressBar.setVisible(false);
        statusLabel = new JLabel(" ");
        statusLabel.setBorder(BorderFactory.createEmptyBorder(2, 5, 2, 5));
        statusPanel.add(progressBar, BorderLayout.NORTH);
        statusPanel.add(statusLabel, BorderLayout.CENTER);
        add(statusPanel, BorderLayout.SOUTH);

        setupShortcuts();
    }

    private void setupShortcuts() {
        InputMap inputMap = textArea.getInputMap(JComponent.WHEN_FOCUSED);
        ActionMap actionMap = textArea.getActionMap();

        // Save: Command/Control + S
        KeyStroke saveKeyStroke = KeyStroke.getKeyStroke(KeyEvent.VK_S, Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx());
        inputMap.put(saveKeyStroke, "save");
        actionMap.put("save", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                saveFile();
            }
        });

        // Find: Command/Control + F
        KeyStroke findKeyStroke = KeyStroke.getKeyStroke(KeyEvent.VK_F, Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx());
        inputMap.put(findKeyStroke, "find");
        actionMap.put("find", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (findReplaceDialog == null) {
                    Window owner = SwingUtilities.getWindowAncestor(LargeFileEditorPanel.this);
                    if (owner instanceof Frame) {
                        findReplaceDialog = new FindReplaceDialog((Frame) owner, textArea);
                    }
                }
                findReplaceDialog.setVisible(true);
            }
        });

        // 撤销/重做由 RSyntaxTextArea 的默认键映射处理
    }

    public void loadFile(File file) {
        setCurrentFile(file);
        textArea.discardAllEdits();
        textArea.setText(""); // Clear previous content
        statusLabel.setText("正在加载 " + file.getName() + "...");
        progressBar.setValue(0);
        progressBar.setVisible(true);
        long length = file.length();

        // 缓冲区大小上限为32KB，以防止长时间阻塞UI更新。
        final int MIN_BUFFER_SIZE = 4 * 1024; // 4KB
        final int MAX_BUFFER_SIZE = 32 * 1024; // 32KB
        int bufferSize = (int) Math.max(MIN_BUFFER_SIZE, Math.min(MAX_BUFFER_SIZE, length / 200));
        final byte[] buffer = new byte[bufferSize];

        // 短暂延迟以让出UI线程。
        final long sleep = 2L;
        setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));

        new SwingWorker<Void, ProgressChunk>() {
            @Override
            protected Void doInBackground() throws Exception {
                long fileLength = file.length();
                try (InputStream in = new FileInputStream(file)) {
                    int bytesRead;
                    long totalBytesRead = 0;
                    while ((bytesRead = in.read(buffer)) != -1) {
                        String chunk = new String(buffer, 0, bytesRead, StandardCharsets.UTF_8);
                        totalBytesRead += bytesRead;
                        int progress = (int) ((totalBytesRead * 100) / fileLength);
                        publish(new ProgressChunk(chunk, progress));
                        Thread.sleep(sleep);
                    }
                }
                return null;
            }

            @Override
            protected void process(List<ProgressChunk> chunks) {
                for (ProgressChunk chunk : chunks) {
                    // 追加到 RSyntaxTextArea 应该会更快
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

    public RSyntaxTextArea getTextArea() {
        return textArea;
    }

    public void saveFile() {
        if (currentFile == null) {
            NotificationUtil.showErrorDialog(this, "没有要保存的文件。");
            return;
        }

        setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        statusLabel.setText("正在保存 " + currentFile.getName() + "...");

        new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() throws Exception {
                try (OutputStreamWriter writer = new OutputStreamWriter(new FileOutputStream(currentFile), StandardCharsets.UTF_8)) {
                    // 对于大文件来说，这可能会很慢，但保存操作预计会花费一些时间。
                    writer.write(textArea.getText());
                }
                return null;
            }

            @Override
            protected void done() {
                try {
                    get();
                    statusLabel.setText("文件已保存: " + currentFile.getName());
                    NotificationUtil.showSaveSuccess(LargeFileEditorPanel.this);
                } catch (Exception e) {
                    e.printStackTrace();
                    statusLabel.setText("保存失败: " + e.getMessage());
                    NotificationUtil.showErrorDialog(LargeFileEditorPanel.this, "保存文件失败: " + e.getMessage());
                } finally {
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
        // 根据文件扩展名设置语法
        String fileName = currentFile.getName();
        int lastDot = fileName.lastIndexOf('.');
        if (lastDot > 0) {
            String extension = fileName.substring(lastDot + 1);
            setSyntaxStyle(extension);
        }
    }
    
    private void setSyntaxStyle(String extension) {
        switch (extension.toLowerCase()) {
            case "java":
                textArea.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_JAVA);
                break;
            case "py":
                textArea.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_PYTHON);
                break;
            case "js":
                textArea.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_JAVASCRIPT);
                break;
            case "ts":
                textArea.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_TYPESCRIPT);
                break;
            case "html":
            case "htm":
                textArea.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_HTML);
                break;
            case "css":
                textArea.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_CSS);
                break;
            case "xml":
                textArea.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_XML);
                break;
            case "json":
                textArea.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_JSON);
                break;
            case "sql":
                textArea.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_SQL);
                break;
            case "md":
                textArea.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_MARKDOWN);
                break;
            case "sh":
                textArea.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_UNIX_SHELL);
                break;
            case "bat":
                textArea.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_WINDOWS_BATCH);
                break;
            case "yaml":
            case "yml":
                textArea.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_YAML);
                break;
            default:
                textArea.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_NONE);
                break;
        }
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