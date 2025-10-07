package com.lfs.ui;

import org.apache.commons.lang3.StringUtils;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.io.File;

public class EditorPanel extends JPanel {

    private final JTextArea rightTextArea;
    private final MainFrameController controller;
    private File currentFile;


    public EditorPanel(MainFrameController controller) {
        super(new BorderLayout());
        this.controller = controller;
        this.rightTextArea = new JTextArea();
        initUI();
        setupSaveShortcut();
    }

    private void initUI() {
        // --- 创建右侧文本区域 ---
        rightTextArea.setEditable(true);
//        rightTextArea.setLineWrap(true);
//        rightTextArea.setWrapStyleWord(true);
        JScrollPane rightScrollPane = new JScrollPane(rightTextArea);
        TextLineNumber tln = new TextLineNumber(rightTextArea);
        rightScrollPane.setRowHeaderView(tln);

        // --- 将菜单栏和文本区域添加到右侧面板 ---
        add(rightScrollPane, BorderLayout.CENTER);
    }

    private void setupSaveShortcut() {
        InputMap inputMap = rightTextArea.getInputMap(JComponent.WHEN_FOCUSED);
        ActionMap actionMap = rightTextArea.getActionMap();

        // 使用 getMenuShortcutKeyMaskEx() 来处理 Mac (Command) 和其他系统 (Ctrl)
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

    public void setTextAreaContent(String content) {
        rightTextArea.setText(content);
    }

    public File getCurrentFile() {
        return currentFile;
    }

    public void setCurrentFile(File currentFile) {
        this.currentFile = currentFile;
    }
}
