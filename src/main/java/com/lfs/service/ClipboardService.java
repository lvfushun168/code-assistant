package com.lfs.service;

import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.IOException;

/**
 * 剪贴板处理
 */
public class ClipboardService {

    private ClipboardService() {
    }

    /**
     * 将给定的文本内容复制到系统剪贴板
     * @param content 要复制的文本
     */
    public static void copyToClipboard(String content) {
        StringSelection stringSelection = new StringSelection(content);
        Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
        clipboard.setContents(stringSelection, null);
    }

    /**
     * 从系统剪贴板获取文本内容
     * @return 剪贴板中的文本内容，如果剪贴板中没有文本内容，则返回null
     */
    public static String getClipboardContents() {
        Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
        Transferable contents = clipboard.getContents(null);
        if (contents != null && contents.isDataFlavorSupported(DataFlavor.stringFlavor)) {
            try {
                return (String) contents.getTransferData(DataFlavor.stringFlavor);
            } catch (UnsupportedFlavorException | IOException ex) {
                ex.printStackTrace();
            }
        }
        return null;
    }
}
