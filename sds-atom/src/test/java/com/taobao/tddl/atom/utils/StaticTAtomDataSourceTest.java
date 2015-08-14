package com.taobao.tddl.atom.utils;

import com.taobao.tddl.atom.StaticTAtomDataSource;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.jdbc.core.JdbcTemplate;

public class StaticTAtomDataSourceTest {

    @Test
    public void createStaticTAtomDataSource() throws Exception {
        StaticTAtomDataSource dataSource = new StaticTAtomDataSource();
        dataSource.setIp("10.232.31.154");
        dataSource.setPort("3306");
        dataSource.setDbName("test");
        dataSource.setDbType("mysql");
        dataSource.setUserName("tddl");
        dataSource.setPasswd("tddl");
        dataSource.setMinPoolSize(1);
        dataSource.setMaxPoolSize(2);
        // 初始化
        dataSource.init();
        JdbcTemplate jtp = new JdbcTemplate();
        jtp.setDataSource(dataSource);
        int actual = jtp.queryForObject("select 1 from dual", Integer.class);
        Assert.assertEquals(actual, 1);
        dataSource.destroyDataSource();
    }
}
