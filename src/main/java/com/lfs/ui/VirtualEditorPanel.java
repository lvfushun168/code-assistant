package com.lfs.ui;

import com.lfs.util.NotificationUtil;

import javax.swing.*;
import java.awt.*;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class VirtualEditorPanel extends JPanel {

    private final JList<String> lineList;
    private final FileLineListModel model;
    private final JScrollPane scrollPane;
    private final JLabel statusLabel;

    public VirtualEditorPanel() {
        super(new BorderLayout());
        model = new FileLineListModel();
        lineList = new JList<>(model);
        lineList.setCellRenderer(new LineCellRenderer());
        lineList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        // 提升滚动性能
        lineList.setFixedCellHeight(16); // 设定一个合理的固定高度
        lineList.setPrototypeCellValue(String.format("%" + 100 + "s", ""));


        scrollPane = new JScrollPane(lineList);
        add(scrollPane, BorderLayout.CENTER);

        statusLabel = new JLabel(" ");
        statusLabel.setBorder(BorderFactory.createEmptyBorder(2, 5, 2, 5));
        add(statusLabel, BorderLayout.SOUTH);
    }

    public void loadFile(File file) {
        statusLabel.setText("正在为 " + file.getName() + " 创建索引...");
        setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));

        new SwingWorker<List<Long>, Void>() {
            @Override
            protected List<Long> doInBackground() throws Exception {
                return model.indexFile(file);
            }

            @Override
            protected void done() {
                try {
                    List<Long> lineOffsets = get();
                    model.setLineOffsets(lineOffsets);
                    statusLabel.setText("加载完成: " + file.getName() + " - 共 " + model.getSize() + " 行");
                } catch (Exception e) {
                    e.printStackTrace();
                    NotificationUtil.showErrorDialog(VirtualEditorPanel.this, "创建文件索引失败: " + e.getMessage());
                    statusLabel.setText("加载失败: " + e.getMessage());
                } finally {
                    setCursor(Cursor.getDefaultCursor());
                }
            }
        }.execute();
    }

    // 用于处理大文件的自定义列表模型
    private static class FileLineListModel extends AbstractListModel<String> {
        private RandomAccessFile file;
        private List<Long> lineOffsets = new ArrayList<>();

        public List<Long> indexFile(File f) throws IOException {
            List<Long> offsets = new ArrayList<>();
            if (this.file != null) {
                this.file.close();
            }
            this.file = new RandomAccessFile(f, "r");
            offsets.add(0L); // 第一行从 0 开始

            byte[] buffer = new byte[81920];  // 缓冲区80kb
            int bytesRead;
            long currentFilePointer = 0;

            while ((bytesRead = file.read(buffer)) != -1) {
                for (int i = 0; i < bytesRead; i++) {
                    if (buffer[i] == '\n') {
                        offsets.add(currentFilePointer + i + 1);
                    }
                }
                currentFilePointer += bytesRead;
            }
            // 确保通过 getElementAt 函数重置文件指针，以便后续读取
            file.seek(0);
            return offsets;
        }

        public void setLineOffsets(List<Long> lineOffsets) {
            int oldSize = getSize();
            this.lineOffsets = lineOffsets;
            int newSize = getSize();
            if (newSize > oldSize) {
                fireIntervalAdded(this, oldSize, newSize - 1);
            } else if (newSize < oldSize) {
                fireIntervalRemoved(this, newSize, oldSize - 1);
            }
            fireContentsChanged(this, 0, newSize - 1);
        }

        @Override
        public int getSize() {
            return lineOffsets.isEmpty() ? 0 : lineOffsets.size();
        }

        @Override
        public String getElementAt(int index) {
            if (index < 0 || index >= lineOffsets.size()) {
                return null;
            }
            try {
                long start = lineOffsets.get(index);
                file.seek(start);

                // 读取直到换行符或文件结束符
                byte[] buffer = new byte[1024]; // 根据需要调整缓冲区大小
                int bytesRead;
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                boolean eol = false;
                while ((bytesRead = file.read(buffer)) != -1 && !eol) {
                    for (int i = 0; i < bytesRead; i++) {
                        if (buffer[i] == '\n') {
                            baos.write(buffer, 0, i);
                            eol = true;
                            break;
                        }
                    }
                    if (!eol) {
                        baos.write(buffer, 0, bytesRead);
                    }
                }
                 return new String(baos.toByteArray(), StandardCharsets.UTF_8);
            } catch (IOException e) {
                e.printStackTrace();
                return "Error reading line " + index;
            }
        }


    }

    // 自定义CellRenderer以显示行号和内容
    private static class LineCellRenderer extends DefaultListCellRenderer {
        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
            JLabel label = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            label.setText(" " + (index + 1) + "  " + value.toString());
            label.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
            return label;
        }
    }
}
