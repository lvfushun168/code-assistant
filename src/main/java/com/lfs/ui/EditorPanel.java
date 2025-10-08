package com.lfs.ui;

import org.apache.commons.lang3.StringUtils;

import javax.swing.*;
import javax.swing.undo.UndoManager;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.io.File;

public class EditorPanel extends JPanel {

    private final JTextArea rightTextArea;
    private final MainFrameController controller;
    private File currentFile;
    private final UndoManager undoManager = new UndoManager();


    public EditorPanel(MainFrameController controller) {
        super(new BorderLayout());
        this.controller = controller;
        this.rightTextArea = new JTextArea();
        initUI();
        setupSaveShortcut();
        setupFindShortcut();
        setupUndoRedo();
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
                    // Find the top-level window (Frame) to be the owner of the dialog
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
        // --- 创建右侧文本区域 ---
        rightTextArea.setEditable(true);
        rightTextArea.getDocument().addUndoableEditListener(undoManager);
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

    private void setupUndoRedo() {
        InputMap inputMap = rightTextArea.getInputMap(JComponent.WHEN_FOCUSED);
        ActionMap actionMap = rightTextArea.getActionMap();

        // Undo: Command/Control + Z
        KeyStroke undoKeyStroke = KeyStroke.getKeyStroke(KeyEvent.VK_Z, Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx());
        inputMap.put(undoKeyStroke, "undo");
        actionMap.put("undo", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (undoManager.canUndo()) {
                    undoManager.undo();
                }
            }
        });

        // Redo: Command/Control + Shift + Z
        KeyStroke redoKeyStroke = KeyStroke.getKeyStroke(KeyEvent.VK_Z, Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx() | InputEvent.SHIFT_DOWN_MASK);
        inputMap.put(redoKeyStroke, "redo");
        actionMap.put("redo", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (undoManager.canRedo()) {
                    undoManager.redo();
                }
            }
        });
    }

    public String getTextAreaContent() {
        return rightTextArea.getText();
    }

    public JTextArea getTextArea() {
        return rightTextArea;
    }

    public void setTextAreaContent(String content) {
        rightTextArea.setText(content);
        undoManager.discardAllEdits();
    }

    public File getCurrentFile() {
        return currentFile;
    }

    public void setCurrentFile(File currentFile) {
        this.currentFile = currentFile;
    }
}
