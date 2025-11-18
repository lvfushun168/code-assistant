package com.lfs.ui;

import com.lfs.config.AppConfig;
import com.lfs.domain.ContentResponse;
import com.lfs.domain.DirTreeResponse;
import com.lfs.service.*;
import com.lfs.util.NotificationUtil;

import javax.swing.*;
import javax.swing.filechooser.FileSystemView;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
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
    private JTabbedPane tabbedPane;
    private JPanel cloudPanel;

    // Cloud components
    private JTree cloudFileTree;
    private DefaultTreeModel cloudTreeModel;
    private DefaultMutableTreeNode cloudRootNode;
    private DirService dirService;
    private DirTreeResponse cloudApiRoot;


    public FileExplorerPanel(MainFrameController controller) {
        super(new BorderLayout());
        this.controller = controller;
        this.prefsService = new UserPreferencesService();
        this.fileProcessorService = new FileProcessorService();
        this.dirService = new DirService();

        rootNode = new DefaultMutableTreeNode();
        treeModel = new DefaultTreeModel(rootNode);
        fileTree = new JTree(treeModel);

        initUI();
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

        // --- Create Tabbed Pane ---
        tabbedPane = new JTabbedPane();

        // --- Local Panel ---
        JPanel localPanel = new JPanel(new BorderLayout());
        fileTree.setRootVisible(false);
        fileTree.setShowsRootHandles(true);
        fileTree.setCellRenderer(new FileTreeCellRenderer());
        fileTree.setFont(new Font("SansSerif", Font.PLAIN, 14));
        JScrollPane scrollPane = new JScrollPane(fileTree);
        localPanel.add(scrollPane, BorderLayout.CENTER);

        // --- Cloud Panel ---
        cloudPanel = new JPanel(new BorderLayout());
        cloudRootNode = new DefaultMutableTreeNode("云端文件");
        cloudTreeModel = new DefaultTreeModel(cloudRootNode);
        cloudFileTree = new JTree(cloudTreeModel);
        cloudFileTree.setCellRenderer(new CloudFileTreeCellRenderer());
        cloudFileTree.setRootVisible(false);
        cloudFileTree.setShowsRootHandles(true);
        JScrollPane cloudScrollPane = new JScrollPane(cloudFileTree);
        cloudPanel.add(cloudScrollPane, BorderLayout.CENTER);


        tabbedPane.addTab("本地", localPanel);
        tabbedPane.addTab("云端", cloudPanel);

        // Disable cloud tab initially
        tabbedPane.setEnabledAt(1, false);
        cloudPanel.removeAll();
        cloudPanel.add(new JLabel("请先登录", SwingConstants.CENTER), BorderLayout.CENTER);


        add(tabbedPane, BorderLayout.CENTER);


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

        cloudFileTree.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    TreePath path = cloudFileTree.getPathForLocation(e.getX(), e.getY());
                    if (path != null) {
                        DefaultMutableTreeNode node = (DefaultMutableTreeNode) path.getLastPathComponent();
                        Object userObject = node.getUserObject();
                        if (userObject instanceof ContentResponse) {
                            ContentResponse fileContent = (ContentResponse) userObject;
                            controller.onCloudFileSelected(fileContent.getTitle(), fileContent.getContent());
                        }
                    }
                }
            }

            @Override
            public void mousePressed(MouseEvent e) {
                if (e.isPopupTrigger()) {
                    showCloudPopupMenu(e);
                }
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                if (e.isPopupTrigger()) {
                    showCloudPopupMenu(e);
                }
            }
        });

        fileTree.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                // 对于 macOS，检查 Command 键；对于其他系统，检查 Control 键
                boolean isCopy = (e.getKeyCode() == KeyEvent.VK_C) && ((e.getModifiersEx() & Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx()) != 0);
                boolean isPaste = (e.getKeyCode() == KeyEvent.VK_V) && ((e.getModifiersEx() & Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx()) != 0);

                if (isCopy) {
                    TreePath path = fileTree.getSelectionPath();
                    if (path != null) {
                        DefaultMutableTreeNode node = (DefaultMutableTreeNode) path.getLastPathComponent();
                        File selectedFile = (File) node.getUserObject();
                        copyFile(selectedFile);
                    }
                } else if (isPaste) {
                    pasteFile();
                } else if (e.getKeyCode() == KeyEvent.VK_F2) {
                    TreePath path = fileTree.getSelectionPath();
                    if (path != null) {
                        DefaultMutableTreeNode node = (DefaultMutableTreeNode) path.getLastPathComponent();
                        File selectedFile = (File) node.getUserObject();
                        renameFile(selectedFile);
                    }
                } else if (e.getKeyCode() == KeyEvent.VK_BACK_SPACE || e.getKeyCode() == KeyEvent.VK_DELETE) {
                    TreePath path = fileTree.getSelectionPath();
                    if (path != null) {
                        DefaultMutableTreeNode node = (DefaultMutableTreeNode) path.getLastPathComponent();
                        File selectedFile = (File) node.getUserObject();
                        deleteFile(selectedFile);
                    }
                }
            }
        });
    }

    private void showPopupMenu(MouseEvent e) {
        TreePath path = fileTree.getPathForLocation(e.getX(), e.getY());

        if (path == null) {
            // 背景点击
            JPopupMenu popupMenu = createBackgroundPopupMenu();
            popupMenu.show(e.getComponent(), e.getX(), e.getY());
        } else {
            // 项目点击
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
            NotificationUtil.showToast(this, "已复制: " + selectedFile.getName());
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
            if (newFile.exists()) {
                newFile = getUniqueFile(destDir, fileToCopy);
            }

            if (fileToCopy.isDirectory()) {
                fileProcessorService.copyFile(fileToCopy, newFile);
            } else {
                Files.copy(fileToCopy.toPath(), newFile.toPath());
            }
            refresh();
            NotificationUtil.showToast(this, "粘贴成功！");
        } catch (IOException ex) {
            NotificationUtil.showErrorDialog(this, "粘贴失败: " + ex.getMessage());
        }
    }

    private File getUniqueFile(File destDir, File fileToCopy) {
        String name = fileToCopy.getName();
        String newName;
        int copySuffix = 1;

        int dotIndex = name.lastIndexOf('.');
        String baseName = (dotIndex == -1) ? name : name.substring(0, dotIndex);
        String extension = (dotIndex == -1) ? "" : name.substring(dotIndex);

        while (true) {
            if (copySuffix == 1) {
                newName = baseName + "_copy" + extension;
            } else {
                newName = baseName + "_copy" + copySuffix + extension;
            }
            File newFile = new File(destDir, newName);
            if (!newFile.exists()) {
                return newFile;
            }
            copySuffix++;
        }
    }

    private void refresh() {
        File currentDir = new File(currentPathLabel.getText().trim());
        navigateTo(currentDir, true); // 强制刷新
    }

    public void lazyLoadInitialDirectory(Runnable onFinished) {
        File lastDir = prefsService.getFileExplorerLastDirectory();
        File initialDir = (lastDir != null && lastDir.exists()) ? lastDir : new File(System.getProperty("user.home"));

        setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        new SwingWorker<File[], Void>() {
            @Override
            protected File[] doInBackground() {
                return initialDir.listFiles();
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

                    currentPathLabel.setText(" " + initialDir.getAbsolutePath());
                    prefsService.saveFileExplorerLastDirectory(initialDir);

                    // Update history for the initial load
                    history.clear();
                    history.add(initialDir);
                    historyIndex = 0;
                    updateNavigationButtons();

                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    setCursor(Cursor.getDefaultCursor());
                    if (onFinished != null) {
                        onFinished.run();
                    }
                }
            }
        }.execute();
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
                        // 更新历史记录
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

    public void setCloudTabEnabled(boolean enabled) {
        tabbedPane.setEnabledAt(1, enabled);
        if (enabled) {
            cloudPanel.removeAll();
            JScrollPane cloudScrollPane = new JScrollPane(cloudFileTree);
            cloudPanel.add(cloudScrollPane, BorderLayout.CENTER);
            cloudPanel.revalidate();
            cloudPanel.repaint();
            loadCloudDirectory();
        } else {
            cloudPanel.removeAll();
            cloudPanel.add(new JLabel("请先登录", SwingConstants.CENTER), BorderLayout.CENTER);
            cloudPanel.revalidate();
            cloudPanel.repaint();
        }
    }

    public void loadCloudDirectory() {
        setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        new SwingWorker<DirTreeResponse, Void>() {
            @Override
            protected DirTreeResponse doInBackground() throws Exception {
                return dirService.getDirTree();
            }

            @Override
            protected void done() {
                try {
                    cloudApiRoot = get(); // Store the root
                    cloudRootNode.removeAllChildren();
                    if (cloudApiRoot != null) {
                        // The API's root is the single visible node in our tree
                        DefaultMutableTreeNode rootTreeNode = new DefaultMutableTreeNode(cloudApiRoot);
                        cloudRootNode.add(rootTreeNode);
                        // Build out the children from this root
                        buildCloudTree(rootTreeNode, cloudApiRoot);
                    }
                    cloudTreeModel.reload(cloudRootNode);
                } catch (Exception e) {
                    NotificationUtil.showErrorDialog(FileExplorerPanel.this, "加载云端目录失败: " + e.getMessage());
                    e.printStackTrace();
                } finally {
                    setCursor(Cursor.getDefaultCursor());
                }
            }
        }.execute();
    }

    private void buildCloudTree(DefaultMutableTreeNode parent, DirTreeResponse dir) {
        // Add files first
        if (dir.getContents() != null && !dir.getContents().isEmpty()) {
            for (ContentResponse content : dir.getContents()) {
                parent.add(new DefaultMutableTreeNode(content));
            }
        }

        // Then add subdirectories
        if (dir.getChildren() != null && !dir.getChildren().isEmpty()) {
            for (DirTreeResponse child : dir.getChildren()) {
                DefaultMutableTreeNode childNode = new DefaultMutableTreeNode(child);
                parent.add(childNode);
                buildCloudTree(childNode, child); // Recurse
            }
        }
    }


    public void switchToLocalTab() {
        tabbedPane.setSelectedIndex(0);
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

    private static class CloudFileTreeCellRenderer extends DefaultTreeCellRenderer {
        @Override
        public Component getTreeCellRendererComponent(JTree tree, Object value, boolean sel, boolean expanded, boolean leaf, int row, boolean hasFocus) {
            super.getTreeCellRendererComponent(tree, value, sel, expanded, leaf, row, hasFocus);
            if (value instanceof DefaultMutableTreeNode) {
                DefaultMutableTreeNode node = (DefaultMutableTreeNode) value;
                Object userObject = node.getUserObject();

                if (userObject instanceof DirTreeResponse) {
                    DirTreeResponse dir = (DirTreeResponse) userObject;
                    setText(dir.getName());
                    setIcon(UIManager.getIcon("FileView.directoryIcon"));
                } else if (userObject instanceof ContentResponse) {
                    ContentResponse content = (ContentResponse) userObject;
                    setText(content.getTitle());
                    setIcon(UIManager.getIcon("FileView.fileIcon"));
                }
            }
                        return this;
                    }
                }
            
                private void showCloudPopupMenu(MouseEvent e) {
                    TreePath path = cloudFileTree.getPathForLocation(e.getX(), e.getY());
            
                    if (path == null) {
                        // Background click
                        JPopupMenu popupMenu = createCloudBackgroundPopupMenu();
                        popupMenu.show(e.getComponent(), e.getX(), e.getY());
                    } else {
                        // Item click
                        cloudFileTree.setSelectionPath(path);
                        DefaultMutableTreeNode node = (DefaultMutableTreeNode) path.getLastPathComponent();
                        if (node.getUserObject() instanceof DirTreeResponse) {
                            JPopupMenu popupMenu = createCloudDirPopupMenu(node);
                            popupMenu.show(e.getComponent(), e.getX(), e.getY());
                        }
                    }
                }
            
                            private JPopupMenu createCloudBackgroundPopupMenu() {
            
                                JPopupMenu popupMenu = new JPopupMenu();
            
                                JMenuItem newDirItem = new JMenuItem("新建目录");
            
                                newDirItem.addActionListener(e -> {
            
                                    if (cloudApiRoot == null) {
            
                                        NotificationUtil.showErrorDialog(this, "无法获取根目录信息，请先刷新。");
            
                                        return;
            
                                    }
            
                                    String dirName = JOptionPane.showInputDialog(this, "请输入新目录名称:", "新建目录", JOptionPane.PLAIN_MESSAGE);
            
                                    if (dirName != null && !dirName.trim().isEmpty()) {
            
                                        LoadingDialog loadingDialog = new LoadingDialog((Frame) SwingUtilities.getWindowAncestor(FileExplorerPanel.this));
            
                                        final String finalDirName = dirName;
            
                        
            
                                        SwingWorker<DirTreeResponse, Void> worker = new SwingWorker<>() {
            
                                            @Override
            
                                            protected DirTreeResponse doInBackground() throws Exception {
            
                                                return dirService.createDir(cloudApiRoot.getId(), finalDirName);
            
                                            }
            
                        
            
                                            @Override
            
                                            protected void done() {
            
                                                loadingDialog.dispose();
            
                                                try {
            
                                                    DirTreeResponse newDir = get();
            
                                                    if (newDir != null) {
            
                                                        DefaultMutableTreeNode parentNode = (DefaultMutableTreeNode) cloudRootNode.getFirstChild();
            
                                                        DefaultMutableTreeNode newNode = new DefaultMutableTreeNode(newDir);
            
                                                        cloudTreeModel.insertNodeInto(newNode, parentNode, parentNode.getChildCount());
            
                                                        // Optional: scroll to the new node
            
                                                        cloudFileTree.scrollPathToVisible(new TreePath(newNode.getPath()));
            
                                                    }
            
                                                } catch (Exception ex) {
            
                                                    ex.printStackTrace();
            
                                                }
            
                                            }
            
                                        };
            
                                        worker.execute();
            
                                        loadingDialog.setVisible(true);
            
                                    }
            
                                });
            
                                popupMenu.add(newDirItem);
            
                                return popupMenu;
            
                            }
            
                        
            
                            private JPopupMenu createCloudDirPopupMenu(DefaultMutableTreeNode node) {
            
                                JPopupMenu popupMenu = new JPopupMenu();
            
                                DirTreeResponse dir = (DirTreeResponse) node.getUserObject();
            
                        
            
                                JMenuItem newDirItem = new JMenuItem("新建目录");
            
                                newDirItem.addActionListener(e -> {
            
                                    String dirName = JOptionPane.showInputDialog(this, "请输入新目录名称:", "新建目录", JOptionPane.PLAIN_MESSAGE);
            
                                    if (dirName != null && !dirName.trim().isEmpty()) {
            
                                        LoadingDialog loadingDialog = new LoadingDialog((Frame) SwingUtilities.getWindowAncestor(FileExplorerPanel.this));
            
                                        final String finalDirName = dirName;
            
                        
            
                                        SwingWorker<DirTreeResponse, Void> worker = new SwingWorker<>() {
            
                                            @Override
            
                                            protected DirTreeResponse doInBackground() throws Exception {
            
                                                return dirService.createDir(dir.getId(), finalDirName);
            
                                            }
            
                        
            
                                            @Override
            
                                            protected void done() {
            
                                                loadingDialog.dispose();
            
                                                try {
            
                                                    DirTreeResponse newDir = get();
            
                                                    if (newDir != null) {
            
                                                        DefaultMutableTreeNode newNode = new DefaultMutableTreeNode(newDir);
            
                                                        cloudTreeModel.insertNodeInto(newNode, node, node.getChildCount());
            
                                                        cloudFileTree.scrollPathToVisible(new TreePath(newNode.getPath()));
            
                                                    }
            
                                                } catch (Exception ex) {
            
                                                    ex.printStackTrace();
            
                                                }
            
                                            }
            
                                        };
            
                                        worker.execute();
            
                                        loadingDialog.setVisible(true);
            
                                    }
            
                                });
            
                                popupMenu.add(newDirItem);
            
                        
            
                                popupMenu.addSeparator();
            
                        
            
                                JMenuItem renameItem = new JMenuItem("重命名");
            
                                renameItem.addActionListener(e -> {
            
                                    String newName = (String) JOptionPane.showInputDialog(this, "请输入新名称:", "重命名", JOptionPane.PLAIN_MESSAGE, null, null, dir.getName());
            
                                    if (newName != null && !newName.trim().isEmpty()) {
            
                                        LoadingDialog loadingDialog = new LoadingDialog((Frame) SwingUtilities.getWindowAncestor(FileExplorerPanel.this));
            
                                        final String finalNewName = newName;
            
                        
            
                                        SwingWorker<DirTreeResponse, Void> worker = new SwingWorker<>() {
            
                                            @Override
            
                                            protected DirTreeResponse doInBackground() throws Exception {
            
                                                return dirService.updateDir(dir.getId(), dir.getParentId(), finalNewName);
            
                                            }
            
                        
            
                                            @Override
            
                                            protected void done() {
            
                                                loadingDialog.dispose();
            
                                                try {
            
                                                    DirTreeResponse updatedDir = get();
            
                                                    if (updatedDir != null) {
            
                                                        dir.setName(updatedDir.getName()); // Update the name in the existing user object
            
                                                        cloudTreeModel.nodeChanged(node);
            
                                                    }
            
                                                } catch (Exception ex) {
            
                                                    ex.printStackTrace();
            
                                                }
            
                                            }
            
                                        };
            
                                        worker.execute();
            
                                        loadingDialog.setVisible(true);
            
                                    }
            
                                });
            
                                popupMenu.add(renameItem);
            
                        
            
                                JMenuItem deleteItem = new JMenuItem("删除");
            
                                deleteItem.addActionListener(e -> {
            
                                    int result = JOptionPane.showConfirmDialog(this, "确定要删除 '" + dir.getName() + "' 吗?", "确认删除", JOptionPane.YES_NO_OPTION);
            
                                    if (result == JOptionPane.YES_OPTION) {
            
                                        LoadingDialog loadingDialog = new LoadingDialog((Frame) SwingUtilities.getWindowAncestor(FileExplorerPanel.this));
            
                                        SwingWorker<Boolean, Void> worker = new SwingWorker<>() {
            
                                            @Override
            
                                            protected Boolean doInBackground() throws Exception {
            
                                                return dirService.deleteDir(dir.getId());
            
                                            }
            
                        
            
                                            @Override
            
                                            protected void done() {
            
                                                loadingDialog.dispose();
            
                                                try {
            
                                                    if (get()) {
            
                                                        cloudTreeModel.removeNodeFromParent(node);
            
                                                    }
            
                                                } catch (Exception ex) {
            
                                                    ex.printStackTrace();
            
                                                }
            
                                            }
            
                                        };
            
                                        worker.execute();
            
                                        loadingDialog.setVisible(true);
            
                                    }
            
                                });
            
                                popupMenu.add(deleteItem);
            
                        
            
                                return popupMenu;
            
                            }
            }
            