package com.lfs.ui;

import javax.swing.*;
import java.awt.*;

public class ActionPanel extends JPanel {

    private final MainFrameController controller;

    public ActionPanel(MainFrameController controller) {
        super(new GridBagLayout());
        this.controller = controller;
        initUI();
    }

    private void initUI() {
        // --- UI 组件创建 ---
        JButton selectContentButton = new JButton("获取代码内容");
        selectContentButton.setFont(new Font("SansSerif", Font.PLAIN, 16));

        JButton getStructureButton = new JButton("获取项目结构");
        getStructureButton.setFont(new Font("SansSerif", Font.PLAIN, 16));

        JButton saveButton = new JButton("保存");
        saveButton.setFont(new Font("SansSerif", Font.PLAIN, 16));

        // --- 竖向布局 ---
        selectContentButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        getStructureButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        saveButton.setAlignmentX(Component.CENTER_ALIGNMENT);

        // --- 事件监听 ---
        selectContentButton.addActionListener(e -> controller.onProcessDirectory(true));
        getStructureButton.addActionListener(e -> controller.onProcessDirectory(false));
        saveButton.addActionListener(e -> controller.saveCurrentFile());

        // --- 布局设置 ---
        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.Y_AXIS));
        buttonPanel.add(selectContentButton);
        buttonPanel.add(Box.createRigidArea(new Dimension(0, 20)));
        buttonPanel.add(getStructureButton);
        buttonPanel.add(Box.createRigidArea(new Dimension(0, 20)));
        buttonPanel.add(saveButton);

        // --- 将按钮面板居中放置 ---
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.anchor = GridBagConstraints.CENTER;
        gbc.fill = GridBagConstraints.NONE;
        add(buttonPanel, gbc);
    }
}
