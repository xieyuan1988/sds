package com.taobao.tddl.atom.jdbc;

import com.alibaba.druid.support.logging.Log;
import com.alibaba.druid.support.logging.LogFactory;
import com.taobao.tddl.atom.TAtomDbStatusEnum;
import com.taobao.tddl.atom.TAtomDbTypeEnum;
import com.taobao.tddl.atom.config.TAtomDsConfDO;
import com.taobao.tddl.atom.exception.AtomNotAvailableException;
import com.taobao.tddl.atom.utils.*;
import com.taobao.tddl.common.jdbc.sorter.ExceptionSorter;
import com.taobao.tddl.common.jdbc.sorter.MySQLExceptionSorter;
import com.taobao.tddl.common.jdbc.sorter.OracleExceptionSorter;
import org.apache.commons.lang3.StringUtils;

import javax.sql.DataSource;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Logger;

public class TDataSourceWrapper implements DataSource {

    private static Log logger = LogFactory.getLog(TDataSourceWrapper.class);
    private final DataSource targetDataSource;
    /**
     * 当前线程的threadCount值,如果进行了切换。 那么使用的是不同的Datasource包装类，不会相互影响。
     * threadCount输出在切换过程中在那个时候不能反应准确的值。
     * 但因为旧的被丢弃前也有用，等于在内存中维持了两份不同的TDataSourceWrapper. 因此线程计数不会额外增加。
     */
    final AtomicInteger threadCount = new AtomicInteger();                        // 包权限
    final AtomicInteger threadCountReject = new AtomicInteger();                        // 包权限
    final AtomicInteger concurrentReadCount = new AtomicInteger();                        // 包权限
    final AtomicInteger concurrentWriteCount = new AtomicInteger();                        // 包权限
    volatile TimesliceFlowControl writeFlowControl;                                                        // 包权限
    volatile TimesliceFlowControl readFlowControl;                                                         // 包权限

    /**
     * 写计数
     */
    // final AtomicInteger writeTimes = new AtomicInteger();//包权限
    final AtomicInteger writeTimesReject = new AtomicInteger();                        // 包权限

    /**
     * 读计数
     */
    // final AtomicInteger readTimes = new AtomicInteger();//包权限
    final AtomicInteger readTimesReject = new AtomicInteger();                        // 包权限
    volatile ConnectionProperties connectionProperties = new ConnectionProperties();                 // 包权限

    /**
     * 应用连接限制
     */
    private ConnRestrictor connRestrictor;

    // final private Timer timer = new Timer();
    // private volatile TimerTask timerTask = new TimerTaskC();

    protected TAtomDsConfDO runTimeConf;
    private static final Map<String, ExceptionSorter> exceptionSorters = new HashMap<String, ExceptionSorter>(2);

    static {
        exceptionSorters.put(TAtomDbTypeEnum.ORACLE.name(), new OracleExceptionSorter());
        exceptionSorters.put(TAtomDbTypeEnum.MYSQL.name(), new MySQLExceptionSorter());
    }

    private final ReentrantLock lock = new ReentrantLock();
    // private volatile boolean isNotAvailable = false; //是否不可用
    private volatile SmoothValve smoothValve = new SmoothValve(0);
    private volatile CountPunisher timeOutPunisher = new CountPunisher(new SmoothValve(0),
            3000,
            300);                                     // 3秒钟之内超时300次则惩罚，不可能的阀值，相当于关闭了

    private static final int default_retryBadDbInterval = 2000;                                       // milliseconds
    protected static int retryBadDbInterval;                                                      // milliseconds

    static {
        int interval = default_retryBadDbInterval;
        String propvalue = System.getProperty("com.taobao.tddl.DBSelector.retryBadDbInterval");
        if (propvalue != null) {
            try {
                interval = Integer.valueOf(propvalue.trim());
            } catch (Exception e) {
                logger.error("", e);
            }
        }
        retryBadDbInterval = interval;
    }

    public TAtomDbStatusEnum getDbStatus() {
        return connectionProperties.dbStatus;
    }

    public void setDbStatus(TAtomDbStatusEnum dbStatus) {
        this.connectionProperties.dbStatus = dbStatus;
    }

    public static class ConnectionProperties {

        public volatile TAtomDbStatusEnum dbStatus;
        /**
         * 当前数据库的名字
         */
        public volatile String datasourceName;

        // add by junyu,2012-4-17,日志统计使用
        public volatile String ip;

        public volatile String port;

        public volatile String realDbName;
        /**
         * 写次数限制，0为不限制
         */
        // public volatile int writeRestrictionTimes;

        /**
         * 读次数限制，0为不限制
         */
        // public volatile int readRestrictionTimes;
        /**
         * 线程count限制，0为不限制
         */
        public volatile int threadCountRestriction;

        /**
         * 允许并发读的最大个数，0为不限制
         */
        public volatile int maxConcurrentReadRestrict;

        /**
         * 允许并发写的最大个数，0为不限制
         */
        public volatile int maxConcurrentWriteRestrict;
    }

    public TDataSourceWrapper(DataSource targetDataSource, TAtomDsConfDO runTimeConf) {
        this.runTimeConf = runTimeConf;
        this.targetDataSource = targetDataSource;

        // timerTask = new TimerTaskC();
        // Monitor.addGlobalConfigListener(globalConfigListener);
        // timer.schedule(timerTask, 0,
        // this.connectionProperties.timeSliceInMillis);

        this.readFlowControl = new TimesliceFlowControl("读流量",
                runTimeConf.getTimeSliceInMillis(),
                runTimeConf.getReadRestrictTimes());
        this.writeFlowControl = new TimesliceFlowControl("写流量",
                runTimeConf.getTimeSliceInMillis(),
                runTimeConf.getWriteRestrictTimes());

        logger.warn("set thread count restrict " + runTimeConf.getThreadCountRestrict());
        this.connectionProperties.threadCountRestriction = runTimeConf.getThreadCountRestrict();

        // logger.warn("set write restrict times " +
        // runTimeConf.getWriteRestrictTimes());
        // this.connectionProperties.writeRestrictionTimes =
        // runTimeConf.getWriteRestrictTimes();

        // logger.warn("set read restrict times " +
        // runTimeConf.getReadRestrictTimes());
        // this.connectionProperties.readRestrictionTimes =
        // runTimeConf.getReadRestrictTimes();

        logger.warn("set maxConcurrentReadRestrict " + runTimeConf.getMaxConcurrentReadRestrict());
        this.connectionProperties.maxConcurrentReadRestrict = runTimeConf.getMaxConcurrentReadRestrict();

        logger.warn("set maxConcurrentWriteRestrict " + runTimeConf.getMaxConcurrentWriteRestrict());
        this.connectionProperties.maxConcurrentWriteRestrict = runTimeConf.getMaxConcurrentWriteRestrict();
    }

    public void init() {
        // changyuan.lh: 初始化连接分桶
        final String datasourceKey = connectionProperties.datasourceName;
        List<ConnRestrictEntry> connRestrictEntries = runTimeConf.getConnRestrictEntries();
        if (connRestrictEntries != null) {
            this.connRestrictor = new ConnRestrictor(datasourceKey, connRestrictEntries);
        }

        // timerTask = new TimerTaskC();
        // Monitor.addGlobalConfigListener(globalConfigListener);
        // timer.schedule(timerTask, 0,
        // this.connectionProperties.timeSliceInMillis);
    }

    // 包权限，给下游对象调用
    void countTimeOut() {
        timeOutPunisher.count();
    }

    private volatile long lastRetryTime = 0;

    @Override
    public Connection getConnection() throws SQLException {
        return getConnection(null, null);
    }

    /**
     * 这里只做了tryLock连接尝试，真正的逻辑委派给getConnection0
     */
    @Override
    public Connection getConnection(String username, String password) throws SQLException {
        SmoothValve valve = smoothValve;
        try {
            // modify by junyu,暂时去掉这个功能。
            // if (!runTimeConf.isSingleInGroup() && timeOutPunisher.punish()) {
            // //group里只剩一个时不做超时惩罚。再慢也得干活
            // throw new AtomSlowPunishException(this.runTimeConf.getDbName() +
            // "'s timeout " + timeOutPunisher); //超时惩罚
            // }
            if (valve.isNotAvailable()) {
                boolean toTry = System.currentTimeMillis() - lastRetryTime > retryBadDbInterval;
                if (toTry && lock.tryLock()) {
                    try {
                        Connection t = this.getConnection0(username, password); // 同一个时间只会有一个线程继续使用这个数据源。
                        // isNotAvailable = false; //用一个线程重试，执行成功则标记为可用，自动恢复
                        valve.setAvailable(); // 用一个线程重试，执行成功则标记为可用，自动恢复
                        return t;
                    } finally {
                        lastRetryTime = System.currentTimeMillis();
                        lock.unlock();
                    }
                } else {
                    throw new AtomNotAvailableException(this.runTimeConf.getDbName() + " isNotAvailable"); // 其他线程fail-fast
                }
            } else {
                if (valve.smoothThroughOnInitial()) {
                    return this.getConnection0(username, password);
                } else {
                    throw new AtomNotAvailableException(this.runTimeConf.getDbName()
                            + " squeezeThrough rejected on fatal reset"); // 未通过复位时的限流保护
                }
            }
        } catch (SQLException e) {
            ExceptionSorter exceptionSorter = exceptionSorters.get(StringUtils.upperCase(this.runTimeConf.getDbType()));
            if (exceptionSorter.isExceptionFatal(e)) {
                // isNotAvailable = true;
                valve.setNotAvailable();
            }

            throw new SQLException("get connection failed,dbKey is "
                    + (connectionProperties.datasourceName != null ? connectionProperties.datasourceName : this.runTimeConf.getDbName()),
                    e);
        }
    }

    private Connection getConnection0(String username, String password) throws SQLException {
        ConnRestrictSlot connRestrictSlot = null;
        TConnectionWrapper tconnectionWrapper;
        try {
            recordThreadCount();
            if (connRestrictor != null) {
                connRestrictSlot = connRestrictor.doRestrict(runTimeConf.getBlockingTimeout());
            }

            tconnectionWrapper = new TConnectionWrapper(getConnectionByTargetDataSource(username, password),
                    connRestrictSlot,
                    this);
        } catch (SQLException e) {
            if (connRestrictSlot != null) {
                connRestrictSlot.freeConnection();
            }
            threadCount.decrementAndGet();
            throw e;
        } catch (RuntimeException e) {
            if (connRestrictSlot != null) {
                connRestrictSlot.freeConnection();
            }
            threadCount.decrementAndGet();
            throw e;
        }
        return tconnectionWrapper;
    }

    private Connection getConnectionByTargetDataSource(String username, String password) throws SQLException {
        if (username == null && password == null) {
            return targetDataSource.getConnection();
        } else {
            return targetDataSource.getConnection(username, password);
        }
    }

    private void recordThreadCount() throws SQLException {
        int threadCountRestriction = connectionProperties.threadCountRestriction;
        int currentThreadCount = threadCount.incrementAndGet();
        if (threadCountRestriction != 0) {
            if (currentThreadCount > threadCountRestriction) {
                threadCountReject.incrementAndGet();
                throw new SQLException("max thread count : " + currentThreadCount);
            }
        }
    }

    /**
     * 设置
     *
     * @param datasourceName
     */
    public synchronized void setDatasourceName(String datasourceName) {
        this.connectionProperties.datasourceName = datasourceName;
    }

    public synchronized void setDatasourceIp(String ip) {
        this.connectionProperties.ip = ip;
    }

    public synchronized void setDatasourcePort(String port) {
        this.connectionProperties.port = port;
    }

    public synchronized void setDatasourceRealDbName(String realDbName) {
        this.connectionProperties.realDbName = realDbName;
    }

    /**
     * 设置时间片，在这个时候要重新制定计划。 bug fix : 以前没有重新制定schedule.导致这个设置是无效的
     *
     * @param timeSliceInMillis
     */
    public synchronized void setTimeSliceInMillis(int timeSliceInMillis) {
        if (timeSliceInMillis == 0) {
            logger.warn("timeSliceInMills is 0,return ");
        }
        /*
         * timerTask.cancel(); timer.purge(); timerTask = new TimerTaskC();
         * timer.schedule(timerTask, 0, timeSliceInMillis);
         */

        this.readFlowControl = new TimesliceFlowControl("读流量", timeSliceInMillis, runTimeConf.getReadRestrictTimes());
        this.writeFlowControl = new TimesliceFlowControl("写流量", timeSliceInMillis, runTimeConf.getWriteRestrictTimes());
        // this.connectionProperties.timeSliceInMillis = timeSliceInMillis;
    }

    /*
     * public ConnectionProperties getConnectionProperties() { return
     * connectionProperties; } public synchronized void
     * setConnectionProperties(ConnectionProperties connectionProperties) {
     * this.connectionProperties = connectionProperties; }
     */

    /*
     * private volatile Values lastReadWriteSnapshot = new Values(); private
     * class TimerTaskC extends TimerTask {
     * @Override public void run() { lastReadWriteSnapshot = new Values();
     * lastReadWriteSnapshot.value1.set(readTimes.longValue());
     * lastReadWriteSnapshot.value2.set(writeTimes.longValue());
     * readTimes.set(0); writeTimes.set(0); } } private
     * SnapshotValuesOutputCallBack snapshotValuesOutputCallBack = new
     * SnapshotValuesOutputCallBack() {
     * @Override public ConcurrentHashMap<String, Values> getValues() {
     * ConcurrentHashMap<String, Values> concurrentHashMap = new
     * ConcurrentHashMap<String, Values>(); String prefix =
     * connectionProperties.datasourceName + "_"; // 添加threadCount Values
     * threadCountValues = new Values();
     * threadCountValues.value1.set(threadCount.longValue());
     * threadCountValues.value2
     * .set(connectionProperties.threadCountRestriction);
     * concurrentHashMap.put(prefix + Key.THREAD_COUNT, threadCountValues);
     * //添加读写拒绝次数 Values rejectCountValues = new Values();
     * rejectCountValues.value1.set(readTimesReject.longValue());
     * rejectCountValues.value2.set(writeTimesReject.longValue());
     * concurrentHashMap.put(prefix + Key.READ_WRITE_TIMES_REJECT_COUNT,
     * rejectCountValues); // 添加读写count concurrentHashMap.put(prefix +
     * Key.READ_WRITE_TIMES, lastReadWriteSnapshot); //添加读写并发次数 Values
     * rwConcurrent = new Values();
     * rwConcurrent.value1.set(concurrentReadCount.longValue());
     * rwConcurrent.value2.set(concurrentWriteCount.longValue());
     * concurrentHashMap.put(prefix + Key.READ_WRITE_CONCURRENT, rwConcurrent);
     * return concurrentHashMap; } }; private GlobalConfigListener
     * globalConfigListener = new GlobalConfigListener() { public void
     * onConfigReceive(Properties p) { for (Map.Entry<Object, Object> entry :
     * p.entrySet()) { String key = ((String) entry.getKey()).trim(); String
     * value = ((String) entry.getValue()).trim(); switch
     * (TDDLConfigKey.valueOf(key)) { case SmoothValveProperties: { SmoothValve
     * old = smoothValve; SmoothValve nnn = SmoothValve.parse(value); if (nnn !=
     * null) { logger.warn("smoothValve switch from [" + old + "] to [" + nnn +
     * "]"); smoothValve = nnn; } break; } case CountPunisherProperties: {
     * CountPunisher old = timeOutPunisher; CountPunisher nnn =
     * CountPunisher.parse(smoothValve, value); if (nnn != null) {
     * logger.warn("timeOutPunisher switch from [" + old + "] to [" + nnn +
     * "]"); timeOutPunisher = nnn; } break; } default: break; } } } }; public
     * void destroy() {
     * Monitor.removeSnapshotValuesCallback(snapshotValuesOutputCallBack);
     * Monitor.removeGlobalConfigListener(globalConfigListener); }
     */

    /*
     * ========================================================================
     * ===== jdbc接口方法，简单委派给targetDataSource
     * ======================================================================
     */

    @Override
    public PrintWriter getLogWriter() throws SQLException {
        return targetDataSource.getLogWriter();
    }

    @Override
    public void setLogWriter(PrintWriter out) throws SQLException {
        targetDataSource.setLogWriter(out);
    }

    @Override
    public void setLoginTimeout(int seconds) throws SQLException {
        targetDataSource.setLoginTimeout(seconds);
    }

    @Override
    public int getLoginTimeout() throws SQLException {
        return targetDataSource.getLoginTimeout();
    }

    @Override
    public Logger getParentLogger() throws SQLFeatureNotSupportedException {
        return null;
    }

    /**
     * jdk1.6 新增接口
     */
    @Override
    @SuppressWarnings("unchecked")
    public <T> T unwrap(Class<T> iface) throws SQLException {
        if (isWrapperFor(iface)) {
            return (T) this;
        } else {
            throw new SQLException("not a wrapper for " + iface);
        }
    }

    @Override
    public boolean isWrapperFor(Class<?> iface) throws SQLException {
        return TDataSourceWrapper.class.isAssignableFrom(iface);
    }
}