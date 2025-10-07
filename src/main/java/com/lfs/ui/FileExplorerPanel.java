package com.lfs.ui;

import com.lfs.config.AppConfig;

import javax.swing.*;
import javax.swing.event.TreeExpansionEvent;
import javax.swing.event.TreeWillExpandListener;
import javax.swing.filechooser.FileSystemView;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.ExpandVetoException;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;

public class FileExplorerPanel extends JPanel {

    private MainFrameController controller;

    public FileExplorerPanel(MainFrameController controller) {
        super(new BorderLayout());
        this.controller = controller;
        initUI();
    }

    private void initUI() {
        DefaultMutableTreeNode rootNode;
        String os = System.getProperty("os.name").toLowerCase();

        if (os.contains("win")) {
            // Windows: 创建“我的电脑”作为根节点
            rootNode = new DefaultMutableTreeNode("我的电脑");
            File[] roots = File.listRoots();
            for (File root : roots) {
                DefaultMutableTreeNode driveNode = new DefaultMutableTreeNode(root);
                driveNode.add(new DefaultMutableTreeNode(null)); // 占位符
                rootNode.add(driveNode);
            }
        } else {
            // macOS/Linux: 使用用户主目录作为根节点
            String home = System.getProperty("user.home");
            File rootDir = new File(home);
            rootNode = new DefaultMutableTreeNode(rootDir);
            createNodes(rootNode, rootDir); // 初始加载
        }

        DefaultTreeModel treeModel = new DefaultTreeModel(rootNode);
        JTree fileTree = new JTree(treeModel);
        fileTree.setFont(new Font("SansSerif", Font.PLAIN, 14));
        fileTree.setCellRenderer(new FileTreeCellRenderer());

        fileTree.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    TreePath path = fileTree.getPathForLocation(e.getX(), e.getY());
                    if (path == null) {
                        return;
                    }
                    DefaultMutableTreeNode node = (DefaultMutableTreeNode) path.getLastPathComponent();
                    if (node != null && node.getUserObject() instanceof File) {
                        File file = (File) node.getUserObject();
                        if (file.isFile()) {
                            String fileName = file.getName();
                            int lastDotIndex = fileName.lastIndexOf('.');
                            if (lastDotIndex > 0) {
                                String extension = fileName.substring(lastDotIndex + 1).toLowerCase();
                                if (AppConfig.ALLOWED_EXTENSIONS.contains(extension)) {
                                    controller.onFileSelected(file);
                                }
                            }
                        }
                    }
                }
            }
        });

        fileTree.addTreeWillExpandListener(new TreeWillExpandListener() {
            @Override
            public void treeWillExpand(TreeExpansionEvent event) throws ExpandVetoException {
                DefaultMutableTreeNode node = (DefaultMutableTreeNode) event.getPath().getLastPathComponent();
                if (node.getUserObject() instanceof File) {
                    // 检查是否是我们的占位符节点
                    if (node.getChildCount() == 1 && node.getFirstChild() instanceof DefaultMutableTreeNode) {
                        DefaultMutableTreeNode child = (DefaultMutableTreeNode) node.getFirstChild();
                        if (child.getUserObject() == null) { // 这是一个占位符
                            node.removeAllChildren(); // 移除占位符
                            File file = (File) node.getUserObject();
                            createNodes(node, file); // 动态加载子节点
                            treeModel.nodeStructureChanged(node);
                        }
                    }
                }
            }

            @Override
            public void treeWillCollapse(TreeExpansionEvent event) throws ExpandVetoException {
                // 不需要处理折叠事件
            }
        });

        JScrollPane scrollPane = new JScrollPane(fileTree);
        add(scrollPane, BorderLayout.CENTER);
    }

    private void createNodes(DefaultMutableTreeNode node, File dir) {
        if (!dir.isDirectory()) {
            return;
        }

        File[] files = dir.listFiles();
        if (files == null) {
            return;
        }

        for (File file : files) {
            DefaultMutableTreeNode childNode = new DefaultMutableTreeNode(file);
            node.add(childNode);
            if (file.isDirectory()) {
                childNode.add(new DefaultMutableTreeNode(null));
            }
        }
    }

    private static class FileTreeCellRenderer extends javax.swing.tree.DefaultTreeCellRenderer {
        private FileSystemView fileSystemView = FileSystemView.getFileSystemView();

        @Override
        public Component getTreeCellRendererComponent(JTree tree, Object value, boolean sel, boolean expanded, boolean leaf, int row, boolean hasFocus) {
            super.getTreeCellRendererComponent(tree, value, sel, expanded, leaf, row, hasFocus);
            if (value instanceof DefaultMutableTreeNode) {
                Object userObject = ((DefaultMutableTreeNode) value).getUserObject();
                if (userObject instanceof File) {
                    File file = (File) userObject;
                    setText(fileSystemView.getSystemDisplayName(file));
                    setIcon(fileSystemView.getSystemIcon(file));
                } else if (userObject != null) {
                    setText(userObject.toString());
                }
            }
            return this;
        }
    }
}