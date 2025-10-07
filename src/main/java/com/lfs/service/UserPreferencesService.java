package com.lfs.service;

import java.awt.Rectangle;
import java.io.File;
import java.util.prefs.Preferences;

/**
 * 管理用户偏好
 */
public class UserPreferencesService {

    private final Preferences prefs;

    // 用于存储首选项
    private static final String LAST_DIRECTORY_KEY = "lastDirectory";
    private static final String WINDOW_X_KEY = "windowX";
    private static final String WINDOW_Y_KEY = "windowY";
    private static final String WINDOW_WIDTH_KEY = "windowWidth";
    private static final String WINDOW_HEIGHT_KEY = "windowHeight";
    private static final String FILE_EXPLORER_LAST_DIRECTORY_KEY = "fileExplorerLastDirectory";

    public UserPreferencesService() {
        // 使用 userNodeForPackage 确保偏好设置按用户存储
        this.prefs = Preferences.userNodeForPackage(UserPreferencesService.class);
    }

    /**
     * 保存上次选择的目录路径。
     * @param directory 保存的目录
     */
    public void saveLastDirectory(File directory) {
        if (directory != null) {
            prefs.put(LAST_DIRECTORY_KEY, directory.getAbsolutePath());
        }
    }

    /**
     * 检索上次使用的目录
     * @return 最后一个目录的 File 对象，如果未设置，则为 null。
     */
    public File getLastDirectory() {
        String lastDirPath = prefs.get(LAST_DIRECTORY_KEY, null);
        if (lastDirPath != null) {
            File lastDir = new File(lastDirPath);
            if (lastDir.exists() && lastDir.isDirectory()) {
                return lastDir;
            }
        }
        return null;
    }

    /**
     * 保存主窗口的位置和大小
     * @param bounds 表示窗口边界的 Rectangle
     */
    public void saveWindowBounds(Rectangle bounds) {
        prefs.putInt(WINDOW_X_KEY, bounds.x);
        prefs.putInt(WINDOW_Y_KEY, bounds.y);
        prefs.putInt(WINDOW_WIDTH_KEY, bounds.width);
        prefs.putInt(WINDOW_HEIGHT_KEY, bounds.height);
    }

    /**
     * 加载窗口上次保存的边界。
     * @param defaultWidth 如果未保存值，则使用默认宽度。
     * @param defaultHeight 如果未保存值，则使用默认高度。
     * @return 一个 Rectangle 对象，包含保存的或默认的边界。
     */
    public Rectangle loadWindowBounds(int defaultWidth, int defaultHeight) {
        int width = prefs.getInt(WINDOW_WIDTH_KEY, defaultWidth);
        int height = prefs.getInt(WINDOW_HEIGHT_KEY, defaultHeight);
        int x = prefs.getInt(WINDOW_X_KEY, -1);
        int y = prefs.getInt(WINDOW_Y_KEY, -1);
        return new Rectangle(x, y, width, height);
    }

    /**
     * 加载窗口上次保存的边界。
     * @return 一个保存了边界的 Rectangle，如果未设置，则返回一个零维度的 Rectangle。
     */
    public Rectangle loadWindowBounds() {
        int width = prefs.getInt(WINDOW_WIDTH_KEY, 0);
        int height = prefs.getInt(WINDOW_HEIGHT_KEY, 0);
        int x = prefs.getInt(WINDOW_X_KEY, -1);
        int y = prefs.getInt(WINDOW_Y_KEY, -1);
        return new Rectangle(x, y, width, height);
    }

    /**
     * 保存文件浏览器上次打开的目录路径。
     * @param directory 要保存的目录
     */
    public void saveFileExplorerLastDirectory(File directory) {
        if (directory != null) {
            prefs.put(FILE_EXPLORER_LAST_DIRECTORY_KEY, directory.getAbsolutePath());
        }
    }

    /**
     * 检索文件浏览器上次使用的目录。
     * @return 最后一个目录的 File 对象，如果未设置，则为 null。
     */
    public File getFileExplorerLastDirectory() {
        String lastDirPath = prefs.get(FILE_EXPLORER_LAST_DIRECTORY_KEY, null);
        if (lastDirPath != null) {
            File lastDir = new File(lastDirPath);
            if (lastDir.exists() && lastDir.isDirectory()) {
                return lastDir;
            }
        }
        return null;
    }
}
