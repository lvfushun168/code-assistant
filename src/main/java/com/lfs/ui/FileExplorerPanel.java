package com.lfs.ui;

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
import java.util.Enumeration;
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

    private JTree cloudFileTree;
    private DefaultTreeModel cloudTreeModel;
    private DefaultMutableTreeNode cloudRootNode;
    private DirService dirService;
    private ContentService contentService;
    private DirTreeResponse cloudApiRoot;


    public FileExplorerPanel(MainFrameController controller) {
        super(new BorderLayout());
        this.controller = controller;
        this.prefsService = new UserPreferencesService();
        this.fileProcessorService = new FileProcessorService();
        this.dirService = new DirService();
        this.contentService = new ContentService();

        rootNode = new DefaultMutableTreeNode();
        treeModel = new DefaultTreeModel(rootNode);
        fileTree = new JTree(treeModel);

        initUI();
    }

    private void initUI() {
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

        tabbedPane = new JTabbedPane();

        JPanel localPanel = new JPanel(new BorderLayout());
        fileTree.setRootVisible(false);
        fileTree.setShowsRootHandles(true);
        fileTree.setCellRenderer(new FileTreeCellRenderer());
        fileTree.setFont(new Font("SansSerif", Font.PLAIN, 14));
        JScrollPane scrollPane = new JScrollPane(fileTree);
        localPanel.add(scrollPane, BorderLayout.CENTER);

        cloudPanel = new JPanel(new BorderLayout());
        cloudRootNode = new DefaultMutableTreeNode("云端文件");
        cloudTreeModel = new DefaultTreeModel(cloudRootNode);
        cloudFileTree = new JTree(cloudTreeModel);
        cloudFileTree.setCellRenderer(new CloudFileTreeCellRenderer());
        cloudFileTree.setRootVisible(false);
        cloudFileTree.setShowsRootHandles(true);
        cloudFileTree.setDragEnabled(true);
        cloudFileTree.setTransferHandler(new CloudTreeTransferHandler(this.controller));
        JScrollPane cloudScrollPane = new JScrollPane(cloudFileTree);
        cloudPanel.add(cloudScrollPane, BorderLayout.CENTER);


        tabbedPane.addTab("本地", localPanel);
        tabbedPane.addTab("云端", cloudPanel);

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
                            ContentResponse fileInfo = (ContentResponse) userObject;

                            setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));

                            new SwingWorker<String, Void>() {
                                @Override
                                protected String doInBackground() {
                                    return contentService.downloadContent(fileInfo.getId());
                                }

                                @Override
                                protected void done() {
                                    try {
                                        String content = get();
                                        if (content != null) {
                                            controller.onCloudFileSelected(fileInfo, content);
                                        }
                                    } catch (Exception ex) {
                                        NotificationUtil.showErrorDialog(FileExplorerPanel.this, "加载云端文件失败: " + ex.getMessage());
                                        ex.printStackTrace();
                                    } finally {
                                        setCursor(Cursor.getDefaultCursor());
                                    }
                                }
                            }.execute();
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
            JPopupMenu popupMenu = createBackgroundPopupMenu();
            popupMenu.show(e.getComponent(), e.getX(), e.getY());
        } else {
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
        navigateTo(currentDir, true);
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
            navigateTo(dir, true);
        }
    }

    private void forward() {
        if (historyIndex < history.size() - 1) {
            historyIndex++;
            File dir = history.get(historyIndex);
            navigateTo(dir, true);
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
                    cloudApiRoot = get();
                    cloudRootNode.removeAllChildren();
                    if (cloudApiRoot != null) {
                        DefaultMutableTreeNode rootTreeNode = new DefaultMutableTreeNode(cloudApiRoot);
                        cloudRootNode.add(rootTreeNode);
                        buildCloudTree(rootTreeNode, cloudApiRoot);
                        cloudTreeModel.reload(cloudRootNode);
                    } else {
                        // 如果获取失败（cloudApiRoot 为 null），禁用云端Tab并切回本地
                        setCloudTabEnabled(false);
                        switchToLocalTab();
                    }
                } catch (Exception e) {
                    NotificationUtil.showErrorDialog(FileExplorerPanel.this, "加载云端目录失败: " + e.getMessage());
                    e.printStackTrace();
                    // 发生异常时也禁用Tab并切回
                    setCloudTabEnabled(false);
                    switchToLocalTab();
                } finally {
                    setCursor(Cursor.getDefaultCursor());
                }
            }
        }.execute();
    }

    private void buildCloudTree(DefaultMutableTreeNode parent, DirTreeResponse dir) {
        if (dir.getContents() != null && !dir.getContents().isEmpty()) {
            for (ContentResponse content : dir.getContents()) {
                parent.add(new DefaultMutableTreeNode(content));
            }
        }

        if (dir.getChildren() != null && !dir.getChildren().isEmpty()) {
            for (DirTreeResponse child : dir.getChildren()) {
                DefaultMutableTreeNode childNode = new DefaultMutableTreeNode(child);
                parent.add(childNode);
                buildCloudTree(childNode, child);
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
            JPopupMenu popupMenu = createCloudBackgroundPopupMenu();
            popupMenu.show(e.getComponent(), e.getX(), e.getY());
        } else {
            cloudFileTree.setSelectionPath(path);
            DefaultMutableTreeNode node = (DefaultMutableTreeNode) path.getLastPathComponent();
            Object userObject = node.getUserObject();

            if (userObject instanceof DirTreeResponse) {
                JPopupMenu popupMenu = createCloudDirPopupMenu(node);
                popupMenu.show(e.getComponent(), e.getX(), e.getY());
            } else if (userObject instanceof ContentResponse) {
                JPopupMenu popupMenu = createCloudFilePopupMenu(node);
                popupMenu.show(e.getComponent(), e.getX(), e.getY());
            }
        }
    }

    private JPopupMenu createCloudFilePopupMenu(DefaultMutableTreeNode node) {
        JPopupMenu popupMenu = new JPopupMenu();
        ContentResponse content = (ContentResponse) node.getUserObject();

        JMenuItem renameItem = new JMenuItem("重命名");
        renameItem.addActionListener(e -> {
            String newName = (String) JOptionPane.showInputDialog(
                    this,
                    "请输入新名称:",
                    "重命名",
                    JOptionPane.PLAIN_MESSAGE,
                    null,
                    null,
                    content.getTitle()
            );

            if (newName != null && !newName.trim().isEmpty() && !newName.trim().equals(content.getTitle())) {
                controller.renameCloudFile(content.getId(), content.getDirId(), newName.trim());
            }
        });
        popupMenu.add(renameItem);
        popupMenu.addSeparator();
        JMenuItem deleteItem = new JMenuItem("删除");
        deleteItem.addActionListener(e -> {
            int result = JOptionPane.showConfirmDialog(
                    this,
                    "确定要删除 '" + content.getTitle() + "' 吗?",
                    "确认删除",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.WARNING_MESSAGE
            );
            if (result == JOptionPane.YES_OPTION) {
                controller.deleteCloudFile(content.getId());
            }
        });
        popupMenu.add(deleteItem);
        return popupMenu;
    }

    private JPopupMenu createCloudBackgroundPopupMenu() {
        JPopupMenu popupMenu = new JPopupMenu();
        JMenuItem newFileItem = new JMenuItem("新建文档");
        newFileItem.addActionListener(e -> handleNewCloudFile(cloudApiRoot != null ? cloudApiRoot.getId() : null));
        popupMenu.add(newFileItem);
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
        JMenuItem newFileItem = new JMenuItem("新建文档");
        newFileItem.addActionListener(e -> handleNewCloudFile(dir.getId()));
        popupMenu.add(newFileItem);
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
                controller.renameCloudDir(dir.getId(), dir.getParentId(), newName.trim());
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

    private void handleNewCloudFile(Long dirId) {
        if (dirId == null) {
            NotificationUtil.showErrorDialog(this, "无法确定父目录，无法创建文件。");
            return;
        }
        String title = JOptionPane.showInputDialog(this, "请输入新文档名称", "新建云端文档", JOptionPane.PLAIN_MESSAGE);
        if (title != null && !title.trim().isEmpty()) {
            controller.createAndOpenCloudFile(dirId, title.trim());
        }
    }

    public void addCloudContentNode(ContentResponse newContent) {
        if (newContent == null || newContent.getDirId() == null) {
            return;
        }

        DefaultMutableTreeNode parentNode = findNodeById(newContent.getDirId());

        if (parentNode != null) {
            DefaultMutableTreeNode newNode = new DefaultMutableTreeNode(newContent);
            cloudTreeModel.insertNodeInto(newNode, parentNode, parentNode.getChildCount());
            cloudFileTree.scrollPathToVisible(new TreePath(newNode.getPath()));
        } else {
            loadCloudDirectory();
        }
    }

    private boolean isIdMatch(Long id1, Object id2Obj) {
        if (id1 == null || id2Obj == null) return false;
        String s1 = String.valueOf(id1);
        String s2 = String.valueOf(id2Obj);
        if (s1.endsWith(".0")) s1 = s1.substring(0, s1.length() - 2);
        if (s2.endsWith(".0")) s2 = s2.substring(0, s2.length() - 2);
        return s1.equals(s2);
    }

    private DefaultMutableTreeNode findNodeById(Long id) {
        if (id == null) return null;

        Enumeration e = cloudRootNode.breadthFirstEnumeration();
        while (e.hasMoreElements()) {
            DefaultMutableTreeNode node = (DefaultMutableTreeNode) e.nextElement();
            Object uo = node.getUserObject();
            if (uo instanceof DirTreeResponse) {
                if (isIdMatch(id, ((DirTreeResponse) uo).getId())) {
                    return node;
                }
            } else if (uo instanceof ContentResponse) {
                if (isIdMatch(id, ((ContentResponse) uo).getId())) {
                    return node;
                }
            }
        }
        return null;
    }

    public void updateCloudContentNode(ContentResponse updatedContent) {
        if (updatedContent == null || updatedContent.getId() == null) {
            return;
        }
        DefaultMutableTreeNode nodeToUpdate = findNodeById(updatedContent.getId());

        if (nodeToUpdate != null) {
            Object obj = nodeToUpdate.getUserObject();
            if (obj instanceof ContentResponse) {
                ContentResponse oldContent = (ContentResponse) obj;
                oldContent.setTitle(updatedContent.getTitle());
                oldContent.setType(updatedContent.getType());
                cloudTreeModel.nodeChanged(nodeToUpdate);
            }
        } else {
            loadCloudDirectory();
        }
    }

    public void updateCloudDirNode(DirTreeResponse updatedDir) {
        if (updatedDir == null || updatedDir.getId() == null) {
            return;
        }
        DefaultMutableTreeNode nodeToUpdate = findNodeById(updatedDir.getId());

        if (nodeToUpdate != null) {
            Object obj = nodeToUpdate.getUserObject();
            if (obj instanceof DirTreeResponse) {
                DirTreeResponse oldDir = (DirTreeResponse) obj;
                oldDir.setName(updatedDir.getName());
                cloudTreeModel.nodeChanged(nodeToUpdate);
            }
        } else {
            loadCloudDirectory();
        }
    }

    public void removeCloudContentNode(Long contentId) {
        if (contentId == null) {
            return;
        }
        DefaultMutableTreeNode nodeToRemove = findNodeById(contentId);

        if (nodeToRemove != null) {
            cloudTreeModel.removeNodeFromParent(nodeToRemove);
        } else {
            loadCloudDirectory();
        }
    }

    public void moveCloudNodeLocal(DefaultMutableTreeNode nodeToMoveStub, DefaultMutableTreeNode newParentStub, Object updatedUserObject) {
        SwingUtilities.invokeLater(() -> {
            try {
                if (cloudRootNode.getChildCount() == 0) return;

                Long moveId = null;
                Object stubObj = nodeToMoveStub.getUserObject();
                if (stubObj instanceof ContentResponse) moveId = ((ContentResponse)stubObj).getId();
                else if (stubObj instanceof DirTreeResponse) moveId = ((DirTreeResponse)stubObj).getId();

                DefaultMutableTreeNode liveOldNode = findNodeById(moveId);

                DefaultMutableTreeNode liveNewParent = null;
                Object parentObj = newParentStub.getUserObject();

                if (newParentStub == cloudRootNode ||
                        (cloudRootNode.getFirstChild() != null && newParentStub == cloudRootNode.getFirstChild())) {
                    liveNewParent = (DefaultMutableTreeNode) cloudRootNode.getFirstChild();
                } else if (parentObj instanceof DirTreeResponse) {
                    Long parentId = ((DirTreeResponse) parentObj).getId();
                    liveNewParent = findNodeById(parentId);
                }

                if (liveOldNode == null) {
                    NotificationUtil.showErrorDialog(this, "局部更新失败：无法在树中找到被移动的节点 (ID=" + moveId + ")");
                    loadCloudDirectory();
                    return;
                }
                if (liveNewParent == null) {
                    NotificationUtil.showErrorDialog(this, "局部更新失败：无法在树中找到目标目录");
                    loadCloudDirectory();
                    return;
                }

                if (liveOldNode == liveNewParent || liveOldNode.isNodeDescendant(liveNewParent)) {
                    return;
                }

                cloudTreeModel.removeNodeFromParent(liveOldNode);

                liveOldNode.setUserObject(updatedUserObject);

                cloudTreeModel.insertNodeInto(liveOldNode, liveNewParent, liveNewParent.getChildCount());

                cloudTreeModel.reload();

                TreePath newPath = new TreePath(cloudTreeModel.getPathToRoot(liveOldNode));
                cloudFileTree.scrollPathToVisible(newPath);
                cloudFileTree.setSelectionPath(newPath);

            } catch (Exception e) {
                e.printStackTrace();
                NotificationUtil.showErrorDialog(this, "局部更新发生异常: " + e.getMessage());
                loadCloudDirectory();
            }
        });
    }
}