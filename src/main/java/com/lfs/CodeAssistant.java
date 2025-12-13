package com.lfs;

import com.formdev.flatlaf.FlatDarculaLaf;
import com.lfs.ui.LoadingSpinner;
import com.lfs.ui.MainFrame;
import lombok.extern.slf4j.Slf4j;

import javax.swing.*;
import java.awt.*;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

@Slf4j
public class CodeAssistant {

    private static final int PORT = 60123; // 定义用于单例检测的端口
    private static MainFrame mainFrameInstance; // 用于从服务器线程访问主窗口

    public static void main(String[] args) {
        // 尝试连接到现有实例
        // 修复: 使用 getLoopbackAddress() 代替 getLocalHost() 以避免 Windows 下的主机名解析延迟
        try (Socket clientSocket = new Socket(InetAddress.getLoopbackAddress(), PORT)) {
            // 如果连接成功，说明已有实例在运行
            log.info("检测到正在运行的实例，传递参数并退出。");
            try (PrintWriter out = new PrintWriter(new OutputStreamWriter(clientSocket.getOutputStream()), true)) {
                // 发送当前工作目录和命令行参数
                out.println(System.getProperty("user.dir"));
                if (args.length > 0) {
                    out.println(args[0]);
                }
            }
            System.exit(0); // 退出当前实例
        } catch (IOException e) {
            // 如果连接失败，说明没有实例在运行，则当前实例成为主实例
            log.info("未检测到正在运行的实例，启动新实例。");
            startApplication(args);
        }
    }

    private static void startApplication(String[] args) {
        // 1. 在创建任何Swing组件之前设置观感
        FlatDarculaLaf.setup();

        // 2. 创建并显示包含加载动画的启动窗口
        final JWindow splash = new JWindow();
        LoadingSpinner spinner = new LoadingSpinner();
        spinner.setPreferredSize(new Dimension(100, 100)); // 给加载动画一个合适的尺寸
        splash.getContentPane().add(spinner);
        splash.pack();
        splash.setLocationRelativeTo(null); // 屏幕居中
        splash.setVisible(true);

        // 使用 AtomicReference 来持有 MainFrame 实例，以便在不同线程间安全共享
        AtomicReference<MainFrame> mainFrameRef = new AtomicReference<>();

        // 启动服务器线程来监听后续的实例启动
        startSingleInstanceServer(mainFrameRef);

        // 3. 在事件调度线程中初始化主UI
        SwingUtilities.invokeLater(() -> {
            System.setProperty("sun.jnu.encoding", "UTF-8");

            // 主窗口实例
            mainFrameInstance = new MainFrame();
            mainFrameRef.set(mainFrameInstance); // 将实例存入 AtomicReference

            // 检查是否支持桌面API以及文件打开操作
            if (java.awt.Desktop.isDesktopSupported()) {
                java.awt.Desktop desktop = java.awt.Desktop.getDesktop();
                if (desktop.isSupported(java.awt.Desktop.Action.APP_OPEN_FILE)) {
                    desktop.setOpenFileHandler(e -> {
                        List<File> files = e.getFiles();
                        for (File file : files) {
                            SwingUtilities.invokeLater(() -> mainFrameInstance.openFile(file));
                        }
                    });
                }
            }

            // 4. 显示主窗口并关闭启动画面
            mainFrameInstance.setVisible(true);
            splash.dispose();

            // 5. 懒加载文件浏览器
            mainFrameInstance.getFileExplorerPanel().lazyLoadInitialDirectory(null);

            // 6. 处理通过命令行参数传递的文件
            if (args.length > 0) {
                mainFrameInstance.openFile(new java.io.File(args[0]));
            }
        });
    }

    private static void startSingleInstanceServer(AtomicReference<MainFrame> mainFrameRef) {
        new Thread(() -> {
            // 同样在服务端绑定时使用 getLoopbackAddress()
            try (ServerSocket serverSocket = new ServerSocket(PORT, 50, InetAddress.getLoopbackAddress())) {
                System.out.println("单例服务器已启动，正在监听端口 " + PORT);
                while (!Thread.currentThread().isInterrupted()) {
                    try (Socket clientSocket = serverSocket.accept();
                         BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()))) {

                        // 读取工作目录和文件路径
                        String workingDir = in.readLine();
                        String filePath = in.readLine();

                        System.out.println("从新实例收到文件路径: " + filePath);

                        SwingUtilities.invokeLater(() -> {
                            MainFrame frame = mainFrameRef.get();
                            if (frame != null) {
                                // 将窗口置于前台
                                frame.setExtendedState(JFrame.NORMAL);
                                frame.toFront();
                                frame.requestFocus();

                                if (filePath != null && !filePath.isEmpty()) {
                                    File fileToOpen = new File(filePath);
                                    // 如果路径不是绝对路径，则结合工作目录
                                    if (!fileToOpen.isAbsolute() && workingDir != null) {
                                        fileToOpen = new File(workingDir, filePath);
                                    }
                                    frame.openFile(fileToOpen);
                                }
                            }
                        });
                    } catch (IOException e) {
                        System.err.println("处理客户端连接时出错: " + e.getMessage());
                    }
                }
            } catch (IOException e) {
                System.err.println("无法启动单例服务器，可能端口已被占用或网络配置问题: " + e.getMessage());
            }
        }, "SingleInstanceServerThread").start();
    }
}