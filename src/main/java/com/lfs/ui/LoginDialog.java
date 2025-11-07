package com.lfs.ui;

import com.lfs.domain.BackendResponse;
import com.lfs.domain.CaptchaResponse;
import com.lfs.service.AccountService;
import com.lfs.service.UserPreferencesService;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;

public class LoginDialog extends JDialog {

    private final JTextField usernameField = new JTextField(20);
    private final JPasswordField passwordField = new JPasswordField(20);
    private final JTextField captchaField = new JTextField(10);
    private final JLabel captchaLabel = new JLabel();
    private String captchaId;

    private final AccountService accountService = new AccountService();
    private final UserPreferencesService userPreferencesService = new UserPreferencesService();

    public LoginDialog(Frame owner) {
        super(owner, "用户登录", true);
        initUI();
        refreshCaptcha();
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

        // 验证码
        gbc.gridx = 0;
        gbc.gridy = 2;
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
        JButton loginButton = new JButton("登录");
        loginButton.addActionListener(this::performLogin);
        JButton cancelButton = new JButton("取消");
        cancelButton.addActionListener(e -> {
            // distinguish between cancel in startup and cancel from menu
            if (getOwner() == null) {
                System.exit(0);
            } else {
                setVisible(false);
            }
        });
        buttonPanel.add(loginButton);
        buttonPanel.add(cancelButton);

        add(panel, BorderLayout.CENTER);
        add(buttonPanel, BorderLayout.SOUTH);

        pack();
        setLocationRelativeTo(getOwner());
    }

    private void refreshCaptcha() {
        SwingWorker<CaptchaResponse, Void> worker = new SwingWorker<>() {
            @Override
            protected CaptchaResponse doInBackground() throws Exception {
                return accountService.getCaptcha();
            }

            @Override
            protected void done() {
                try {
                    CaptchaResponse response = get();
                    captchaId = response.getCaptchaId();
                    byte[] imageBytes = response.getImageData();
                    BufferedImage image = ImageIO.read(new ByteArrayInputStream(imageBytes));
                    captchaLabel.setIcon(new ImageIcon(image));
                    pack();
                } catch (Exception e) {
                    JOptionPane.showMessageDialog(LoginDialog.this, "获取验证码时发生错误: " + e.getMessage(), "严重错误", JOptionPane.ERROR_MESSAGE);
                }
            }
        };
        worker.execute();
    }

    private void performLogin(ActionEvent e) {
        String username = usernameField.getText();
        String password = new String(passwordField.getPassword());
        String captcha = captchaField.getText();

        if (username.isEmpty() || password.isEmpty() || captcha.isEmpty()) {
            JOptionPane.showMessageDialog(this, "请填写所有字段。", "错误", JOptionPane.ERROR_MESSAGE);
            return;
        }

        LoadingDialog loadingDialog = new LoadingDialog(this);

        SwingWorker<BackendResponse<String>, Void> worker = new SwingWorker<>() {
            @Override
            protected BackendResponse<String> doInBackground() throws Exception {
                return accountService.login(username, password, captcha, captchaId);
            }

            @Override
            protected void done() {
                loadingDialog.setVisible(false);
                loadingDialog.dispose();
                try {
                    BackendResponse<String> response = get();
                    if (response.getCode() == 200) {
                        String token = response.getData();
                        userPreferencesService.saveToken(token);
                        JOptionPane.showMessageDialog(LoginDialog.this, "登录成功！", "成功", JOptionPane.INFORMATION_MESSAGE);
                        setVisible(false);
                    } else {
                        JOptionPane.showMessageDialog(LoginDialog.this, "登录失败: " + response.getMessage(), "错误", JOptionPane.ERROR_MESSAGE);
                        refreshCaptcha();
                    }
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(LoginDialog.this, "登录过程中发生错误: " + ex.getMessage(), "严重错误", JOptionPane.ERROR_MESSAGE);
                    ex.printStackTrace();
                    refreshCaptcha();
                }
            }
        };

        worker.execute();
        loadingDialog.setVisible(true);
    }
}
