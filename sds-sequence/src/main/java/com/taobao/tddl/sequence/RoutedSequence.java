package com.taobao.tddl.sequence;

import com.taobao.tddl.sequence.exception.SequenceException;

/**
 * 带路由信息序列接口
 *
 * @param <DatabaseRouteType> 数据库路由信息类型
 * @param <TableRouteType>    表路由信息类型
 * @author guangxia
 */
public interface RoutedSequence<DatabaseRouteType, TableRouteType> {

    /**
     * 取得序列下一个值
     *
     * @param databaseRoute 数据库路由信息
     * @param tableRoute    表路由信息
     * @return 返回序列下一个值
     * @throws com.taobao.tddl.sequence.exception.SequenceException
     */
    long nextValue(DatabaseRouteType databaseRoute, TableRouteType tableRoute) throws SequenceException;
}
