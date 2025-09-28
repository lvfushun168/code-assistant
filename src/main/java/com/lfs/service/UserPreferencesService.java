package com.lfs.service;

import java.awt.Rectangle;
import java.io.File;
import java.util.prefs.Preferences;

/**
 * 管理用户偏好
 */
public class UserPreferencesService {

    private final Preferences prefs;

    // Keys for storing preferences.
    private static final String LAST_DIRECTORY_KEY = "lastDirectory";
    private static final String WINDOW_X_KEY = "windowX";
    private static final String WINDOW_Y_KEY = "windowY";
    private static final String WINDOW_WIDTH_KEY = "windowWidth";
    private static final String WINDOW_HEIGHT_KEY = "windowHeight";

    public UserPreferencesService() {
        // Using the userNodeForPackage ensures preferences are stored on a per-user basis for this app.
        this.prefs = Preferences.userNodeForPackage(UserPreferencesService.class);
    }

    /**
     * Saves the last selected directory path.
     * @param directory The directory to save.
     */
    public void saveLastDirectory(File directory) {
        if (directory != null) {
            prefs.put(LAST_DIRECTORY_KEY, directory.getAbsolutePath());
        }
    }

    /**
     * Retrieves the last used directory.
     * @return A File object for the last directory, or null if not set.
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
     * Saves the main window's position and size.
     * @param bounds The Rectangle representing the window's bounds.
     */
    public void saveWindowBounds(Rectangle bounds) {
        prefs.putInt(WINDOW_X_KEY, bounds.x);
        prefs.putInt(WINDOW_Y_KEY, bounds.y);
        prefs.putInt(WINDOW_WIDTH_KEY, bounds.width);
        prefs.putInt(WINDOW_HEIGHT_KEY, bounds.height);
    }

    /**
     * Loads the window's last saved bounds.
     * @param defaultWidth The default width to use if no value is saved.
     * @param defaultHeight The default height to use if no value is saved.
     * @return A Rectangle with the saved or default bounds.
     */
    public Rectangle loadWindowBounds(int defaultWidth, int defaultHeight) {
        int width = prefs.getInt(WINDOW_WIDTH_KEY, defaultWidth);
        int height = prefs.getInt(WINDOW_HEIGHT_KEY, defaultHeight);
        int x = prefs.getInt(WINDOW_X_KEY, -1);
        int y = prefs.getInt(WINDOW_Y_KEY, -1);
        return new Rectangle(x, y, width, height);
    }
}
