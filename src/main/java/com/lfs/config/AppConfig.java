package com.lfs.config;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * 应用程序范围的配置常量
 */
public class AppConfig {

    /**
     * 允许应用程序读取的文件扩展名
     */
    public static final Set<String> ALLOWED_EXTENSIONS = new HashSet<>(Arrays.asList(
            "java", "py", "python", "html", "htm", "css", "js", "ts",
            "yml", "yaml", "properties", "conf", "config", "application",
            "txt", "text", "md", "sql", "xml", "json", "sh", "bat", "gradle", "kt", "kts", "log",
            "c", "h", "cpp", "hpp", "cs", "go", "rb", "php", "swift"
    ));

    private AppConfig() {
    }
}
