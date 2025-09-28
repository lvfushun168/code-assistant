package com.lfs.service;

import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;

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
}
