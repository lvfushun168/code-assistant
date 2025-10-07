package com.lfs.ui;

import com.lfs.config.AppConfig;
import com.lfs.service.ClipboardService;
import com.lfs.service.FileProcessorService;
import com.lfs.service.UserPreferencesService;
import com.lfs.util.NotificationUtil;

import javax.swing.*;
import javax.swing.filechooser.FileSystemView;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

public class FileExplorerPanel extends JPanel {

    private final MainFrameController controller;
    private final UserPreferencesService prefsService;
    private final FileProcessorService fileProcessorService;
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
        this.fileProcessorService = new FileProcessorService();
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
                int index = fileList.locationToIndex(e.getPoint());
                if (index < 0) {
                    return;
                }
                File file = listModel.getElementAt(index);
                // 双击打开
                if (e.getClickCount() == 2) {
                    if (file.isDirectory()) {
                        navigateTo(file);
                    } else if (file.isFile()) {
                        openFile(file);
                    }
                }
            }

            @Override
            public void mousePressed(MouseEvent e) {
                showPopupMenu(e);
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                showPopupMenu(e);
            }
        });

        // 快捷键
        int shortcutMask = Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx();
        fileList.getInputMap(JComponent.WHEN_FOCUSED).put(KeyStroke.getKeyStroke(KeyEvent.VK_C, shortcutMask), "copy");
        fileList.getActionMap().put("copy", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                copyFile();
            }
        });

        fileList.getInputMap(JComponent.WHEN_FOCUSED).put(KeyStroke.getKeyStroke(KeyEvent.VK_V, shortcutMask), "paste");
        fileList.getActionMap().put("paste", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                pasteFile();
            }
        });
    }

    private void showPopupMenu(MouseEvent e) {
        if (!e.isPopupTrigger()) {
            return;
        }

        int index = fileList.locationToIndex(e.getPoint());
        if (index < 0) {
            return;
        }
        fileList.setSelectedIndex(index);
        File selectedFile = fileList.getSelectedValue();

        JPopupMenu popupMenu = new JPopupMenu();

        if (selectedFile.isFile()) {
            JMenuItem openItem = new JMenuItem("打开");
            openItem.addActionListener(evt -> openFile(selectedFile));
            popupMenu.add(openItem);

            JMenuItem openReadOnlyItem = new JMenuItem("只读打开");
            openReadOnlyItem.addActionListener(evt -> openFileReadOnly(selectedFile));
            popupMenu.add(openReadOnlyItem);
        } else if (selectedFile.isDirectory()) {
            JMenuItem newFileItem = new JMenuItem("新建文档");
            newFileItem.addActionListener(evt -> createFile(selectedFile));
            popupMenu.add(newFileItem);

            JMenuItem newDirItem = new JMenuItem("新建文件夹");
            newDirItem.addActionListener(evt -> createDirectory(selectedFile));
            popupMenu.add(newDirItem);
        }

        popupMenu.addSeparator();

        JMenuItem renameItem = new JMenuItem("重命名");
        renameItem.addActionListener(evt -> renameFile(selectedFile));
        popupMenu.add(renameItem);

        JMenuItem deleteItem = new JMenuItem("删除");
        deleteItem.addActionListener(evt -> deleteFile(selectedFile));
        popupMenu.add(deleteItem);

        popupMenu.addSeparator();

        JMenuItem copyItem = new JMenuItem("复制");
        copyItem.addActionListener(evt -> copyFile());
        popupMenu.add(copyItem);

        JMenuItem pasteItem = new JMenuItem("粘贴");
        pasteItem.addActionListener(evt -> pasteFile());
        popupMenu.add(pasteItem);

        popupMenu.show(e.getComponent(), e.getX(), e.getY());
    }

    private void openFileReadOnly(File file) {
        if (file != null && file.isFile()) {
            controller.onFileSelectedReadOnly(file);
        }
    }

    private void createFile(File parentDir) {
        String fileName = JOptionPane.showInputDialog(this, "请输入新文档名称:", "新建文档", JOptionPane.PLAIN_MESSAGE);
        if (fileName != null && !fileName.trim().isEmpty()) {
            try {
                fileProcessorService.createFile(parentDir, fileName);
                refresh();
            } catch (IOException ex) {
                NotificationUtil.showErrorDialog(this, "创建文件失败: " + ex.getMessage());
            }
        }
    }

    private void createDirectory(File parentDir) {
        String dirName = JOptionPane.showInputDialog(this, "请输入新文件夹名称:", "新建文件夹", JOptionPane.PLAIN_MESSAGE);
        if (dirName != null && !dirName.trim().isEmpty()) {
            fileProcessorService.createDirectory(parentDir, dirName);
            refresh();
        }
    }

    private void renameFile(File oldFile) {
        String newName = JOptionPane.showInputDialog(this, "请输入新名称:", "重命名", JOptionPane.PLAIN_MESSAGE, null, null, oldFile.getName()).toString();
        if (newName != null && !newName.trim().isEmpty()) {
            if (fileProcessorService.renameFile(oldFile, newName)) {
                refresh();
            } else {
                NotificationUtil.showErrorDialog(this, "重命名失败");
            }
        }
    }

    private void deleteFile(File file) {
        int result = JOptionPane.showConfirmDialog(this, "确定要删除 '" + file.getName() + "' 吗?", "确认删除", JOptionPane.YES_NO_OPTION);
        if (result == JOptionPane.YES_OPTION) {
            if (fileProcessorService.deleteFile(file)) {
                refresh();
            } else {
                NotificationUtil.showErrorDialog(this, "删除失败");
            }
        }
    }

    private void copyFile() {
        File selectedFile = fileList.getSelectedValue();
        if (selectedFile != null) {
            ClipboardService.copyToClipboard(selectedFile.getAbsolutePath());
            NotificationUtil.showSuccessDialog(this, "已复制: " + selectedFile.getName());
        }
    }

    private void pasteFile() {
        String path = ClipboardService.getClipboardContents();
        if (path == null || path.trim().isEmpty()) {
            NotificationUtil.showErrorDialog(this, "剪贴板为空");
            return;
        }

        File fileToCopy = new File(path);
        if (!fileToCopy.exists()) {
            NotificationUtil.showErrorDialog(this, "源文件不存在");
            return;
        }

        File destDir = new File(currentPathLabel.getText().trim());
        if (!destDir.isDirectory()) {
            destDir = destDir.getParentFile();
        }

        try {
            File newFile = new File(destDir, fileToCopy.getName());
            if (fileToCopy.isDirectory()) {
                // 如果是文件夹，则需要递归复制
                fileProcessorService.copyFile(fileToCopy, newFile);
            } else {
                Files.copy(fileToCopy.toPath(), newFile.toPath());
            }
            refresh();
        } catch (IOException ex) {
            NotificationUtil.showErrorDialog(this, "粘贴失败: " + ex.getMessage());
        }
    }

    private void refresh() {
        File currentDir = new File(currentPathLabel.getText().trim());
        navigateToHistory(currentDir);
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
