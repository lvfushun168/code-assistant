package com.lfs.ui;

import cn.hutool.core.codec.Base64;
import cn.hutool.crypto.digest.DigestUtil;
import com.lfs.domain.BackendResponse;
import com.lfs.domain.CaptchaResponse;
import com.lfs.domain.KeyPackageResponse;
import com.lfs.service.AccountService;
import com.lfs.service.CryptoService;
import com.lfs.service.SystemKeyStoreService;
import com.lfs.service.UserPreferencesService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.crypto.AEADBadTagException;
import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.security.GeneralSecurityException;

public class LoginDialog extends JDialog {

    private static final Logger log = LoggerFactory.getLogger(LoginDialog.class);

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
     * 执行安全登录流程
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

        SwingWorker<String, String> worker = new SwingWorker<>() {
            @Override
            protected String doInBackground() throws Exception {
                publish("正在请求密钥包...");
                // 获取密钥包
                BackendResponse<KeyPackageResponse> keyPkgResp = accountService.getKeyPackage(username);
                if (keyPkgResp == null) {
                    throw new Exception("服务器无响应或网络错误");
                }
                if (keyPkgResp.getCode() != 200) {
                    throw new Exception("获取密钥包失败: " + keyPkgResp.getMessage());
                }
                KeyPackageResponse keyPkg = keyPkgResp.getData();
                if (keyPkg == null) {
                    throw new Exception("该账号未启用设备安全保护，请联系管理员。");
                }
                try {
                    log.info("Argon2 参数: Iter={}, Mem={}KB, Parallel={}", keyPkg.getIterations(), keyPkg.getMemoryCost(), keyPkg.getParallelism());
                } catch (Exception ignored) {}
                publish("正在解密安全密钥...");
                String passwordHash = DigestUtil.sha256Hex(password);
                log.info("使用密码哈希进行 KEK 派生 (Client Side)");
                // 客户端内存解密 DEK
                // KEK = Argon2(密码Hash, Salt)
                byte[] kek = cryptoService.deriveKek(
                        passwordHash,
                        keyPkg.getSalt(),
                        keyPkg.getIterations(),
                        keyPkg.getMemoryCost(),
                        keyPkg.getParallelism()
                );
                // DEK = Decrypt(Encrypted_DEK, KEK)
                byte[] dek;
                try {
                    dek = cryptoService.decryptDek(keyPkg.getEncryptedDek(), kek, keyPkg.getNonce());
                } catch (AEADBadTagException tagEx) {
                    log.error("解密 DEK 失败 (Tag Mismatch)。", tagEx);
                    throw new Exception("安全密钥校验失败！请确认密码是否正确。", tagEx);
                } catch (GeneralSecurityException secEx) {
                    throw new Exception("加密算法错误: " + secEx.getMessage(), secEx);
                }
                publish("正在安全存储密钥...");
                // 存入系统保险箱
                boolean stored = systemKeyStoreService.storeDek(dek, username);
                if (!stored) {
                    System.err.println("警告：DEK未能存入系统安全存储，可能影响下次免密体验。");
                }
                publish("正在验证身份...");
                // 常规登录获取 Token (用于后续API调用)
                BackendResponse<String> loginResp = accountService.login(username, password, captcha, captchaId);
                if (loginResp == null) {
                    throw new Exception("登录请求无响应");
                }
                if (loginResp.getCode() != 200) {
                    throw new Exception("登录验证失败: " + loginResp.getMessage());
                }
                return loginResp.getData(); // Token
            }

            @Override
            protected void process(java.util.List<String> chunks) {
            }

            @Override
            protected void done() {
                loadingDialog.setVisible(false);
                loadingDialog.dispose();

                try {
                    String token = get();
                    userPreferencesService.saveToken(token);
                    JOptionPane.showMessageDialog(LoginDialog.this, "登录成功（已启用设备级安全防护）", "成功", JOptionPane.INFORMATION_MESSAGE);

                    Window owner = getOwner();
                    if (owner instanceof MainFrame) {
                        ((MainFrame) owner).updateAccountMenu();
                    }
                    setVisible(false);
                } catch (Exception ex) {
                    String msg = ex.getMessage();
                    Throwable cause = ex.getCause();
                    if (cause != null) {
                        msg = cause.getMessage();
                    }

                    if (!(ex instanceof java.util.concurrent.CancellationException)) {
                        JOptionPane.showMessageDialog(LoginDialog.this, msg, "登录失败", JOptionPane.ERROR_MESSAGE);
                        refreshCaptcha();
                        passwordField.setText("");
                    }
                }
            }
        };

        worker.execute();
        loadingDialog.setVisible(true);
    }
}