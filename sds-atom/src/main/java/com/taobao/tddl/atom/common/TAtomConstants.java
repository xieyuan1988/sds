package com.taobao.tddl.atom.common;

import com.alibaba.druid.util.StringUtils;

import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Map;

/**
 * TAtom数据源的常量设置类
 *
 * @author qihao
 */
public class TAtomConstants {

    public final static String DEFAULT_DIAMOND_GROUP = null;

    public final static String DEFAULT_MYSQL_CHAR_SET = "gbk";

    // public final static String ORACLE_DBTYPE_STR = "ORACLE";

    // public final static String MYSQL_DBTYPE_STR = "MYSQL";

    public final static String DEFAULT_ORACLE_CON_TYPE = "oci";

    public final static String DB_STATUS_R = "R";

    public final static String DB_STATUS_W = "W";

    public final static String DB_STATUS_RW = "RW";

    public final static String DB_STATUS_NA = "NA";

    public static Map<String, String> DEFAULT_ORACLE_CONNECTION_PROPERTIES = new HashMap<String, String>(2);

    static {
        TAtomConstants.DEFAULT_ORACLE_CONNECTION_PROPERTIES.put("SetBigStringTryClob", "true");
        TAtomConstants.DEFAULT_ORACLE_CONNECTION_PROPERTIES.put("defaultRowPrefetch", "50");
    }

    public static Map<String, String> DEFAULT_MYSQL_CONNECTION_PROPERTIES = new HashMap<String, String>(1);

    static {
        TAtomConstants.DEFAULT_MYSQL_CONNECTION_PROPERTIES.put("characterEncoding", "gbk");
    }

    public final static String DEFAULT_ORACLE_DRIVER_CLASS = "oracle.jdbc.driver.OracleDriver";

    public final static String DEFAULT_MYSQL_DRIVER_CLASS = "com.mysql.jdbc.Driver";

    public final static String DEFAULT_DRUID_ORACLE_SORTER_CLASS = "com.alibaba.druid.pool.vendor.OracleExceptionSorter";

    public final static String DEFAULT_DRUID_MYSQL_SORTER_CLASS = "com.alibaba.druid.pool.vendor.MySqlExceptionSorter";

    public final static String DRUID_MYSQL_INTEGRATION_SORTER_CLASS = "com.alibaba.druid.pool.vendor.NullExceptionSorter";

    public final static String DEFAULT_DRUID_MYSQL_VALID_CONNECTION_CHECKERCLASS = "com.alibaba.druid.pool.vendor.MySqlValidConnectionChecker";

    public final static String DEFAULT_DRUID_ORACLE_VALIDATION_QUERY = "select 'x' from dual";

    public final static String DEFAULT_DRUID_MYSQL_VALIDATION_QUERY = "select 'x'";

    /**
     * 全局配置dataId模板
     */
    private static MessageFormat GLOBAL_FORMAT = new MessageFormat("com.taobao.tddl.atom.global.{0}");

    /**
     * 应用配置dataId模板
     */
    private static MessageFormat APP_FORMAT = new MessageFormat("com.taobao.tddl.atom.app.{0}.{1}");

    private static MessageFormat PASSWD_FORMAT = new MessageFormat("com.taobao.tddl.atom.passwd.{0}.{1}.{2}");

    /**
     * dbName模板
     */
    private static final MessageFormat DB_NAME_FORMAT = new MessageFormat("atom.dbkey.{0}^{1}^{2}");
    private static final String NULL_UNIT_NAME = "DEFAULT_UNIT";

    /**
     * 根据dbKey获取全局配置dataId
     *
     * @param dbKey 数据库名KEY
     * @return
     */
    public static String getGlobalDataId(String dbKey) {
        return GLOBAL_FORMAT.format(new Object[]{dbKey});
    }

    /**
     * 根据应用名和dbKey获取指定的应用配置dataId
     *
     * @param appName
     * @param dbKey
     * @return
     */
    public static String getAppDataId(String appName, String dbKey) {
        return APP_FORMAT.format(new Object[]{appName, dbKey});
    }

    /**
     * 根据dbKey和userName获得对应的passwd的dataId
     *
     * @param
     * @param userName
     * @return
     */
    public static String getPasswdDataId(String dbName, String dbType, String userName) {
        return PASSWD_FORMAT.format(new Object[]{dbName, dbType, userName});
    }

    /**
     * @param unitName
     * @param appName
     * @param dbkey
     * @return
     */
    public static String getDbNameStr(String unitName, String appName, String dbkey) {
        if (StringUtils.isEmpty(unitName)) {
            return DB_NAME_FORMAT.format(new Object[]{NULL_UNIT_NAME, appName, dbkey});
        } else {
            return DB_NAME_FORMAT.format(new Object[]{appName, dbkey});
        }
    }
}
