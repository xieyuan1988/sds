package com.taobao.tddl.atom.jdbc;

import com.taobao.tddl.common.model.SqlMetaData;

import java.util.ArrayList;
import java.util.List;

public class SqlMetaDataImpl implements SqlMetaData {

    private StringBuilder sqlBuilder = new StringBuilder();

    private List<String> logicTables = new ArrayList<String>();

    private String oriSql = null;

    private boolean parsed = false;

    public StringBuilder getSqlBuilder() {
        return sqlBuilder;
    }

    public void setSqlBuilder(StringBuilder sqlBuilder) {
        this.sqlBuilder = sqlBuilder;
    }

    public void addLogicTables(String... logicTables) {
        for (String table : logicTables) {
            this.logicTables.add(table);
        }
    }

    public void setOriSql(String oriSql) {
        this.oriSql = oriSql;
    }

    @Override
    public String getOriSql() {
        return this.oriSql;
    }

    @Override
    public String getLogicSql() {
        return sqlBuilder.toString();
    }

    @Override
    public List<String> getLogicTables() {
        return logicTables;
    }

    public boolean isParsed() {
        return parsed;
    }

    public void setParsed(boolean parsed) {
        this.parsed = parsed;
    }

}
