package com.lfs.ui;

import javax.swing.*;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultHighlighter;
import javax.swing.text.Highlighter;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import javax.swing.Timer;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

public class FindReplaceDialog extends JDialog {

    private final JTextArea textArea;
    private final JTextField findField;
    private final JTextField replaceField;
    private final JCheckBox regexCheckBox;
    private final JLabel statusLabel;
    private final Highlighter highlighter;
    private final Highlighter.HighlightPainter painter;

    private int lastMatchIndex = -1;

    public FindReplaceDialog(Frame owner, JTextArea textArea) {
        super(owner, "查找和替换", false);
        this.textArea = textArea;
        this.highlighter = textArea.getHighlighter();
        this.painter = new DefaultHighlighter.DefaultHighlightPainter(new Color(177, 220, 252)); // 浅蓝色高亮

        // --- UI 组件 ---
        findField = new JTextField(20);
        replaceField = new JTextField(20);
        regexCheckBox = new JCheckBox("正则表达式");
        statusLabel = new JLabel(" ");

        JButton findNextButton = new JButton("查找下一个");
        JButton replaceButton = new JButton("替换");
        JButton replaceAllButton = new JButton("全部替换");
        JButton closeButton = new JButton("关闭");

        // --- 布局 ---
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        gbc.gridx = 0; gbc.gridy = 0; panel.add(new JLabel("查找:"), gbc);
        gbc.gridx = 1; gbc.gridy = 0; gbc.gridwidth = 2; panel.add(findField, gbc);
        gbc.gridx = 3; gbc.gridy = 0; gbc.gridwidth = 1; panel.add(findNextButton, gbc);

        gbc.gridx = 0; gbc.gridy = 1; panel.add(new JLabel("替换为:"), gbc);
        gbc.gridx = 1; gbc.gridy = 1; gbc.gridwidth = 2; panel.add(replaceField, gbc);
        gbc.gridx = 3; gbc.gridy = 1; gbc.gridwidth = 1; panel.add(replaceButton, gbc);

        gbc.gridx = 1; gbc.gridy = 2; panel.add(regexCheckBox, gbc);
        gbc.gridx = 3; gbc.gridy = 2; panel.add(replaceAllButton, gbc);

        gbc.gridx = 0; gbc.gridy = 3; gbc.gridwidth = 3; panel.add(statusLabel, gbc);
        gbc.gridx = 3; gbc.gridy = 3; panel.add(closeButton, gbc);

        // --- 监听器 ---
        findNextButton.addActionListener(e -> findNext());
        replaceButton.addActionListener(e -> replace());
        replaceAllButton.addActionListener(e -> replaceAll());
        closeButton.addActionListener(e -> dispose());

        // 延迟搜索的计时器
        Timer searchTimer = new Timer(500, e -> updateOccurrenceCount());
        searchTimer.setRepeats(false);

        findField.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) {
                searchTimer.restart();
            }
            public void removeUpdate(DocumentEvent e) {
                searchTimer.restart();
            }
            public void changedUpdate(DocumentEvent e) {
                searchTimer.restart();
            }
        });

        findField.addActionListener(e -> findNext()); // 处理 Enter 键

        // 对话框打开时重置搜索
        addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent windowEvent) {
                resetSearch();
            }
        });
        closeButton.addActionListener(e -> resetSearch());


        // --- 对话框设置 ---
        getContentPane().add(panel);
        pack();
        setLocationRelativeTo(owner);
        setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
    }

    private void findNext() {
        highlighter.removeAllHighlights();
        String findText = findField.getText();
        if (findText.isEmpty()) {
            statusLabel.setText("请输入查找内容");
            return;
        }
        String content = textArea.getText();
        int searchFrom = (lastMatchIndex == -1) ? 0 : lastMatchIndex + 1;

        if (searchFrom >= content.length()) {
            searchFrom = 0; // 包含搜索
        }

        try {
            if (regexCheckBox.isSelected()) {
                Pattern pattern = Pattern.compile(findText);
                Matcher matcher = pattern.matcher(content);
                if (matcher.find(searchFrom)) {
                    highlightMatch(matcher.start(), matcher.end());
                    lastMatchIndex = matcher.start();
                } else if (lastMatchIndex != -1) { // 如果从当前位置未找到，则进行全局搜索
                    lastMatchIndex = -1;
                    findNext(); // 从头再来
                } else {
                    statusLabel.setText("未找到匹配项");
                }
            } else {
                int foundIndex = content.indexOf(findText, searchFrom);
                if (foundIndex != -1) {
                    highlightMatch(foundIndex, foundIndex + findText.length());
                    lastMatchIndex = foundIndex;
                } else if (lastMatchIndex != -1) { // Wrap search
                    lastMatchIndex = -1;
                    findNext();
                } else {
                    statusLabel.setText("未找到匹配项");
                }
            }
        } catch (PatternSyntaxException ex) {
            statusLabel.setText("正则表达式错误");
        }
    }

    private void replace() {
        String findText = findField.getText();
        String replaceText = replaceField.getText();
        if (findText.isEmpty()) {
            statusLabel.setText("请输入查找内容");
            return;
        }

        // If there's a selection that matches, replace it
        String selectedText = textArea.getSelectedText();
        if (selectedText != null && selectedText.equals(findText)) {
            textArea.replaceSelection(replaceText);
        }
        findNext();
    }

    private void replaceAll() {
        highlighter.removeAllHighlights();
        String findText = findField.getText();
        String replaceText = replaceField.getText();
        if (findText.isEmpty()) {
            statusLabel.setText("请输入查找内容");
            return;
        }

        try {
            String content = textArea.getText();
            String newContent;
            if (regexCheckBox.isSelected()) {
                Pattern pattern = Pattern.compile(findText);
                newContent = pattern.matcher(content).replaceAll(replaceText);
            } else {
                newContent = content.replace(findText, replaceText);
            }

            if (!content.equals(newContent)) {
                textArea.setText(newContent);
                statusLabel.setText("已完成全部替换");
            } else {
                statusLabel.setText("未找到可替换的匹配项");
            }
        } catch (PatternSyntaxException ex) {
            statusLabel.setText("正则表达式错误");
        }
        lastMatchIndex = -1;
    }

    private void updateOccurrenceCount() {
        highlighter.removeAllHighlights();
        lastMatchIndex = -1;
        String findText = findField.getText();
        if (findText.isEmpty()) {
            statusLabel.setText(" ");
            return;
        }

        String content = textArea.getText();
        int count = 0;
        try {
            if (regexCheckBox.isSelected()) {
                Pattern pattern = Pattern.compile(findText);
                Matcher matcher = pattern.matcher(content);
                while (matcher.find()) {
                    count++;
                }
            } else {
                int index = -1;
                while ((index = content.indexOf(findText, index + 1)) != -1) {
                    count++;
                }
            }
            statusLabel.setText("找到 " + count + " 个匹配项");
        } catch (PatternSyntaxException ex) {
            statusLabel.setText("正则表达式错误");
        }
    }

    private void highlightMatch(int start, int end) {
        try {
            highlighter.addHighlight(start, end, painter);
            textArea.setCaretPosition(start);
            textArea.moveCaretPosition(end);
        } catch (BadLocationException e) {
            e.printStackTrace();
        }
    }

    private void resetSearch() {
        highlighter.removeAllHighlights();
        lastMatchIndex = -1;
    }

    @Override
    public void setVisible(boolean b) {
        if (b) {
            // 每次显示对话框时，重置搜索索引
            lastMatchIndex = -1;
            // 并更新查找字段中当前文本的计数
            updateOccurrenceCount();
        } else {
            // 隐藏时，移除所有高亮显示
            resetSearch();
        }
        super.setVisible(b);
    }
}
