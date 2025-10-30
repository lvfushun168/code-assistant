package com.lfs.ui;

import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.SyntaxConstants;
import org.fife.ui.rtextarea.RTextScrollPane;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.io.File;

public class EditorPanel extends JPanel {

    private final RSyntaxTextArea rightTextArea;
    private final MainFrameController controller;
    private File currentFile;
    // RSyntaxTextArea 有自己的撤销管理器，所以我们不需要一个单独的。

    public EditorPanel(MainFrameController controller) {
        super(new BorderLayout());
        this.controller = controller;
        this.rightTextArea = new RSyntaxTextArea();
        initUI();
        setupSaveShortcut();
        setupFindShortcut();
        // 撤销/重做由 RSyntaxTextArea 默认处理
    }

    private FindReplaceDialog findReplaceDialog;

    private void setupFindShortcut() {
        InputMap inputMap = rightTextArea.getInputMap(JComponent.WHEN_FOCUSED);
        ActionMap actionMap = rightTextArea.getActionMap();

        KeyStroke keyStroke = KeyStroke.getKeyStroke(KeyEvent.VK_F, Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx());
        String findActionKey = "findAction";

        inputMap.put(keyStroke, findActionKey);
        actionMap.put(findActionKey, new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (findReplaceDialog == null) {
                    Window owner = SwingUtilities.getWindowAncestor(EditorPanel.this);
                    if (owner instanceof Frame) {
                        // FindReplaceDialog 需要一个 JTextArea，而 RSyntaxTextArea 是其子类，所以这应该能工作。
                        findReplaceDialog = new FindReplaceDialog((Frame) owner, rightTextArea);
                    }
                }
                findReplaceDialog.setVisible(true);
            }
        });
    }

    private void initUI() {
        rightTextArea.setEditable(true);
        rightTextArea.setCodeFoldingEnabled(true);
        rightTextArea.setAntiAliasingEnabled(true);

        try {
            java.io.InputStream in = getClass().getResourceAsStream("/org/fife/ui/rsyntaxtextarea/themes/dark.xml");
            org.fife.ui.rsyntaxtextarea.Theme theme = org.fife.ui.rsyntaxtextarea.Theme.load(in);
            theme.apply(rightTextArea);
        } catch (java.io.IOException e) {
            // 如果出现错误，我们可以记录它，但编辑器仍将使用默认颜色正常工作。
            e.printStackTrace();
        }

        RTextScrollPane rightScrollPane = new RTextScrollPane(rightTextArea);
        rightScrollPane.setLineNumbersEnabled(true);

        add(rightScrollPane, BorderLayout.CENTER);
    }

    private void setupSaveShortcut() {
        InputMap inputMap = rightTextArea.getInputMap(JComponent.WHEN_FOCUSED);
        ActionMap actionMap = rightTextArea.getActionMap();

        KeyStroke keyStroke = KeyStroke.getKeyStroke(KeyEvent.VK_S, Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx());
        String saveActionKey = "saveAction";

        inputMap.put(keyStroke, saveActionKey);
        actionMap.put(saveActionKey, new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                controller.saveCurrentFile();
            }
        });
    }

    public String getTextAreaContent() {
        return rightTextArea.getText();
    }

    public RSyntaxTextArea getTextArea() {
        return rightTextArea;
    }

    public void setTextAreaContent(String content) {
        // RSyntaxTextArea 的速度足够快，我们不需要为设置文本而进行复杂的监听器管理。
        rightTextArea.setText(content);
        rightTextArea.discardAllEdits(); // 设置新内容后清除撤销历史
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
                rightTextArea.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_JAVA);
                break;
            case "py":
                rightTextArea.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_PYTHON);
                break;
            case "js":
                rightTextArea.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_JAVASCRIPT);
                break;
            case "ts":
                rightTextArea.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_TYPESCRIPT);
                break;
            case "html":
            case "htm":
                rightTextArea.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_HTML);
                break;
            case "css":
                rightTextArea.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_CSS);
                break;
            case "xml":
                rightTextArea.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_XML);
                break;
            case "json":
                rightTextArea.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_JSON);
                break;
            case "sql":
                rightTextArea.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_SQL);
                break;
            case "md":
                rightTextArea.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_MARKDOWN);
                break;
            case "sh":
                rightTextArea.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_UNIX_SHELL);
                break;
            case "bat":
                rightTextArea.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_WINDOWS_BATCH);
                break;
            case "yaml":
            case "yml":
                rightTextArea.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_YAML);
                break;
            default:
                rightTextArea.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_NONE);
                break;
        }
    }
}