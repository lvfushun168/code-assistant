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
        textArea.setEditable(false); // Initially not editable
        textArea.setCodeFoldingEnabled(true);
        textArea.setAntiAliasingEnabled(true);

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

        // Undo/Redo are handled by RSyntaxTextArea's default keymap
    }

    public void loadFile(File file) {
        setCurrentFile(file);
        textArea.discardAllEdits();
        textArea.setText(""); // Clear previous content
        statusLabel.setText("正在加载 " + file.getName() + "...");
        progressBar.setValue(0);
        progressBar.setVisible(true);
        long length = file.length();

        // Buffer size capped at 32KB to prevent long-blocking UI updates.
        final int MIN_BUFFER_SIZE = 4 * 1024; // 4KB
        final int MAX_BUFFER_SIZE = 32 * 1024; // 32KB
        int bufferSize = (int) Math.max(MIN_BUFFER_SIZE, Math.min(MAX_BUFFER_SIZE, length / 200));
        final byte[] buffer = new byte[bufferSize];

        // A small delay to yield to the UI thread.
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
                    // Appending to RSyntaxTextArea should be faster
                    textArea.append(chunk.getText());
                    progressBar.setValue(chunk.getProgress());
                }
            }

            @Override
            protected void done() {
                try {
                    get(); // Check for exceptions
                    textArea.setEditable(true);
                    textArea.setCaretPosition(0); // Move caret to the beginning
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
                    // This can be slow for huge files, but saving is expected to take time.
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
        // Set syntax based on file extension
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

// Helper class for publishing text chunks and progress
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