package com.lfs.ui;

import com.lfs.service.UserPreferencesService;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.SyntaxConstants;
import org.fife.ui.rtextarea.RTextScrollPane;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class EditorPanel extends JPanel {

    private final RSyntaxTextArea rightTextArea;
    private final MainFrameController controller;
    private final UserPreferencesService preferencesService;
    private File currentFile;

    private boolean isCloudFile = false;
    private Long cloudContentId;
    private Long cloudDirId;
    private String cloudTitle;

    private String currentSyntax = "txt";

    public String getCurrentSyntax() {
        return currentSyntax;
    }

    public EditorPanel(MainFrameController controller, UserPreferencesService preferencesService) {
        super(new BorderLayout());
        this.controller = controller;
        this.preferencesService = preferencesService;
        this.rightTextArea = new RSyntaxTextArea();
        initUI();
        setupSaveShortcut();
        setupFindShortcut();
    }

    public boolean isCloudFile() {
        return isCloudFile;
    }

    public void setCloudFile(boolean cloudFile) {
        isCloudFile = cloudFile;
    }

    public Long getCloudContentId() {
        return cloudContentId;
    }

    public void setCloudContentId(Long cloudContentId) {
        this.cloudContentId = cloudContentId;
    }

    public Long getCloudDirId() {
        return cloudDirId;
    }

    public void setCloudDirId(Long cloudDirId) {
        this.cloudDirId = cloudDirId;
    }

    public String getCloudTitle() {
        return cloudTitle;
    }

    public void setCloudTitle(String cloudTitle) {
        this.cloudTitle = cloudTitle;
    }

    private FindReplaceDialog findReplaceDialog;
    private JMenu syntaxMenu;
    private final ButtonGroup syntaxGroup = new ButtonGroup();

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
            e.printStackTrace();
        }

        float fontSize = preferencesService.loadFontSize();
        rightTextArea.setFont(rightTextArea.getFont().deriveFont(fontSize));
        rightTextArea.setLineWrap(preferencesService.loadLineWrap());

        JPopupMenu popupMenu = rightTextArea.getPopupMenu();
        syntaxMenu = new JMenu("语法类型");

        List<String> sortedExtensions = new ArrayList<>(com.lfs.config.AppConfig.ALLOWED_EXTENSIONS);
        Collections.sort(sortedExtensions);

        for (String extension : sortedExtensions) {
            JRadioButtonMenuItem syntaxItem = new JRadioButtonMenuItem(extension);
            syntaxItem.setActionCommand(extension);
            syntaxItem.addActionListener(e -> setSyntaxStyle(e.getActionCommand()));
            syntaxGroup.add(syntaxItem);
            syntaxMenu.add(syntaxItem);
        }

        JMenuItem compressWhitespaceItem = new JMenuItem("压缩空白");
        compressWhitespaceItem.addActionListener(e -> {
            String selectedText = rightTextArea.getSelectedText();
            if (selectedText != null && !selectedText.isEmpty()) {
                String compressedText = selectedText.replaceAll("\\s+", "");
                rightTextArea.replaceSelection(compressedText);
            }
        });
        popupMenu.add(compressWhitespaceItem);

        popupMenu.addSeparator();
        popupMenu.add(syntaxMenu);

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
                if (isCloudFile) {
                    controller.saveCloudFile();
                } else {
                    controller.saveCurrentFile();
                }
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
        rightTextArea.setText(content);
        rightTextArea.discardAllEdits();
    }

    public File getCurrentFile() {
        return currentFile;
    }

    public void setCurrentFile(File currentFile) {
        this.currentFile = currentFile;

        // 优先加载用户保存的语法偏好
        String savedSyntax = preferencesService.loadFileSyntax(currentFile.getAbsolutePath());
        if (savedSyntax != null) {
            setSyntaxStyle(savedSyntax);
            return;
        }

        // 默认自动推断
        String fileName = currentFile.getName();
        int lastDot = fileName.lastIndexOf('.');
        if (lastDot > 0) {
            String extension = fileName.substring(lastDot + 1);
            setSyntaxStyle(extension);
        } else {
            setSyntaxStyle("none");
        }
    }

    private void updateSyntaxMenuSelection(String currentExtension) {
        if (syntaxMenu == null) return;
        for (int i = 0; i < syntaxMenu.getItemCount(); i++) {
            JMenuItem item = syntaxMenu.getItem(i);
            if (item instanceof JRadioButtonMenuItem) {
                JRadioButtonMenuItem radioItem = (JRadioButtonMenuItem) item;
                if (radioItem.getActionCommand().equalsIgnoreCase(currentExtension)) {
                    radioItem.setSelected(true);
                    break;
                }
            }
        }
    }

    public void setSyntaxStyle(String extension) {
        String style;
        String effectiveExtension = extension.toLowerCase();
        switch (effectiveExtension) {
            case "java": style = SyntaxConstants.SYNTAX_STYLE_JAVA; break;
            case "py": style = SyntaxConstants.SYNTAX_STYLE_PYTHON; break;
            case "js": style = SyntaxConstants.SYNTAX_STYLE_JAVASCRIPT; break;
            case "ts": style = SyntaxConstants.SYNTAX_STYLE_TYPESCRIPT; break;
            case "html": case "htm": style = SyntaxConstants.SYNTAX_STYLE_HTML; break;
            case "css": style = SyntaxConstants.SYNTAX_STYLE_CSS; break;
            case "xml": style = SyntaxConstants.SYNTAX_STYLE_XML; break;
            case "json": style = SyntaxConstants.SYNTAX_STYLE_JSON; break;
            case "sql": style = SyntaxConstants.SYNTAX_STYLE_SQL; break;
            case "md": style = SyntaxConstants.SYNTAX_STYLE_MARKDOWN; break;
            case "sh": style = SyntaxConstants.SYNTAX_STYLE_UNIX_SHELL; break;
            case "bat": style = SyntaxConstants.SYNTAX_STYLE_WINDOWS_BATCH; break;
            case "yaml": case "yml": style = SyntaxConstants.SYNTAX_STYLE_YAML; break;
            case "c": style = SyntaxConstants.SYNTAX_STYLE_C; break;
            case "cpp": style = SyntaxConstants.SYNTAX_STYLE_CPLUSPLUS; break;
            case "cs": style = SyntaxConstants.SYNTAX_STYLE_CSHARP; break;
            case "go": style = SyntaxConstants.SYNTAX_STYLE_GO; break;
            case "php": style = SyntaxConstants.SYNTAX_STYLE_PHP; break;
            case "rb": style = SyntaxConstants.SYNTAX_STYLE_RUBY; break;
            case "kt": case "kts": style = SyntaxConstants.SYNTAX_STYLE_KOTLIN; break;
            case "gradle": style = SyntaxConstants.SYNTAX_STYLE_GROOVY; break;
            default:
                style = SyntaxConstants.SYNTAX_STYLE_NONE;
                effectiveExtension = "none";
                break;
        }
        rightTextArea.setSyntaxEditingStyle(style);
        this.currentSyntax = effectiveExtension;
        updateSyntaxMenuSelection(effectiveExtension);

        // 如果是本地文件，记录用户的选择
        if (this.currentFile != null) {
            preferencesService.saveFileSyntax(this.currentFile.getAbsolutePath(), effectiveExtension);
        }
    }

    public void zoomIn() {
        Font font = rightTextArea.getFont();
        float size = font.getSize() + 1.0f;
        rightTextArea.setFont(font.deriveFont(size));
        preferencesService.saveFontSize(size);
    }

    public void zoomOut() {
        Font font = rightTextArea.getFont();
        float size = font.getSize() - 1.0f;
        if (size >= 8.0f) {
            rightTextArea.setFont(font.deriveFont(size));
            preferencesService.saveFontSize(size);
        }
    }

    public void setLineWrap(boolean wrap) {
        rightTextArea.setLineWrap(wrap);
        preferencesService.saveLineWrap(wrap);
    }

    public boolean getLineWrap() {
        return rightTextArea.getLineWrap();
    }
}