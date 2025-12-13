package com.lfs.ui;

import com.lfs.domain.BackendResponse;
import com.lfs.service.AccountService;
import com.lfs.util.NotificationUtil;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;

/**
 * 修改密码对话框
 */
public class ChangePasswordDialog extends JDialog {

    private final JPasswordField oldPasswordField = new JPasswordField(20);
    private final JPasswordField newPasswordField = new JPasswordField(20);
    private final JPasswordField confirmPasswordField = new JPasswordField(20);

    private final AccountService accountService = new AccountService();

    public ChangePasswordDialog(Frame owner) {
        super(owner, "修改密码", true);
        initUI();
    }

    private void initUI() {
        setLayout(new BorderLayout(15, 15));
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // 旧密码
        gbc.gridx = 0;
        gbc.gridy = 0;
        panel.add(new JLabel("旧密码:"), gbc);
        gbc.gridx = 1;
        panel.add(oldPasswordField, gbc);

        // 新密码
        gbc.gridx = 0;
        gbc.gridy = 1;
        panel.add(new JLabel("新密码:"), gbc);
        gbc.gridx = 1;
        panel.add(newPasswordField, gbc);

        // 确认新密码
        gbc.gridx = 0;
        gbc.gridy = 2;
        panel.add(new JLabel("确认新密码:"), gbc);
        gbc.gridx = 1;
        panel.add(confirmPasswordField, gbc);

        // 按钮
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 10));
        JButton submitButton = new JButton("确认修改");
        submitButton.addActionListener(this::performChangePassword);
        JButton cancelButton = new JButton("取消");
        cancelButton.addActionListener(e -> setVisible(false));
        buttonPanel.add(submitButton);
        buttonPanel.add(cancelButton);

        add(panel, BorderLayout.CENTER);
        add(buttonPanel, BorderLayout.SOUTH);

        pack();
        setLocationRelativeTo(getOwner());
    }

    private void performChangePassword(ActionEvent e) {
        String oldPass = new String(oldPasswordField.getPassword());
        String newPass = new String(newPasswordField.getPassword());
        String confirmPass = new String(confirmPasswordField.getPassword());

        if (oldPass.isEmpty() || newPass.isEmpty()) {
            NotificationUtil.showErrorDialog(this, "密码不能为空。");
            return;
        }

        if (newPass.length() < 6) {
            NotificationUtil.showErrorDialog(this, "新密码长度不能少于6位。");
            return;
        }

        if (!newPass.equals(confirmPass)) {
            NotificationUtil.showErrorDialog(this, "两次输入的新密码不一致。");
            return;
        }

        LoadingDialog loadingDialog = new LoadingDialog(this);

        SwingWorker<BackendResponse<Object>, Void> worker = new SwingWorker<>() {
            @Override
            protected BackendResponse<Object> doInBackground() {
                return accountService.changePassword(oldPass, newPass);
            }

            @Override
            protected void done() {
                loadingDialog.dispose();
                try {
                    BackendResponse<Object> response = get();
                    if (response != null && response.getCode() == 200) {
                        NotificationUtil.showSuccessDialog(ChangePasswordDialog.this, "密码修改成功！");
                        setVisible(false);
                    } else {
                        String msg = (response != null) ? response.getMessage() : "未知错误";
                        NotificationUtil.showErrorDialog(ChangePasswordDialog.this, "修改失败: " + msg);
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                    NotificationUtil.showErrorDialog(ChangePasswordDialog.this, "请求发生错误: " + ex.getMessage());
                }
            }
        };

        worker.execute();
        loadingDialog.setVisible(true);
    }
}