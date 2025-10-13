package com.lfs.util;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.apache.ibatis.datasource.DataSourceFactory;

import javax.sql.DataSource;
import java.util.Properties;

/**
 * 自定义的 HikariCP 数据源工厂，用于集成 MyBatis 和 HikariCP。
 */
public class HikariCPDataSourceFactory implements DataSourceFactory {

    private Properties props;

    @Override
    public void setProperties(Properties props) {
        this.props = props;
    }

    @Override
    public DataSource getDataSource() {
        // HikariConfig 会自动映射 mybatis-config.xml 中 dataSource 节点下的 property
        HikariConfig config = new HikariConfig(props);
        // 我们可以在这里添加一些额外的、固定的配置
        config.addDataSourceProperty("cachePrepStmts", "true");
        config.addDataSourceProperty("prepStmtCacheSize", "250");
        config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
        return new HikariDataSource(config);
    }
}
