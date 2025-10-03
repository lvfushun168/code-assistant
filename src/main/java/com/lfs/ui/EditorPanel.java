package com.lfs.ui;

import org.apache.commons.lang3.StringUtils;

import javax.swing.*;
import java.awt.*;

public class EditorPanel extends JPanel {

    private final JTextArea rightTextArea;
    private final MainFrameController controller;

    public EditorPanel(MainFrameController controller) {
        super(new BorderLayout());
        this.controller = controller;
        this.rightTextArea = new JTextArea();
        initUI();
    }

    private void initUI() {
        // --- 创建右侧文本区域 ---
        rightTextArea.setEditable(true);
//        rightTextArea.setLineWrap(true);
//        rightTextArea.setWrapStyleWord(true);
        JScrollPane rightScrollPane = new JScrollPane(rightTextArea);
        TextLineNumber tln = new TextLineNumber(rightTextArea);
        rightScrollPane.setRowHeaderView(tln);

        // --- 创建菜单栏 ---
        JMenuBar menuBar = new JMenuBar();
        menuBar.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, Color.LIGHT_GRAY));

        // --- 创建“项目”菜单 ---
        JMenu project = new JMenu("<html><u>项目</u></html>");
        JMenuItem getContentMenuItem = new JMenuItem("获取代码内容");
        getContentMenuItem.addActionListener(e -> controller.onProcessDirectory(true));
        project.add(getContentMenuItem);
        JMenuItem getStructureMenuItem = new JMenuItem("获取项目结构");
        getStructureMenuItem.addActionListener(e -> controller.onProcessDirectory(false));
        project.add(getStructureMenuItem);

        // --- 创建“保存”菜单 ---
        JMenu saveMenu = new JMenu("<html><u>保存</u></html>");
        JMenuItem saveAsMenuItem = new JMenuItem("另存为...");
        saveAsMenuItem.addActionListener(e -> controller.onSaveAs());
        saveMenu.add(saveAsMenuItem);
        JMenuItem saveMenuItem = new JMenuItem("保存");
        saveMenu.add(saveMenuItem);

        // --- 创建“导入”菜单 ---
        JMenu importMenu = new JMenu("<html><u>导入</u></html>");

        // --- 将菜单添加到菜单栏 ---
        menuBar.add(project);
        menuBar.add(saveMenu);
        menuBar.add(importMenu);

        // --- 将菜单栏和文本区域添加到右侧面板 ---
        add(menuBar, BorderLayout.NORTH);
        add(rightScrollPane, BorderLayout.CENTER);
    }

    public String getTextAreaContent() {
        return rightTextArea.getText();
    }
}
