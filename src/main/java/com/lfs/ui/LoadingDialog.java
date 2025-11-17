package com.lfs.ui;

import javax.swing.*;
import java.awt.*;

public class LoadingDialog extends JDialog {

    public LoadingDialog(Frame owner) {
        super(owner, "加载中...", true);
        initUI();
    }

    public LoadingDialog(JDialog owner) {
        super(owner, "加载中...", true);
        initUI();
    }

    private void initUI() {
        setLayout(new BorderLayout(15, 15));
        JProgressBar progressBar = new JProgressBar();
        progressBar.setIndeterminate(true);
        JLabel label = new JLabel("请稍候，正在处理您的请求...");
        label.setHorizontalAlignment(SwingConstants.CENTER);

        add(label, BorderLayout.NORTH);
        add(progressBar, BorderLayout.CENTER);

        setSize(300, 100);
        setLocationRelativeTo(getOwner());
        setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
    }
}
