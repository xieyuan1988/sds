package com.taobao.tddl.common.mock;

import com.taobao.tddl.common.jdbc.ParameterContext;
import com.taobao.tddl.common.jdbc.ParameterMethod;
import com.taobao.tddl.common.mock.MockDataSource.ExecuteInfo;
import com.taobao.tddl.common.utils.logger.Logger;
import com.taobao.tddl.common.utils.logger.LoggerFactory;

import java.io.InputStream;
import java.io.Reader;
import java.math.BigDecimal;
import java.net.URL;
import java.sql.*;
import java.sql.Date;
import java.util.*;

public class MockPreparedStatement extends MockStatement implements PreparedStatement {

    private static final Logger logger = LoggerFactory.getLogger(MockPreparedStatement.class);

    private int autoGeneratedKeys = -1;

    private int[] columnIndexes;

    private String[] columnNames;

    private Map<Integer, ParameterContext> parameterSettings = new TreeMap<Integer, ParameterContext>();

    public MockPreparedStatement(String method, MockDataSource mockDataSource, String sql) {
        super(method, mockDataSource, sql);
    }

    public List<Object> getParameters() {
        List<Object> parameters = new ArrayList<Object>();
        for (ParameterContext context : parameterSettings.values()) {
            if (context.getParameterMethod() != ParameterMethod.setNull1
                    && context.getParameterMethod() != ParameterMethod.setNull2) {
                parameters.add(context.getArgs()[1]);
            } else {
                parameters.add(null);
            }
        }

        return parameters;
    }

    public void clearParameters() throws SQLException {
        parameterSettings.clear();
    }

    public boolean execute() throws SQLException {
        checkClosed();
        return executerHandler.executeSql("ps.execute", sql);
    }

    public ResultSet executeQuery() throws SQLException {
        if (logger.isDebugEnabled()) {
            logger.debug("invoke executeQuery, sql = " + sql);
        }

        checkClosed();

        SQLException e = MockDataSource.popPreException(MockDataSource.m_executeQuery);
        if (e != null) {
            throw e;
        }

        return executerHandler.execute("ps.executeQuery", sql);
    }

    public int executeUpdate() throws SQLException {
        if (logger.isDebugEnabled()) {
            logger.debug("invoke executeUpdate, sql = " + sql);
        }

        checkClosed();

        SQLException e = MockDataSource.popPreException(MockDataSource.m_executeUpdate);
        if (e != null) {
            throw e;
        }

        return super.updateInternal("ps.executeUpdate", sql);
    }

    public ResultSetMetaData getMetaData() throws SQLException {
        throw new UnsupportedOperationException("getMetaData");
    }

    public ParameterMetaData getParameterMetaData() throws SQLException {
        throw new UnsupportedOperationException("getParameterMetaData");
    }

    public void setArray(int i, Array x) throws SQLException {
        parameterSettings.put(i, new ParameterContext(ParameterMethod.setArray, new Object[]{i, x}));
    }

    public void setAsciiStream(int parameterIndex, InputStream x, int length) throws SQLException {
        parameterSettings.put(parameterIndex, new ParameterContext(ParameterMethod.setAsciiStream, new Object[]{
                parameterIndex, x, length}));
    }

    public void setBigDecimal(int parameterIndex, BigDecimal x) throws SQLException {
        parameterSettings.put(parameterIndex, new ParameterContext(ParameterMethod.setBigDecimal, new Object[]{
                parameterIndex, x}));
    }

    public void setBinaryStream(int parameterIndex, InputStream x, int length) throws SQLException {
        parameterSettings.put(parameterIndex, new ParameterContext(ParameterMethod.setBinaryStream, new Object[]{
                parameterIndex, x, length}));
    }

    public void setBlob(int i, Blob x) throws SQLException {
        parameterSettings.put(i, new ParameterContext(ParameterMethod.setBlob, new Object[]{i, x}));
    }

    public void setBoolean(int parameterIndex, boolean x) throws SQLException {
        parameterSettings.put(parameterIndex, new ParameterContext(ParameterMethod.setBoolean, new Object[]{
                parameterIndex, x}));
    }

    public void setByte(int parameterIndex, byte x) throws SQLException {
        parameterSettings.put(parameterIndex, new ParameterContext(ParameterMethod.setByte, new Object[]{
                parameterIndex, x}));
    }

    public void setBytes(int parameterIndex, byte[] x) throws SQLException {
        parameterSettings.put(parameterIndex, new ParameterContext(ParameterMethod.setBytes, new Object[]{
                parameterIndex, x}));
    }

    public void setCharacterStream(int parameterIndex, Reader reader, int length) throws SQLException {
        parameterSettings.put(parameterIndex, new ParameterContext(ParameterMethod.setCharacterStream, new Object[]{
                parameterIndex, reader, length}));
    }

    public void setClob(int i, Clob x) throws SQLException {
        parameterSettings.put(i, new ParameterContext(ParameterMethod.setClob, new Object[]{i, x}));
    }

    public void setDate(int parameterIndex, Date x) throws SQLException {
        parameterSettings.put(parameterIndex, new ParameterContext(ParameterMethod.setDate1, new Object[]{
                parameterIndex, x}));
    }

    public void setDate(int parameterIndex, Date x, Calendar cal) throws SQLException {
        parameterSettings.put(parameterIndex, new ParameterContext(ParameterMethod.setDate2, new Object[]{
                parameterIndex, x, cal}));
    }

    public void setDouble(int parameterIndex, double x) throws SQLException {
        parameterSettings.put(parameterIndex, new ParameterContext(ParameterMethod.setDouble, new Object[]{
                parameterIndex, x}));
    }

    public void setFloat(int parameterIndex, float x) throws SQLException {
        parameterSettings.put(parameterIndex, new ParameterContext(ParameterMethod.setFloat, new Object[]{
                parameterIndex, x}));
    }

    public void setInt(int parameterIndex, int x) throws SQLException {
        parameterSettings.put(parameterIndex, new ParameterContext(ParameterMethod.setInt, new Object[]{
                parameterIndex, x}));
    }

    public void setLong(int parameterIndex, long x) throws SQLException {
        parameterSettings.put(parameterIndex, new ParameterContext(ParameterMethod.setLong, new Object[]{
                parameterIndex, x}));
    }

    public void setNull(int parameterIndex, int sqlType) throws SQLException {
        parameterSettings.put(parameterIndex, new ParameterContext(ParameterMethod.setNull1, new Object[]{
                parameterIndex, sqlType}));
    }

    public void setNull(int paramIndex, int sqlType, String typeName) throws SQLException {
        parameterSettings.put(paramIndex, new ParameterContext(ParameterMethod.setNull2, new Object[]{paramIndex,
                sqlType, typeName}));
    }

    public void setObject(int parameterIndex, Object x) throws SQLException {
        parameterSettings.put(parameterIndex, new ParameterContext(ParameterMethod.setObject1, new Object[]{
                parameterIndex, x}));
    }

    public void setObject(int parameterIndex, Object x, int targetSqlType) throws SQLException {
        parameterSettings.put(parameterIndex, new ParameterContext(ParameterMethod.setObject2, new Object[]{
                parameterIndex, x, targetSqlType}));
    }

    public void setObject(int parameterIndex, Object x, int targetSqlType, int scale) throws SQLException {
        parameterSettings.put(parameterIndex, new ParameterContext(ParameterMethod.setObject3, new Object[]{
                parameterIndex, x, targetSqlType, scale}));
    }

    public void setRef(int i, Ref x) throws SQLException {
        parameterSettings.put(i, new ParameterContext(ParameterMethod.setRef, new Object[]{i, x}));
    }

    public void setShort(int parameterIndex, short x) throws SQLException {
        parameterSettings.put(parameterIndex, new ParameterContext(ParameterMethod.setShort, new Object[]{
                parameterIndex, x}));
    }

    public void setString(int parameterIndex, String x) throws SQLException {
        parameterSettings.put(parameterIndex, new ParameterContext(ParameterMethod.setString, new Object[]{
                parameterIndex, x}));
    }

    public void setTime(int parameterIndex, Time x) throws SQLException {
        parameterSettings.put(parameterIndex, new ParameterContext(ParameterMethod.setTime1, new Object[]{
                parameterIndex, x}));
    }

    public void setTime(int parameterIndex, Time x, Calendar cal) throws SQLException {
        parameterSettings.put(parameterIndex, new ParameterContext(ParameterMethod.setTime2, new Object[]{
                parameterIndex, x, cal}));
    }

    public void setTimestamp(int parameterIndex, Timestamp x) throws SQLException {
        parameterSettings.put(parameterIndex, new ParameterContext(ParameterMethod.setTimestamp1, new Object[]{
                parameterIndex, x}));
    }

    public void setTimestamp(int parameterIndex, Timestamp x, Calendar cal) throws SQLException {
        parameterSettings.put(parameterIndex, new ParameterContext(ParameterMethod.setTimestamp2, new Object[]{
                parameterIndex, x, cal}));
    }

    public void setURL(int parameterIndex, URL x) throws SQLException {
        parameterSettings.put(parameterIndex, new ParameterContext(ParameterMethod.setURL, new Object[]{
                parameterIndex, x}));
    }

    @Deprecated
    public void setUnicodeStream(int parameterIndex, InputStream x, int length) throws SQLException {
        parameterSettings.put(parameterIndex, new ParameterContext(ParameterMethod.setUnicodeStream, new Object[]{
                parameterIndex, x, length}));
    }

    List<Object> batchedArgs;

    public void addBatch() throws SQLException {
        if (batchedArgs == null) {
            batchedArgs = new ArrayList<Object>();
        }

        List<ParameterContext> batchedParameterSettings = new ArrayList<ParameterContext>();
        batchedParameterSettings.addAll(parameterSettings.values());

        batchedArgs.add(batchedParameterSettings);
    }

    public int[] executeBatch() throws SQLException {
        // int[] ints = new int[batchedArgs.size()];
        // for(int i = 0 ; i <ints.length ; i ++){
        // ints[i] = executeUpdate();
        // }
        // return ints;
        mds.checkState();
        MockDataSource.record(new ExecuteInfo(this.mds, "executeBatch", this.sql, null));
        return new int[]{-1, -1};
    }

    public void clearBatch() throws SQLException {
        super.clearBatch();
    }

    public String getSql() {
        return sql;
    }

    public void setSql(String sql) {
        this.sql = sql;
    }

    public int getAutoGeneratedKeys() {
        return autoGeneratedKeys;
    }

    public void setAutoGeneratedKeys(int autoGeneratedKeys) {
        this.autoGeneratedKeys = autoGeneratedKeys;
    }

    public int[] getColumnIndexes() {
        return columnIndexes;
    }

    public void setColumnIndexes(int[] columnIndexes) {
        this.columnIndexes = columnIndexes;
    }

    public String[] getColumnNames() {
        return columnNames;
    }

    public void setColumnNames(String[] columnNames) {
        this.columnNames = columnNames;
    }

    public void setPoolable(boolean poolable) throws SQLException {

    }

    public boolean isPoolable() throws SQLException {

        return false;
    }

    public <T> T unwrap(Class<T> iface) throws SQLException {

        return null;
    }

    public boolean isWrapperFor(Class<?> iface) throws SQLException {

        return false;
    }

    public void setRowId(int parameterIndex, RowId x) throws SQLException {

    }

    public void setNString(int parameterIndex, String value) throws SQLException {

    }

    public void setNCharacterStream(int parameterIndex, Reader value, long length) throws SQLException {

    }

    public void setNClob(int parameterIndex, NClob value) throws SQLException {

    }

    public void setClob(int parameterIndex, Reader reader, long length) throws SQLException {

    }

    public void setBlob(int parameterIndex, InputStream inputStream, long length) throws SQLException {

    }

    public void setNClob(int parameterIndex, Reader reader, long length) throws SQLException {

    }

    public void setSQLXML(int parameterIndex, SQLXML xmlObject) throws SQLException {

    }

    public void setAsciiStream(int parameterIndex, InputStream x, long length) throws SQLException {

    }

    public void setBinaryStream(int parameterIndex, InputStream x, long length) throws SQLException {

    }

    public void setCharacterStream(int parameterIndex, Reader reader, long length) throws SQLException {

    }

    public void setAsciiStream(int parameterIndex, InputStream x) throws SQLException {

    }

    public void setBinaryStream(int parameterIndex, InputStream x) throws SQLException {

    }

    public void setCharacterStream(int parameterIndex, Reader reader) throws SQLException {

    }

    public void setNCharacterStream(int parameterIndex, Reader value) throws SQLException {

    }

    public void setClob(int parameterIndex, Reader reader) throws SQLException {

    }

    public void setBlob(int parameterIndex, InputStream inputStream) throws SQLException {

    }

    public void setNClob(int parameterIndex, Reader reader) throws SQLException {

    }
}
