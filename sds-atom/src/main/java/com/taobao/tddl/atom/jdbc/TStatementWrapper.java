package com.taobao.tddl.atom.jdbc;

import com.taobao.tddl.atom.TAtomDbStatusEnum;
import com.taobao.tddl.common.jdbc.SqlTypeParser;
import com.taobao.tddl.common.model.SqlMetaData;
import com.taobao.tddl.common.utils.logger.Logger;
import com.taobao.tddl.common.utils.logger.LoggerFactory;

import java.sql.*;

/**
 * Statement 包装类
 *
 * @author shenxun
 */
public class TStatementWrapper implements TStatement {

    private static Logger log = LoggerFactory.getLogger(TStatementWrapper.class);
    protected static final String UPDATE = "UPDATE";
    protected static final String QUERY = "QUERY";

    protected Statement targetStatement;
    protected final TConnectionWrapper connectionWrapper;
    protected final TDataSourceWrapper datasourceWrapper;

    /**
     * 经过计算后的结果集，允许使用 getResult函数调用. 一个statement只允许有一个结果集
     */
    protected TResultSetWrapper currentResultSet;

    /**
     * sql元信息持有
     */
    protected SqlMetaData sqlMetaData = null;

    public TStatementWrapper(Statement targetStatement, TConnectionWrapper connectionWrapper,
                             TDataSourceWrapper tdsWrapper) {
        this.targetStatement = targetStatement;
        this.connectionWrapper = connectionWrapper;
        this.datasourceWrapper = tdsWrapper;
    }

    public void addBatch(String sql) throws SQLException {
        this.targetStatement.addBatch(sql);
    }

    public void cancel() throws SQLException {
        this.targetStatement.cancel();
    }

    public void clearBatch() throws SQLException {
        this.targetStatement.clearBatch();
    }

    public void clearWarnings() throws SQLException {
        this.targetStatement.clearWarnings();
    }

    public void close() throws SQLException {
        close(true);
    }

    void close(boolean removeThis) throws SQLException {
        try {
            if (currentResultSet != null) currentResultSet.close();
        } catch (SQLException e) {
            log.warn("Close currentResultSet failed.", e);
        } finally {
            currentResultSet = null;
        }

        try {
            if (this.targetStatement != null) this.targetStatement.close();
        } finally {
            this.targetStatement = null; // 端口与物理statement的引用，底下可能会有ps
            // cache，导致节点无法被gc
            if (removeThis) {
                // 关闭之后，移除
                connectionWrapper.removeOpenedStatements(this);
            }
        }
    }

    protected void recordReadTimes() throws SQLException {
        TAtomDbStatusEnum status = datasourceWrapper.connectionProperties.dbStatus;
        if (status != TAtomDbStatusEnum.R_STATUS && status != TAtomDbStatusEnum.RW_STATUS) {
            throw new SQLException("db do not allow to execute read ! dbStatus is " + status);
        }
        /*
         * int readRestrictionTimes =
         * datasourceWrapper.connectionProperties.readRestrictionTimes; int
         * currentReadTimes = datasourceWrapper.readTimes.incrementAndGet(); if
         * (readRestrictionTimes != 0) { if (currentReadTimes >
         * readRestrictionTimes) {
         * datasourceWrapper.readTimesReject.incrementAndGet(); throw new
         * SQLException("max read times ," + currentReadTimes); } }
         */
        if (!datasourceWrapper.readFlowControl.allow()) {
            throw new SQLException(datasourceWrapper.readFlowControl.reportExceed());
        }
    }

    protected void recordWriteTimes() throws SQLException {
        TAtomDbStatusEnum status = datasourceWrapper.connectionProperties.dbStatus;
        if (status != TAtomDbStatusEnum.W_STATUS && status != TAtomDbStatusEnum.RW_STATUS) {
            throw new SQLException("db do not allow to execute write ! dbStatus is " + status);
        }
        /*
         * int writeRestrictionTimes =
         * datasourceWrapper.connectionProperties.writeRestrictionTimes; int
         * currentWriteTimes = datasourceWrapper.writeTimes.incrementAndGet();
         * if (writeRestrictionTimes != 0) { if (currentWriteTimes >
         * writeRestrictionTimes) {
         * datasourceWrapper.writeTimesReject.incrementAndGet(); throw new
         * SQLException("max write times , " + currentWriteTimes); } }
         */
        if (!datasourceWrapper.writeFlowControl.allow()) {
            throw new SQLException(datasourceWrapper.writeFlowControl.reportExceed());
        }
    }

    // 增加并发读计数并判断阀值
    protected void increaseConcurrentRead() throws SQLException {
        int maxConcurrentReadRestrict = datasourceWrapper.connectionProperties.maxConcurrentReadRestrict;
        int concurrentReadCount = datasourceWrapper.concurrentReadCount.incrementAndGet();
        if (maxConcurrentReadRestrict != 0) {
            if (concurrentReadCount > maxConcurrentReadRestrict) {
                datasourceWrapper.readTimesReject.incrementAndGet();
                throw new SQLException("maxConcurrentReadRestrict reached , " + maxConcurrentReadRestrict);
            }
        }
    }

    // 增加并发写计数并判断阀值
    protected void increaseConcurrentWrite() throws SQLException {
        int maxConcurrentWriteRestrict = datasourceWrapper.connectionProperties.maxConcurrentWriteRestrict;
        int concurrentWriteCount = datasourceWrapper.concurrentWriteCount.incrementAndGet();
        if (maxConcurrentWriteRestrict != 0) {
            if (concurrentWriteCount > maxConcurrentWriteRestrict) {
                datasourceWrapper.writeTimesReject.incrementAndGet();
                throw new SQLException("maxConcurrentWriteRestrict reached , " + maxConcurrentWriteRestrict);
            }
        }
    }

    // 减少并发读计数
    protected void decreaseConcurrentRead() throws SQLException {
        datasourceWrapper.concurrentReadCount.decrementAndGet();
    }

    // 减少并发写计数
    protected void decreaseConcurrentWrite() throws SQLException {
        datasourceWrapper.concurrentWriteCount.decrementAndGet();
    }

    public boolean execute(String sql) throws SQLException {
        return executeInternal(sql, -1, null, null);
    }

    public boolean execute(String sql, int autoGeneratedKeys) throws SQLException {
        return executeInternal(sql, autoGeneratedKeys, null, null);
    }

    public boolean execute(String sql, int[] columnIndexes) throws SQLException {
        return executeInternal(sql, -1, columnIndexes, null);
    }

    public boolean execute(String sql, String[] columnNames) throws SQLException {
        return executeInternal(sql, -1, null, columnNames);
    }

    private boolean executeInternal(String sql, int autoGeneratedKeys, int[] columnIndexes, String[] columnNames)
            throws SQLException {
        if (SqlTypeParser.isQuerySql(sql)) {
            executeQuery(sql);
            return true;
        } else {
            executeUpdateInternal(sql, autoGeneratedKeys, columnIndexes, columnNames);
            return false;
        }
    }

    private int executeUpdateInternal(String sql, int autoGeneratedKeys, int[] columnIndexes, String[] columnNames)
            throws SQLException {
        // if (sqlMetaData == null) throw new
        // NullPointerException("miss sql meta data.");
        ensureResultSetIsEmpty();
        recordWriteTimes();
        increaseConcurrentWrite();
        long time0 = System.currentTimeMillis();
        Exception e0 = null;

        try {
            if (autoGeneratedKeys == -1 && columnIndexes == null && columnNames == null) {
                return this.targetStatement.executeUpdate(sql);
            } else if (autoGeneratedKeys != -1) {
                return this.targetStatement.executeUpdate(sql, autoGeneratedKeys);
            } else if (columnIndexes != null) {
                return this.targetStatement.executeUpdate(sql, columnIndexes);
            } else if (columnNames != null) {
                return this.targetStatement.executeUpdate(sql, columnNames);
            } else {
                return this.targetStatement.executeUpdate(sql);
            }
        } catch (SQLException e) {
            e0 = e;
            throw e;
        } finally {
            endRpc(sql, e0);
            decreaseConcurrentWrite();
        }
    }

    public int[] executeBatch() throws SQLException {
        ensureResultSetIsEmpty();
        recordWriteTimes();
        increaseConcurrentWrite();
        try {
            return this.targetStatement.executeBatch();
        } finally {
            decreaseConcurrentWrite();
        }
    }

    public ResultSet executeQuery(String sql) throws SQLException {
        // if (sqlMetaData == null) throw new
        // NullPointerException("miss sql meta data.");

        ensureResultSetIsEmpty();
        recordReadTimes();
        increaseConcurrentRead();
        long time0 = System.currentTimeMillis();
        Exception e0 = null;

        try {
            currentResultSet = new TResultSetWrapper(this, this.targetStatement.executeQuery(sql));
            return currentResultSet;
        } catch (SQLException e) {
            decreaseConcurrentRead();
            e0 = e;
            throw e;
        } finally {
            endRpc(sql, e0);
        }
    }

    protected void endRpc(String sql, Exception e) {
        if (sqlMetaData == null) sqlMetaData = SqlMetaDataFactory.getSqlMetaData(sql);
    }

    public int executeUpdate(String sql) throws SQLException {
        return executeUpdateInternal(sql, -1, null, null);
    }

    public int executeUpdate(String sql, int autoGeneratedKeys) throws SQLException {
        return executeUpdateInternal(sql, autoGeneratedKeys, null, null);
    }

    public int executeUpdate(String sql, int[] columnIndexes) throws SQLException {
        return executeUpdateInternal(sql, -1, columnIndexes, null);
    }

    public int executeUpdate(String sql, String[] columnNames) throws SQLException {
        return executeUpdateInternal(sql, -1, null, columnNames);
    }

    public Connection getConnection() throws SQLException {
        return connectionWrapper;
    }

    public int getFetchDirection() throws SQLException {
        return this.targetStatement.getFetchDirection();
    }

    public int getFetchSize() throws SQLException {
        return this.targetStatement.getFetchSize();
    }

    public ResultSet getGeneratedKeys() throws SQLException {
        return new TResultSetWrapper(this, this.targetStatement.getGeneratedKeys());
    }

    public int getMaxFieldSize() throws SQLException {
        return this.targetStatement.getMaxFieldSize();
    }

    public int getMaxRows() throws SQLException {
        return this.targetStatement.getMaxRows();
    }

    public boolean getMoreResults() throws SQLException {
        return this.targetStatement.getMoreResults();
    }

    public boolean getMoreResults(int current) throws SQLException {
        return this.targetStatement.getMoreResults(current);
    }

    public int getQueryTimeout() throws SQLException {
        return this.targetStatement.getQueryTimeout();
    }

    // FIXME
    public ResultSet getResultSet() throws SQLException {
        /*
         * ResultSet targetRS = this.targetStatement.getResultSet(); if
         * (targetRS == null) { return null; } return new
         * TResultSetWrapper(this, targetRS);
         */
        return currentResultSet;
    }

    public int getResultSetConcurrency() throws SQLException {
        return this.targetStatement.getResultSetConcurrency();
    }

    public int getResultSetHoldability() throws SQLException {
        return this.targetStatement.getResultSetHoldability();
    }

    public int getResultSetType() throws SQLException {
        return this.targetStatement.getResultSetType();
    }

    public int getUpdateCount() throws SQLException {
        return this.targetStatement.getUpdateCount();
    }

    public SQLWarning getWarnings() throws SQLException {
        return this.targetStatement.getWarnings();
    }

    public void setCursorName(String name) throws SQLException {
        this.targetStatement.setCursorName(name);
    }

    public void setEscapeProcessing(boolean enable) throws SQLException {
        this.targetStatement.setEscapeProcessing(enable);
    }

    public void setFetchDirection(int direction) throws SQLException {
        this.targetStatement.setFetchDirection(direction);
    }

    public void setFetchSize(int rows) throws SQLException {
        this.targetStatement.setFetchSize(rows);
    }

    public void setMaxFieldSize(int max) throws SQLException {
        this.targetStatement.setMaxFieldSize(max);
    }

    public void setMaxRows(int max) throws SQLException {
        this.targetStatement.setMaxRows(max);
    }

    public void setQueryTimeout(int seconds) throws SQLException {
        this.targetStatement.setQueryTimeout(seconds);
    }

    /**
     * 如果新建了查询，那么上一次查询的结果集应该被显示的关闭掉。这才是符合jdbc规范的
     *
     * @throws java.sql.SQLException
     */
    protected void ensureResultSetIsEmpty() throws SQLException {
        if (currentResultSet != null) {
            try {
                currentResultSet.close();
            } catch (SQLException e) {
                log.error("exception on close last result set . can do nothing..", e);
            } finally {
                currentResultSet = null;
            }
        }
    }

    public boolean isWrapperFor(Class<?> iface) throws SQLException {
        return this.getClass().isAssignableFrom(iface);
    }

    @SuppressWarnings("unchecked")
    public <T> T unwrap(Class<T> iface) throws SQLException {
        try {
            return (T) this;
        } catch (Exception e) {
            throw new SQLException(e);
        }
    }

    public boolean isClosed() throws SQLException {
        return this.targetStatement.isClosed();
    }

    public void setPoolable(boolean poolable) throws SQLException {
        this.targetStatement.setPoolable(poolable);
    }

    public boolean isPoolable() throws SQLException {
        return this.targetStatement.isPoolable();
    }

    @Override
    public void closeOnCompletion() throws SQLException {

    }

    @Override
    public boolean isCloseOnCompletion() throws SQLException {
        return false;
    }

    @Override
    public void fillMetaData(SqlMetaData sqlMetaData) {
        this.sqlMetaData = sqlMetaData;
    }

    @Override
    public SqlMetaData getSqlMetaData() {
        return this.sqlMetaData;
    }
}
