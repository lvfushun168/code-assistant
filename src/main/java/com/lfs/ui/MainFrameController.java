package com.lfs.ui;

import com.lfs.domain.ContentResponse;
import com.lfs.service.ClipboardService;
import com.lfs.service.ContentService;
import com.lfs.service.FileProcessorService;
import com.lfs.service.UserPreferencesService;
import com.lfs.util.NotificationUtil;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.io.IOException;

import static com.lfs.config.AppConfig.ALLOWED_EXTENSIONS;

public class MainFrameController {

    private final FileProcessorService fileProcessorService;
    private final UserPreferencesService preferencesService;
    private final ContentService contentService;
    private final MainFrame mainFrame;

    public MainFrameController(MainFrame mainFrame) {
        this.mainFrame = mainFrame;
        this.fileProcessorService = new FileProcessorService();
        this.preferencesService = new UserPreferencesService();
        this.contentService = new ContentService();
    }

    /**
     * 处理“获取内容”和“获取结构”按钮点击的逻辑。
     *
     * @param isContentMode 获取内容时为true，获取结构时为false。
     */
    public void onProcessDirectory(boolean isContentMode) {
        String dialogTitle = isContentMode ? "请选择一个项目文件夹以读取内容" : "请选择一个项目文件夹以获取结构";
        JFileChooser fileChooser = createConfiguredFileChooser(dialogTitle);
        fileChooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("Directory", "dir"));
        int result = fileChooser.showOpenDialog(mainFrame);

        if (result == JFileChooser.APPROVE_OPTION) {
            File selectedDirectory = fileChooser.getSelectedFile();
            preferencesService.saveLastDirectory(selectedDirectory);

            // 显示加载指示器（可选，但有利于用户体验）
            mainFrame.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));

            // 在后台线程中执行处理以保持UI的响应性。
            new Thread(() -> {
                try {
                    final String processedResult;
                    if (isContentMode) {
                        processedResult = fileProcessorService.generateContentFromDirectory(selectedDirectory);
                    } else {
                        processedResult = fileProcessorService.generateStructureFromDirectory(selectedDirectory);
                    }

                    // 返回UI线程进行用户交互
                    SwingUtilities.invokeLater(() -> {
                        int choice = JOptionPane.showOptionDialog(mainFrame, "是否生成文件？", "确认",
                                JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE, null, new String[]{" 是 ", " 否 "}, "是");

                        if (choice == JOptionPane.YES_OPTION) {
                            // 用户选择保存文件
                            JFileChooser fileChooserSave = new JFileChooser();
                            fileChooserSave.setDialogTitle("选择保存目录");
                            fileChooserSave.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
                            fileChooserSave.setCurrentDirectory(preferencesService.getLastDirectory()); // 使用上次的目录

                            int saveResult = fileChooserSave.showSaveDialog(mainFrame);
                            if (saveResult == JFileChooser.APPROVE_OPTION) {
                                File saveDir = fileChooserSave.getSelectedFile();
                                String fileName = isContentMode ? "code.txt" : "structure.txt";
                                File outputFile = new File(saveDir, fileName);

                                // 在工作线程中执行文件写入
                                new Thread(() -> {
                                    try {
                                        fileProcessorService.saveFile(outputFile, processedResult);
                                        SwingUtilities.invokeLater(() -> NotificationUtil.showSuccessDialog(mainFrame, "文件已保存到: " + outputFile.getAbsolutePath()));
                                    } catch (Exception ex) {
                                        SwingUtilities.invokeLater(() -> NotificationUtil.showErrorDialog(mainFrame, "保存文件时出错: " + ex.getMessage()));
                                        ex.printStackTrace();
                                    } finally {
                                        mainFrame.setCursor(Cursor.getDefaultCursor());
                                    }
                                }).start();
                            } else {
                                // 用户取消了保存
                                mainFrame.setCursor(Cursor.getDefaultCursor());
                            }
                        } else {
                            // 用户选择不保存文件，则复制到剪贴板
                            ClipboardService.copyToClipboard(processedResult);
                            String successMessage = isContentMode ? "内容已粘贴到剪切板" : "项目结构已粘贴到剪切板";
                            NotificationUtil.showSuccessDialog(mainFrame, successMessage);
                            mainFrame.setCursor(Cursor.getDefaultCursor());
                        }
                    });

                } catch (Exception ex) {
                    SwingUtilities.invokeLater(() -> {
                        NotificationUtil.showErrorDialog(mainFrame, "处理时发生错误: " + ex.getMessage());
                        mainFrame.setCursor(Cursor.getDefaultCursor());
                    });
                    ex.printStackTrace();
                }
                // finally 块被移到各个分支内部，以确保光标在正确的时间恢复
            }).start();
        }
    }

    public void onSaveAs() {
        EditorPanel activeEditorPanel = (EditorPanel) mainFrame.getActiveEditorPanel();
        if (activeEditorPanel == null) {
            NotificationUtil.showErrorDialog(mainFrame, "没有活动的编辑器,无法保存!");
            return;
        }
        String text = activeEditorPanel.getTextAreaContent();
        if (text == null || text.trim().isEmpty()) {
            NotificationUtil.showErrorDialog(mainFrame, "内容为空,无法保存!");
            return;
        }
        String[] options = {"云端", "本地"};
        int choice = JOptionPane.showOptionDialog(mainFrame, "请选择保存方式", "保存",
                JOptionPane.YES_NO_OPTION, JOptionPane.PLAIN_MESSAGE, null, options, options[1]);

        if (choice == 1) { // 本地
            JFileChooser fileChooser = new JFileChooser();
            fileChooser.setDialogTitle("选择保存目录");
            fileChooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("Text Files (*.txt)", "txt"));
            fileChooser.addChoosableFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("Markdown Files (*.md)", "md"));
            fileChooser.addChoosableFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("YAML Files (*.yml)", "yml"));
            fileChooser.addChoosableFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("XML Files (*.xml)", "xml"));

            int userSelection = fileChooser.showSaveDialog(mainFrame);

            if (userSelection == JFileChooser.APPROVE_OPTION) {
                File fileToSave = fileChooser.getSelectedFile();
                String filePath = fileToSave.getAbsolutePath();
                if (!filePath.endsWith("." + ((javax.swing.filechooser.FileNameExtensionFilter) fileChooser.getFileFilter()).getExtensions()[0])) {
                    filePath += "." + ((javax.swing.filechooser.FileNameExtensionFilter) fileChooser.getFileFilter()).getExtensions()[0];
                    fileToSave = new File(filePath);
                }

                try {
                    fileProcessorService.saveFile(fileToSave, text);
                    NotificationUtil.showSuccessDialog(mainFrame, "文件已保存到: " + fileToSave.getAbsolutePath());
                } catch (Exception ex) {
                    NotificationUtil.showErrorDialog(mainFrame, "保存文件时出错: " + ex.getMessage());
                    ex.printStackTrace();
                }
            }
        }
    }

    /**
     * 创建并配置一个JFileChooser实例。
     *
     * @param dialogTitle 对话框标题
     * @return 配置好的 JFileChooser 实例
     */
    private JFileChooser createConfiguredFileChooser(String dialogTitle) {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        fileChooser.setDialogTitle(dialogTitle);
        fileChooser.setFileHidingEnabled(false); // 显示隐藏文件

        File lastDir = preferencesService.getLastDirectory();
        if (lastDir != null) {
            fileChooser.setCurrentDirectory(lastDir);
        }
        return fileChooser;
    }

    public void onFileSelected(File file) {
        String name = file.getName();
        String[] split = name.split("\\.");
        if (split.length > 1) {
            String extension = split[split.length - 1];
            if (!ALLOWED_EXTENSIONS.contains(extension)) {
                NotificationUtil.showErrorDialog(mainFrame, "不支持的文件格式: " + extension);
                return;
            }
        }
        final long LARGE_FILE_THRESHOLD = 10 * 1024 * 1024; // 10MB

        if (file.length() > LARGE_FILE_THRESHOLD) {
            mainFrame.openBigFileInTab(file);
        } else {
            mainFrame.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
            SwingWorker<String, Void> worker = new SwingWorker<String, Void>() {
                @Override
                protected String doInBackground() throws Exception {
                    return fileProcessorService.readFileContent(file);
                }

                @Override
                protected void done() {
                    try {
                        String content = get();
                        mainFrame.openFileInTab(file, content);
                    } catch (Exception e) {
                        NotificationUtil.showErrorDialog(mainFrame, "无法读取此文件");
                        e.printStackTrace();
                    } finally {
                        mainFrame.setCursor(Cursor.getDefaultCursor());
                    }
                }
            };
            worker.execute();
        }
    }

    public void onFileSelectedReadOnly(File file) {
        mainFrame.openFileInTabReadOnly(file);
    }

    public void onCloudFileSelected(ContentResponse fileInfo, String content) {
        mainFrame.openCloudFileInTab(fileInfo, content);
    }

    public void createAndOpenCloudFile(Long dirId, String title) {
        mainFrame.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        new SwingWorker<ContentResponse, Void>() {
            @Override
            protected ContentResponse doInBackground() throws Exception {
                // 立即创建一个内容为空的文档
                return contentService.createContent(dirId, title, "");
            }

            @Override
            protected void done() {
                try {
                    ContentResponse response = get();
                    if (response != null) {
                        // 1. 刷新目录树
                        mainFrame.getFileExplorerPanel().addCloudContentNode(response);
                        // 2. 打开新创建的空文件
                        mainFrame.openCloudFileInTab(response, "");
                        NotificationUtil.showToast(mainFrame, "创建成功！");
                    }
                } catch (Exception e) {
                    NotificationUtil.showErrorDialog(mainFrame, "创建失败: " + e.getMessage());
                    e.printStackTrace();
                } finally {
                    mainFrame.setCursor(Cursor.getDefaultCursor());
                }
            }
        }.execute();
    }

    public void renameCloudFile(Long contentId, Long dirId, String newTitle) {
        mainFrame.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        new SwingWorker<ContentResponse, Void>() {
            @Override
            protected ContentResponse doInBackground() throws Exception {
                // 内容传null，因为只重命名，不更新内容
                return contentService.updateContent(contentId, dirId, newTitle, null);
            }

            @Override
            protected void done() {
                try {
                    ContentResponse response = get();
                    if (response != null) {
                        NotificationUtil.showSuccessDialog(mainFrame, "重命名成功！");
                        // 局部刷新节点
                        mainFrame.getFileExplorerPanel().updateCloudContentNode(response);
                    }
                } catch (Exception e) {
                    NotificationUtil.showErrorDialog(mainFrame, "重命名失败: " + e.getMessage());
                    e.printStackTrace();
                } finally {
                    mainFrame.setCursor(Cursor.getDefaultCursor());
                }
            }
        }.execute();
    }

    public void deleteCloudFile(Long contentId) {
        mainFrame.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        new SwingWorker<Boolean, Void>() {
            @Override
            protected Boolean doInBackground() throws Exception {
                return contentService.deleteContent(contentId);
            }

            @Override
            protected void done() {
                try {
                    if (get()) {
                        NotificationUtil.showSuccessDialog(mainFrame, "删除成功！");
                        mainFrame.getFileExplorerPanel().removeCloudContentNode(contentId);
                    }
                } catch (Exception e) {
                    NotificationUtil.showErrorDialog(mainFrame, "删除失败: " + e.getMessage());
                    e.printStackTrace();
                } finally {
                    mainFrame.setCursor(Cursor.getDefaultCursor());
                }
            }
        }.execute();
    }

    public void saveCloudFile() {
        Component activeComponent = mainFrame.getActiveEditorPanel();
        if (!(activeComponent instanceof EditorPanel)) {
            return; // Not a standard editor panel that supports this
        }
        EditorPanel activeEditorPanel = (EditorPanel) activeComponent;

        // 由于新建流程已改变，这里只处理更新逻辑
        Long contentId = activeEditorPanel.getCloudContentId();
        Long dirId = activeEditorPanel.getCloudDirId();
        String title = activeEditorPanel.getCloudTitle();
        String content = activeEditorPanel.getTextAreaContent();

        if (contentId == null) {
            NotificationUtil.showErrorDialog(mainFrame, "无法保存文件，文档ID丢失。");
            return;
        }

        mainFrame.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));

        new SwingWorker<ContentResponse, Void>() {
            @Override
            protected ContentResponse doInBackground() {
                return contentService.updateContent(contentId, dirId, title, content);
            }

            @Override
            protected void done() {
                try {
                    ContentResponse response = get();
                    if (response != null) {
                        NotificationUtil.showToast(mainFrame, "保存成功！");
                        // 由于只是内容更新，UI上暂时不需要做额外刷新
                    }
                } catch (Exception e) {
                    NotificationUtil.showErrorDialog(mainFrame, "更新云文件失败: " + e.getMessage());
                    e.printStackTrace();
                } finally {
                    mainFrame.setCursor(Cursor.getDefaultCursor());
                }
            }
        }.execute();
    }


    public void saveCurrentFile() {
        Component activeComponent = mainFrame.getActiveEditorPanel();
        if (activeComponent == null) {
            // 没有活动的选项卡，无需执行任何操作
            return;
        }

        File fileToSave = null;
        String content = null;

        if (activeComponent instanceof EditorPanel) {
            EditorPanel activeEditorPanel = (EditorPanel) activeComponent;
            fileToSave = activeEditorPanel.getCurrentFile();
            content = activeEditorPanel.getTextAreaContent();
        } else if (activeComponent instanceof LargeFileEditorPanel) {
            LargeFileEditorPanel activeLargeFileEditorPanel = (LargeFileEditorPanel) activeComponent;
            fileToSave = activeLargeFileEditorPanel.getCurrentFile();
            content = activeLargeFileEditorPanel.getTextAreaContent();
        }

        if (fileToSave == null) {
            // 如果没有打开的文件，则可以调用“另存为”
            onSaveAs();
            return;
        }

        try {
            fileProcessorService.saveFile(fileToSave, content);
            NotificationUtil.showSaveSuccess(mainFrame);
        } catch (IOException e) {
            NotificationUtil.showErrorDialog(mainFrame, "保存文件失败: " + e.getMessage());
        }
    }

}
