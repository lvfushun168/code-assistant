package com.lfs.util;

import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.io.Resources;
import com.baomidou.mybatisplus.core.MybatisSqlSessionFactoryBuilder;
import org.apache.ibatis.io.Resources;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * MyBatis 工具类
 */
@Slf4j
public class MyBatisUtil {

    private static SqlSessionFactory sqlSessionFactory;

    static {
        try {
            String resource = "mybatis-config.xml";
            InputStream inputStream = Resources.getResourceAsStream(resource);

            Properties properties = new Properties();
            properties.setProperty("db.password", System.getenv("DB_PASSWORD"));

            sqlSessionFactory = new MybatisSqlSessionFactoryBuilder().build(inputStream, properties);
        } catch (IOException e) {
            log.error("MyBatis 初始化失败", e);
        }
    }

    /**
     * 获取 SqlSession
     *
     * @return SqlSession 实例
     */
    public static SqlSession getSqlSession() {
        return sqlSessionFactory.openSession();
    }
}
