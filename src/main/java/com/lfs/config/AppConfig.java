package com.lfs.config;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * 应用程序范围的配置常量
 */
public class AppConfig {

    /**
     * API基础URL
     */
    public static final String BASE_URL = "http://localhost:6324";

    /**
     * API前缀
     */
    public static final String API_PREFIX = "/lfs-code-assistant";

    /**
     * 目录树
     */
    public static final String DIR_TREE_URL = API_PREFIX + "/dir/tree";

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
