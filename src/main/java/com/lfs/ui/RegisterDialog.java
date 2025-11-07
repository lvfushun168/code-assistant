package com.lfs.ui;

import com.lfs.domain.BackendResponse;
import com.lfs.service.AccountService;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.Random;

/**
 * 注册对话框
 */
public class RegisterDialog extends JDialog {

    private final JTextField usernameField = new JTextField(20);
    private final JPasswordField passwordField = new JPasswordField(20);
    private final JPasswordField confirmPasswordField = new JPasswordField(20);
    private final JTextField captchaField = new JTextField(10);
    private final JLabel captchaLabel = new JLabel();
    private String captchaText;

    private final AccountService accountService = new AccountService();

    public RegisterDialog(Frame owner) {
        super(owner, "用户注册", true);
        initUI();
    }

    private void initUI() {
        setLayout(new BorderLayout(15, 15));
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // 用户名
        gbc.gridx = 0;
        gbc.gridy = 0;
        panel.add(new JLabel("用户名:"), gbc);
        gbc.gridx = 1;
        panel.add(usernameField, gbc);

        // 密码
        gbc.gridx = 0;
        gbc.gridy = 1;
        panel.add(new JLabel("密码:"), gbc);
        gbc.gridx = 1;
        panel.add(passwordField, gbc);

        // 确认密码
        gbc.gridx = 0;
        gbc.gridy = 2;
        panel.add(new JLabel("确认密码:"), gbc);
        gbc.gridx = 1;
        panel.add(confirmPasswordField, gbc);

        // 验证码
        gbc.gridx = 0;
        gbc.gridy = 3;
        panel.add(new JLabel("验证码:"), gbc);
        gbc.gridx = 1;
        JPanel captchaPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        captchaPanel.add(captchaField);
        captchaPanel.add(captchaLabel);
        JButton refreshCaptcha = new JButton("刷新");
        refreshCaptcha.addActionListener(e -> refreshCaptcha());
        captchaPanel.add(refreshCaptcha);
        panel.add(captchaPanel, gbc);

        // 按钮
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 10));
        JButton registerButton = new JButton("注册");
        registerButton.addActionListener(this::performRegistration);
        JButton cancelButton = new JButton("取消");
        cancelButton.addActionListener(e -> setVisible(false));
        buttonPanel.add(registerButton);
        buttonPanel.add(cancelButton);

        add(panel, BorderLayout.CENTER);
        add(buttonPanel, BorderLayout.SOUTH);

        // 初始化验证码
        refreshCaptcha();

        pack();
        setLocationRelativeTo(getOwner());
    }

    private void refreshCaptcha() {
        captchaText = generateCaptchaString(4);
        captchaLabel.setText(captchaText);
        captchaLabel.setFont(new Font("Arial", Font.BOLD | Font.ITALIC, 20));
        // 强制对话框重新计算其所有组件的布局和大小，以解决顽固的渲染问题
        pack();
    }

    private String generateCaptchaString(int length) {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
        StringBuilder sb = new StringBuilder();
        Random random = new Random();
        for (int i = 0; i < length; i++) {
            sb.append(chars.charAt(random.nextInt(chars.length())));
        }
        return sb.toString();
    }

    private void performRegistration(ActionEvent e) {
        String username = usernameField.getText();
        String password = new String(passwordField.getPassword());
        String confirmPassword = new String(confirmPasswordField.getPassword());
        String captchaInput = captchaField.getText();

        if (username.isEmpty() || password.isEmpty()) {
            JOptionPane.showMessageDialog(this, "用户名和密码不能为空。", "错误", JOptionPane.ERROR_MESSAGE);
            return;
        }

        if (!password.equals(confirmPassword)) {
            JOptionPane.showMessageDialog(this, "两次输入的密码不一致。", "错误", JOptionPane.ERROR_MESSAGE);
            return;
        }

        if (!captchaText.equalsIgnoreCase(captchaInput)) {
            JOptionPane.showMessageDialog(this, "验证码不正确。", "错误", JOptionPane.ERROR_MESSAGE);
            refreshCaptcha();
            return;
        }

        LoadingDialog loadingDialog = new LoadingDialog(this);

        SwingWorker<BackendResponse<Object>, Void> worker = new SwingWorker<BackendResponse<Object>, Void>() {
            @Override
            protected BackendResponse<Object> doInBackground() throws Exception {
                return accountService.register(username, password);
            }

            @Override
            protected void done() {
                loadingDialog.setVisible(false);
                loadingDialog.dispose();
                try {
                    BackendResponse<Object> response = get();
                    if (response.getCode() == 200) {
                        JOptionPane.showMessageDialog(RegisterDialog.this, "注册成功！", "成功", JOptionPane.INFORMATION_MESSAGE);
                        setVisible(false);
                    } else {
                        JOptionPane.showMessageDialog(RegisterDialog.this, "注册失败: " + response.getMessage(), "错误", JOptionPane.ERROR_MESSAGE);
                        refreshCaptcha();
                    }
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(RegisterDialog.this, "注册过程中发生错误: " + ex.getMessage(), "严重错误", JOptionPane.ERROR_MESSAGE);
                    ex.printStackTrace();
                    refreshCaptcha();
                }
            }
        };

        worker.execute();
        loadingDialog.setVisible(true);
    }
}
