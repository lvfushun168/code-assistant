package com.lfs.ui;

import com.lfs.config.AppConfig;
import com.lfs.service.ClipboardService;
import com.lfs.service.FileProcessorService;
import com.lfs.service.UserPreferencesService;
import com.lfs.util.NotificationUtil;

import javax.swing.*;
import javax.swing.filechooser.FileSystemView;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

public class FileExplorerPanel extends JPanel {

    private final MainFrameController controller;
    private final UserPreferencesService prefsService;
    private final FileProcessorService fileProcessorService;
    private final JTree fileTree;
    private final DefaultTreeModel treeModel;
    private final DefaultMutableTreeNode rootNode;
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

        rootNode = new DefaultMutableTreeNode();
        treeModel = new DefaultTreeModel(rootNode);
        fileTree = new JTree(treeModel);

        initUI();
        loadInitialDirectory();
    }

    private void initUI() {
        // Navigation Toolbar
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

        // File Tree
        fileTree.setRootVisible(false);
        fileTree.setShowsRootHandles(true);
        fileTree.setCellRenderer(new FileTreeCellRenderer());
        fileTree.setFont(new Font("SansSerif", Font.PLAIN, 14));
        JScrollPane scrollPane = new JScrollPane(fileTree);
        add(scrollPane, BorderLayout.CENTER);

        addListeners();
    }

    private void addListeners() {
        backButton.addActionListener(e -> back());
        forwardButton.addActionListener(e -> forward());
        upButton.addActionListener(e -> up());

        fileTree.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (e.isPopupTrigger()) {
                    showPopupMenu(e);
                }
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                if (e.isPopupTrigger()) {
                    showPopupMenu(e);
                }
            }

            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    TreePath path = fileTree.getPathForLocation(e.getX(), e.getY());
                    if (path != null) {
                        DefaultMutableTreeNode node = (DefaultMutableTreeNode) path.getLastPathComponent();
                        File file = (File) node.getUserObject();
                        if (file.isDirectory()) {
                            navigateTo(file);
                        } else {
                            openFile(file);
                        }
                    }
                }
            }
        });
    }

    private void showPopupMenu(MouseEvent e) {
        TreePath path = fileTree.getPathForLocation(e.getX(), e.getY());

        if (path == null) {
            // Background click
            JPopupMenu popupMenu = createBackgroundPopupMenu();
            popupMenu.show(e.getComponent(), e.getX(), e.getY());
        } else {
            // Item click
            fileTree.setSelectionPath(path);
            DefaultMutableTreeNode node = (DefaultMutableTreeNode) path.getLastPathComponent();
            File selectedFile = (File) node.getUserObject();
            JPopupMenu popupMenu = createItemPopupMenu(selectedFile);
            popupMenu.show(e.getComponent(), e.getX(), e.getY());
        }
    }

    private JPopupMenu createBackgroundPopupMenu() {
        JPopupMenu popupMenu = new JPopupMenu();
        File currentDir = new File(currentPathLabel.getText().trim());

        JMenuItem newFileItem = new JMenuItem("新建文档");
        newFileItem.addActionListener(evt -> createFile(currentDir));
        popupMenu.add(newFileItem);

        JMenuItem newDirItem = new JMenuItem("新建文件夹");
        newDirItem.addActionListener(evt -> createDirectory(currentDir));
        popupMenu.add(newDirItem);

        popupMenu.addSeparator();

        JMenuItem pasteItem = new JMenuItem("粘贴");
        pasteItem.addActionListener(evt -> pasteFile());
        popupMenu.add(pasteItem);
        return popupMenu;
    }

    private JPopupMenu createItemPopupMenu(File selectedFile) {
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
        copyItem.addActionListener(evt -> copyFile(selectedFile));
        popupMenu.add(copyItem);

        return popupMenu;
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
            if (fileProcessorService.createDirectory(parentDir, dirName) != null) {
                refresh();
            }
        }
    }

    private void renameFile(File oldFile) {
        String newName = (String) JOptionPane.showInputDialog(this, "请输入新名称:", "重命名", JOptionPane.PLAIN_MESSAGE, null, null, oldFile.getName());
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

    private void copyFile(File selectedFile) {
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
        navigateTo(currentDir, true); // Force refresh
    }

    private void loadInitialDirectory() {
        File lastDir = prefsService.getFileExplorerLastDirectory();
        File initialDir = (lastDir != null && lastDir.exists()) ? lastDir : new File(System.getProperty("user.home"));
        navigateTo(initialDir, true);
    }

    private void navigateTo(File directory) {
        navigateTo(directory, false);
    }

    private void navigateTo(File directory, boolean isRefresh) {
        if (directory == null || !directory.isDirectory()) {
            return;
        }

        setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        new SwingWorker<File[], Void>() {
            @Override
            protected File[] doInBackground() {
                return directory.listFiles();
            }

            @Override
            protected void done() {
                try {
                    File[] files = get();
                    if (files == null) return;

                    Arrays.sort(files, (f1, f2) -> {
                        if (f1.isDirectory() && !f2.isDirectory()) return -1;
                        if (!f1.isDirectory() && f2.isDirectory()) return 1;
                        return f1.getName().compareToIgnoreCase(f2.getName());
                    });

                    rootNode.removeAllChildren();
                    for (File file : files) {
                        rootNode.add(new DefaultMutableTreeNode(file));
                    }
                    treeModel.reload(rootNode);

                    currentPathLabel.setText(" " + directory.getAbsolutePath());
                    prefsService.saveFileExplorerLastDirectory(directory);

                    if (!isRefresh) {
                        // Update history
                        while (history.size() > historyIndex + 1) {
                            history.remove(history.size() - 1);
                        }
                        history.add(directory);
                        historyIndex++;
                    }
                    updateNavigationButtons();
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    setCursor(Cursor.getDefaultCursor());
                }
            }
        }.execute();
    }

    private void back() {
        if (historyIndex > 0) {
            historyIndex--;
            File dir = history.get(historyIndex);
            navigateTo(dir, true); // 无痕浏览
        }
    }

    private void forward() {
        if (historyIndex < history.size() - 1) {
            historyIndex++;
            File dir = history.get(historyIndex);
            navigateTo(dir, true); // 无痕浏览
        }
    }

    private void up() {
        File currentDir = new File(currentPathLabel.getText().trim());
        File parentDir = currentDir.getParentFile();
        if (parentDir != null) {
            navigateTo(parentDir);
        }
    }

    private void updateNavigationButtons() {
        backButton.setEnabled(historyIndex > 0);
        forwardButton.setEnabled(historyIndex < history.size() - 1);
        File currentDir = new File(currentPathLabel.getText().trim());
        upButton.setEnabled(currentDir.getParentFile() != null);
    }

    private void openFile(File file) {
        if (file != null && file.isFile()) {
            controller.onFileSelected(file);
        }
    }

    private static class FileTreeCellRenderer extends DefaultTreeCellRenderer {
        private final FileSystemView fileSystemView = FileSystemView.getFileSystemView();

        @Override
        public Component getTreeCellRendererComponent(JTree tree, Object value, boolean sel, boolean expanded, boolean leaf, int row, boolean hasFocus) {
            super.getTreeCellRendererComponent(tree, value, sel, expanded, leaf, row, hasFocus);
            if (value instanceof DefaultMutableTreeNode) {
                DefaultMutableTreeNode node = (DefaultMutableTreeNode) value;
                if (node.getUserObject() instanceof File) {
                    File file = (File) node.getUserObject();
                    setText(fileSystemView.getSystemDisplayName(file));
                    setIcon(fileSystemView.getSystemIcon(file));
                }
            }
            return this;
        }
    }
}
