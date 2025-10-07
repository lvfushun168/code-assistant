package com.lfs.ui;

import com.lfs.config.AppConfig;
import com.lfs.service.UserPreferencesService;

import javax.swing.*;
import javax.swing.filechooser.FileSystemView;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

public class FileExplorerPanel extends JPanel {

    private final MainFrameController controller;
    private final UserPreferencesService prefsService;
    private final DefaultListModel<File> listModel = new DefaultListModel<>();
    private final JList<File> fileList = new JList<>(listModel);
    private final JLabel currentPathLabel = new JLabel();
    private final List<File> history = new ArrayList<>();
    private int historyIndex = -1;

    private JButton backButton;
    private JButton forwardButton;
    private JButton upButton;

    public FileExplorerPanel(MainFrameController controller) {
        super(new BorderLayout());
        this.controller = controller;
        this.prefsService = new UserPreferencesService();
        initUI();
        loadInitialDirectory();
    }

    private void initUI() {
        // 导航工具栏
        JToolBar toolBar = new JToolBar();
        toolBar.setFloatable(false);
        toolBar.setLayout(new BorderLayout());

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        backButton = new JButton("<");
        forwardButton = new JButton(">");
        upButton = new JButton("↑");

        backButton.setToolTipText("后退");
        forwardButton.setToolTipText("前进");
        upButton.setToolTipText("上一级");

        buttonPanel.add(backButton);
        buttonPanel.add(forwardButton);
        buttonPanel.add(upButton);

        toolBar.add(buttonPanel, BorderLayout.WEST);
        toolBar.add(currentPathLabel, BorderLayout.CENTER);

        add(toolBar, BorderLayout.NORTH);

        // 文件列表
        fileList.setCellRenderer(new FileListCellRenderer());
        fileList.setFont(new Font("SansSerif", Font.PLAIN, 14));
        JScrollPane scrollPane = new JScrollPane(fileList);
        add(scrollPane, BorderLayout.CENTER);

        // 添加监听器
        addListeners();
    }

    private void addListeners() {
        backButton.addActionListener(e -> back());
        forwardButton.addActionListener(e -> forward());
        upButton.addActionListener(e -> up());

        fileList.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    int index = fileList.locationToIndex(e.getPoint());
                    if (index >= 0) {
                        File file = listModel.getElementAt(index);
                        if (file.isDirectory()) {
                            navigateTo(file);
                        } else if (file.isFile()) {
                            openFile(file);
                        }
                    }
                }
            }
        });
    }

    private void loadInitialDirectory() {
        File lastDir = prefsService.getFileExplorerLastDirectory();
        File initialDir = (lastDir != null && lastDir.exists()) ? lastDir : new File(System.getProperty("user.home"));
        navigateTo(initialDir, true);
    }

    private void navigateTo(File directory) {
        navigateTo(directory, false);
    }

    private void navigateTo(File directory, boolean isInitial) {
        if (directory == null || !directory.isDirectory()) {
            return;
        }

        File[] files = directory.listFiles();
        if (files == null) {
            // 此处可以显示错误消息
            return;
        }

        listModel.clear();
        Arrays.sort(files, Comparator.comparing(File::getName));
        Arrays.sort(files, Comparator.comparing(f -> !f.isDirectory())); // Folders first

        for (File file : files) {
            listModel.addElement(file);
        }

        currentPathLabel.setText(" " + directory.getAbsolutePath());
        prefsService.saveFileExplorerLastDirectory(directory);

        if (!isInitial) {
            // 清除前进历史
            while (history.size() > historyIndex + 1) {
                history.remove(history.size() - 1);
            }
            history.add(directory);
            historyIndex++;
        } else {
            history.add(directory);
            historyIndex = 0;
        }
        updateNavigationButtons();
    }

    private void back() {
        if (historyIndex > 0) {
            historyIndex--;
            File dir = history.get(historyIndex);
            navigateToHistory(dir);
        }
    }

    private void forward() {
        if (historyIndex < history.size() - 1) {
            historyIndex++;
            File dir = history.get(historyIndex);
            navigateToHistory(dir);
        }
    }

    private void up() {
        File currentDir = new File(currentPathLabel.getText().trim());
        File parentDir = currentDir.getParentFile();
        if (parentDir != null) {
            navigateTo(parentDir);
        }
    }

    private void navigateToHistory(File directory) {
        listModel.clear();
        File[] files = directory.listFiles();
        if (files != null) {
            Arrays.sort(files, Comparator.comparing(File::getName));
            Arrays.sort(files, Comparator.comparing(f -> !f.isDirectory())); // Folders first
            for (File file : files) {
                listModel.addElement(file);
            }
        }
        currentPathLabel.setText(" " + directory.getAbsolutePath());
        prefsService.saveFileExplorerLastDirectory(directory);
        updateNavigationButtons();
    }

    private void updateNavigationButtons() {
        backButton.setEnabled(historyIndex > 0);
        forwardButton.setEnabled(historyIndex < history.size() - 1);
        File currentDir = new File(currentPathLabel.getText().trim());
        upButton.setEnabled(currentDir.getParentFile() != null);
    }

    private void openFile(File file) {
        String fileName = file.getName();
        int lastDotIndex = fileName.lastIndexOf('.');
        if (lastDotIndex > 0) {
            String extension = fileName.substring(lastDotIndex + 1).toLowerCase();
            if (AppConfig.ALLOWED_EXTENSIONS.contains(extension)) {
                controller.onFileSelected(file);
            }
        }
    }

    private static class FileListCellRenderer extends DefaultListCellRenderer {
        private final FileSystemView fileSystemView = FileSystemView.getFileSystemView();

        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
            super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            if (value instanceof File) {
                File file = (File) value;
                setText(fileSystemView.getSystemDisplayName(file));
                setIcon(fileSystemView.getSystemIcon(file));
            }
            return this;
        }
    }
}
