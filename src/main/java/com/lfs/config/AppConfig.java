package com.lfs.config;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * 应用程序范围的配置常量
 */
public class AppConfig {

    /**
     * 开发模式切换
     * true: 使用本地地址 (http://localhost:6324)
     * false: 使用生产地址 (https://8.148.146.195)
     */
    private static final boolean IS_DEV_MODE = false;
//    private static final boolean IS_DEV_MODE = true;


    /**
     * 生产环境API基础URL
     */
    public static final String PROD_BASE_URL = "https://8.148.146.195";

    /**
     * 本地开发API基础URL
     */
    public static final String LOCAL_BASE_URL = "http://localhost:6324";

    /**
     * API基础URL
     */
    public static final String BASE_URL = IS_DEV_MODE ? LOCAL_BASE_URL : PROD_BASE_URL;

    /**
     * API前缀
     */
    public static final String API_PREFIX = "/lfs-code-assistant";

    /**
     * 目录树
     */
    public static final String DIR_TREE_URL = API_PREFIX + "/dir/tree";

    /**
     * 目录
     */
    public static final String DIR_URL = API_PREFIX + "/dir";
    /**
     * 注册
     */
    public static final String REGISTER_URL = API_PREFIX + "/account/register";
    /**
     * 登录
     */
    public static final String LOGIN_URL = API_PREFIX + "/account/login";
    /**
     * 验证码
     */
    public static final String CAPTCHA_URL = API_PREFIX + "/captcha/generate";

    /**
     * 内容下载
     */
    public static final String CONTENT_DOWNLOAD_URL = API_PREFIX + "/content/download";

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
