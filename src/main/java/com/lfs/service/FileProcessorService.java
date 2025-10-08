package com.lfs.service;

import com.lfs.config.AppConfig;

import com.lfs.config.AppConfig;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.Comparator;

/**
 * 文件处理
 */
public class FileProcessorService {

    /**
     * 处理所选目录，生成所有允许的文件内容的平面字符串。
     
     * @param directory 要处理的根目录
     * @return 一个包含所有有效文件的路径和内容的字符串
     * @throws IOException 文件读取错误
     */
    public String generateContentFromDirectory(File directory) throws IOException {
        StringBuilder contentBuilder = new StringBuilder();
        traverseDirectoryForContent(directory, contentBuilder);
        return contentBuilder.toString();
    }

    /**
     * 处理所选目录以生成树状结构字符串
     *
     * @param directory 要处理的根目录
     * @return 表示目录结构的字符串
     */
    public String generateStructureFromDirectory(File directory) {
        StringBuilder structureBuilder = new StringBuilder();
        structureBuilder.append(directory.getName()).append("\n"); // Add root directory
        buildTreeStructure(directory, structureBuilder, "");
        return structureBuilder.toString();
    }

    /**
     * 递归遍历目录，并将允许的文件内容添加到StringBuilder中。
     *
     * @param dir     要遍历的当前目录
     * @param builder 要向其中追加内容的StringBuilder
     * @throws IOException 文件读取错误
     */
    private void traverseDirectoryForContent(File dir, StringBuilder builder) throws IOException {
        File[] files = listAndSortFiles(dir);

        for (File file : files) {
            if (file.isDirectory()) {
                traverseDirectoryForContent(file, builder);
            } else {
                String fileName = file.getName();
                int lastDotIndex = fileName.lastIndexOf('.');
                if (lastDotIndex > 0) {
                    String extension = fileName.substring(lastDotIndex + 1).toLowerCase();
                    if (AppConfig.ALLOWED_EXTENSIONS.contains(extension)) {
                        String content = Files.readString(file.toPath());
//                        builder.append("--- 文件路径: ").append(file.getAbsolutePath()).append(" ---\n\n");
                        builder.append(content).append("\n\n");
                    }
                }
            }
        }
    }

    /**
     * 递归构建目录树的字符串表示形式
     *
     * @param dir     当前目录
     * @param builder 用于附加树结构的StringBuilder
     * @param prefix  绘制树线的前缀
     */
    private void buildTreeStructure(File dir, StringBuilder builder, String prefix) {
        File[] files = listAndSortFiles(dir);

        for (int i = 0; i < files.length; i++) {
            File file = files[i];
            boolean isLast = (i == files.length - 1);
            builder.append(prefix);
            builder.append(isLast ? "└── " : "├── ");
            builder.append(file.getName()).append("\n");

            if (file.isDirectory()) {
                String newPrefix = prefix + (isLast ? "    " : "│   ");
                buildTreeStructure(file, builder, newPrefix);
            }
        }
    }

    private File[] listAndSortFiles(File dir) {
        File[] files = dir.listFiles();
        if (files == null) {
            return new File[0]; // 返回空数组而不是null
        }
        Arrays.sort(files);
        return files;
    }


    /**
     * 读取单个文件的内容
     * @param file 要读取的文件
     * @return 文件内容
     * @throws IOException 读取异常
     */
    public String readFileContent(File file) throws IOException {
        return Files.readString(file.toPath());
    }

    /**
     * 将内容保存到指定文件
     * @param file 要保存的文件
     * @param content 要写入的内容
     * @throws IOException 写入异常
     */
    public void saveFile(File file, String content) throws IOException {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
            writer.write(content);
        }
    }

    /**
     * 创建新文件
     * @param parentDir 父目录
     * @param fileName 文件名
     * @return a new file
     * @throws IOException
     */
    public File createFile(File parentDir, String fileName) throws IOException {
        File newFile = new File(parentDir, fileName);
        if (newFile.createNewFile()) {
            return newFile;
        }
        return null;
    }

    /**
     * 创建新文件夹
     * @param parentDir 父目录
     * @param dirName 文件夹名称
     * @return a new directory
     */
    public File createDirectory(File parentDir, String dirName) {
        File newDir = new File(parentDir, dirName);
        if (newDir.mkdir()) {
            return newDir;
        }
        return null;
    }

    /**
     * 重命名文件或文件夹
     * @param oldFile 旧文件
     * @param newName 新名称
     * @return
     */
    public boolean renameFile(File oldFile, String newName) {
        File newFile = new File(oldFile.getParent(), newName);
        return oldFile.renameTo(newFile);
    }

    /**
     * 删除文件或文件夹
     * @param file
     * @return
     */
    public boolean deleteFile(File file) {
        if (file.isDirectory()) {
            File[] files = file.listFiles();
            if (files != null) {
                for (File f : files) {
                    deleteFile(f);
                }
            }
        }
        return file.delete();
    }

    /**
     * 复制文件
     * @param source
     * @param dest
     * @throws IOException
     */
    public void copyFile(File source, File dest) throws IOException {
        Files.copy(source.toPath(), dest.toPath(), StandardCopyOption.REPLACE_EXISTING);
    }
}
