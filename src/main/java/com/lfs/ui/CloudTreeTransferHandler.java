package com.lfs.ui;

import com.lfs.domain.ContentResponse;
import com.lfs.domain.DirTreeResponse;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreePath;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.Serializable;

/**
 * 处理云文件树的拖放操作
 */
public class CloudTreeTransferHandler extends TransferHandler {

    private final MainFrameController controller;
    // 使用一个可序列化的容器来传递数据，避免transient字段丢失
    private static final DataFlavor FLAVOR = new DataFlavor(TransferData.class, "Cloud Tree Transfer Data");

    /**
     * 可序列化的数据传输容器
     */
    public static class TransferData implements Serializable {
        public final Object userObject;
        public final TreePath treePath;

        public TransferData(Object userObject, TreePath treePath) {
            this.userObject = userObject;
            this.treePath = treePath;
        }
    }

    /**
     * Transferable 实现，用于包装 TransferData
     */
    public static class DataTransferable implements Transferable {
        private final TransferData data;

        public DataTransferable(TransferData data) {
            this.data = data;
        }

        @Override
        public DataFlavor[] getTransferDataFlavors() {
            return new DataFlavor[]{FLAVOR};
        }

        @Override
        public boolean isDataFlavorSupported(DataFlavor flavor) {
            return FLAVOR.equals(flavor);
        }

        @Override
        public Object getTransferData(DataFlavor flavor) throws UnsupportedFlavorException {
            if (!isDataFlavorSupported(flavor)) {
                throw new UnsupportedFlavorException(flavor);
            }
            return data;
        }
    }


    public CloudTreeTransferHandler(MainFrameController controller) {
        this.controller = controller;
    }

    @Override
    public int getSourceActions(JComponent c) {
        return MOVE;
    }

    @Override
    protected Transferable createTransferable(JComponent c) {
        JTree tree = (JTree) c;
        TreePath path = tree.getSelectionPath();
        if (path != null) {
            DefaultMutableTreeNode node = (DefaultMutableTreeNode) path.getLastPathComponent();
            Object userObject = node.getUserObject();
            if (userObject != null) {
                // 在数据丢失前捕获 userObject 和 path
                TransferData data = new TransferData(userObject, path);
                return new DataTransferable(data);
            }
        }
        return null;
    }

    @Override
    public boolean canImport(TransferSupport support) {
        if (!support.isDrop()) {
            return false;
        }
        support.setShowDropLocation(true);
        if (!support.isDataFlavorSupported(FLAVOR)) {
            return false;
        }

        JTree.DropLocation dl = (JTree.DropLocation) support.getDropLocation();
        TreePath destPath = dl.getPath();
        DefaultMutableTreeNode targetNode = (DefaultMutableTreeNode) destPath.getLastPathComponent();

        if (!(targetNode.getUserObject() instanceof DirTreeResponse)) {
            return false;
        }

        try {
            Transferable t = support.getTransferable();
            TransferData data = (TransferData) t.getTransferData(FLAVOR);
            DefaultMutableTreeNode draggedNode = (DefaultMutableTreeNode) data.treePath.getLastPathComponent();

            if (draggedNode.equals(targetNode)) return false;
            if (targetNode.isNodeDescendant(draggedNode)) return false;
            if (draggedNode.getParent() != null && draggedNode.getParent().equals(targetNode)) return false;

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }

        return true;
    }

    @Override
    public boolean importData(TransferSupport support) {
        if (!canImport(support)) {
            return false;
        }

        TransferData transferData;
        try {
            transferData = (TransferData) support.getTransferable().getTransferData(FLAVOR);
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }

        // 现在可以安全地使用 transferData 中的所有信息
        Object draggedObject = transferData.userObject;
        DefaultMutableTreeNode draggedNode = (DefaultMutableTreeNode) transferData.treePath.getLastPathComponent();

        JTree.DropLocation dl = (JTree.DropLocation) support.getDropLocation();
        DefaultMutableTreeNode targetNode = (DefaultMutableTreeNode) dl.getPath().getLastPathComponent();
        DirTreeResponse targetDir = (DirTreeResponse) targetNode.getUserObject();

        String itemName = "";
        if (draggedObject instanceof DirTreeResponse) {
            itemName = ((DirTreeResponse) draggedObject).getName();
        } else if (draggedObject instanceof ContentResponse) {
            itemName = ((ContentResponse) draggedObject).getTitle();
        }

        int response = JOptionPane.showConfirmDialog(
                (JComponent) support.getComponent(),
                "要将 '" + itemName + "' 移动到 '" + targetDir.getName() + "' 吗?",
                "确认移动",
                JOptionPane.YES_NO_OPTION);

        if (response == JOptionPane.YES_OPTION) {
            controller.moveCloudNode(draggedNode, targetNode);
        }

        return false;
    }
}
