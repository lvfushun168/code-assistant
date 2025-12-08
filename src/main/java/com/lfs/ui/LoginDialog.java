package com.lfs.ui;

import com.lfs.domain.BackendResponse;
import com.lfs.domain.CaptchaResponse;
import com.lfs.domain.KeyPackageResponse;
import com.lfs.service.AccountService;
import com.lfs.service.CryptoService;
import com.lfs.service.SystemKeyStoreService;
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
    private final CryptoService cryptoService = new CryptoService();
    private final SystemKeyStoreService systemKeyStoreService = new SystemKeyStoreService();

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
        loginButton.addActionListener(this::performSecureLogin);
        JButton cancelButton = new JButton("取消");
        cancelButton.addActionListener(e -> {
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

    /**
     * 执行安全登录流程：
     * 1. 验证码校验 & 用户名密码基础校验
     * 2. 获取密钥包 [Salt, Encrypted_DEK]
     * 3. 在内存中派生 KEK 并解密出 DEK
     * 4. 请求系统存储 DEK (Windows Hello/TPM)
     * 5. 执行常规登录获取 API Token
     */
    private void performSecureLogin(ActionEvent e) {
        String username = usernameField.getText();
        String password = new String(passwordField.getPassword());
        String captcha = captchaField.getText();

        if (username.isEmpty() || password.isEmpty() || captcha.isEmpty()) {
            JOptionPane.showMessageDialog(this, "请填写所有字段。", "错误", JOptionPane.ERROR_MESSAGE);
            return;
        }

        LoadingDialog loadingDialog = new LoadingDialog(this);
        loadingDialog.setVisible(true);

        SwingWorker<String, String> worker = new SwingWorker<>() {
            @Override
            protected String doInBackground() throws Exception {
                publish("正在请求密钥包...");
                // 步骤 1: 获取密钥包
                BackendResponse<KeyPackageResponse> keyPkgResp = accountService.getKeyPackage(username);
                if (keyPkgResp.getCode() != 200) {
                    throw new Exception("获取密钥包失败: " + keyPkgResp.getMessage());
                }
                KeyPackageResponse keyPkg = keyPkgResp.getData();

                publish("正在解密安全密钥...");
                // 步骤 2: 客户端内存解密 DEK
                // a. KEK = Argon2(密码, Salt)
                byte[] kek = cryptoService.deriveKek(password, keyPkg.getSalt());
                // b. DEK = Decrypt(Encrypted_DEK, KEK)
                byte[] dek = cryptoService.decryptDek(keyPkg.getEncryptedDek(), kek, keyPkg.getNonce());

                publish("正在安全存储密钥...");
                // 步骤 3: 存入系统保险箱
                boolean stored = systemKeyStoreService.storeDek(dek, username);
                if (!stored) {
                    // 这里可以选择仅仅警告，或者阻断
                    System.err.println("警告：DEK未能存入系统安全存储，可能影响下次免密体验。");
                }

                publish("正在验证身份...");
                // 步骤 4: 常规登录获取 Token (用于后续API调用)
                BackendResponse<String> loginResp = accountService.login(username, password, captcha, captchaId);
                if (loginResp.getCode() != 200) {
                    throw new Exception("登录验证失败: " + loginResp.getMessage());
                }

                return loginResp.getData(); // Token
            }

            @Override
            protected void process(java.util.List<String> chunks) {
                // 可以在 LoadingDialog 上显示具体步骤，这里暂略
            }

            @Override
            protected void done() {
                loadingDialog.setVisible(false);
                loadingDialog.dispose();
                try {
                    String token = get();
                    userPreferencesService.saveToken(token);
                    JOptionPane.showMessageDialog(LoginDialog.this, "登录成功，且已启用设备级安全保护！", "成功", JOptionPane.INFORMATION_MESSAGE);

                    Window owner = getOwner();
                    if (owner instanceof MainFrame) {
                        ((MainFrame) owner).updateAccountMenu();
                    }
                    setVisible(false);
                } catch (Exception ex) {
                    String msg = ex.getMessage();
                    if (ex.getCause() != null) {
                        msg = ex.getCause().getMessage();
                    }
                    JOptionPane.showMessageDialog(LoginDialog.this, msg, "登录失败", JOptionPane.ERROR_MESSAGE);
                    // 只有在非密钥错误的情况下才刷新验证码，但为了简单起见，统一刷新
                    refreshCaptcha();
                }
            }
        };

        worker.execute();
    }
}