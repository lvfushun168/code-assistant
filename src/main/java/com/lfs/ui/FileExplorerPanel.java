package com.lfs.ui;

import javax.swing.*;
import javax.swing.event.TreeExpansionEvent;
import javax.swing.event.TreeWillExpandListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.ExpandVetoException;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.io.File;
import java.util.Objects;

public class FileExplorerPanel extends JPanel {

    public FileExplorerPanel(String rootPath) {
        super(new BorderLayout());
        initUI(rootPath);
    }

    private void initUI(String rootPath) {
        File rootDir = new File(rootPath);
        // 使用带有完整路径的File对象作为根节点的用户对象，而不仅仅是名称
        DefaultMutableTreeNode rootNode = new DefaultMutableTreeNode(rootDir);

        DefaultTreeModel treeModel = new DefaultTreeModel(rootNode);
        JTree fileTree = new JTree(treeModel);
        fileTree.setFont(new Font("SansSerif", Font.PLAIN, 14));

        // 设置自定义的渲染器来只显示文件名
        fileTree.setCellRenderer(new FileTreeCellRenderer());

        // 初始加载顶层目录
        createNodes(rootNode, rootDir);

        // 添加展开/折叠监听器
        fileTree.addTreeWillExpandListener(new TreeWillExpandListener() {
            @Override
            public void treeWillExpand(TreeExpansionEvent event) {
                TreePath path = event.getPath();
                DefaultMutableTreeNode node = (DefaultMutableTreeNode) path.getLastPathComponent();
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

            @Override
            public void treeWillCollapse(TreeExpansionEvent event) {
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
            // 使用带有完整路径的File对象作为用户对象
            DefaultMutableTreeNode childNode = new DefaultMutableTreeNode(file);
            node.add(childNode);
            if (file.isDirectory()) {
                // 为子目录添加一个空的占位符节点，使其可以展开
                childNode.add(new DefaultMutableTreeNode(null));
            }
        }
    }

    // 自定义单元格渲染器，以仅显示文件名
    private static class FileTreeCellRenderer extends javax.swing.tree.DefaultTreeCellRenderer {
        @Override
        public Component getTreeCellRendererComponent(JTree tree, Object value, boolean sel, boolean expanded, boolean leaf, int row, boolean hasFocus) {
            super.getTreeCellRendererComponent(tree, value, sel, expanded, leaf, row, hasFocus);
            if (value instanceof DefaultMutableTreeNode) {
                Object userObject = ((DefaultMutableTreeNode) value).getUserObject();
                if (userObject instanceof File) {
                    setText(((File) userObject).getName());
                } else if (userObject != null) {
                    setText(userObject.toString());
                } else {
                    // 这是占位符节点，不显示文本
                    setText("");
                }
            }
            return this;
        }
    }
}