package com.taobao.tddl.atom.utils;

import java.util.Arrays;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

/**
 * 实现应用连接数限制功能中, 具体某一个槽 (Slot) 的连接数限制。
 *
 * @author changyuan.lh
 */
public final class ConnRestrictSlot {

    private final ConnRestrictEntry entry;

    /**
     * 直接用 信号量, 跟锁一样都是基于 AbstractQueuedSynchronizer, 性能应该 没有问题, 就是不能动态改
     * permits。但是现在的推送机制是直接丢掉旧的 TDataSourceWrapper 换个新的: 旧的连接还到旧的 Slot, 新的申请走新
     * 建的 Slot, 所以看来没有动态的必要。
     */
    private final Semaphore semaphore;

    public ConnRestrictSlot(String datasourceKey, String slotKey, ConnRestrictEntry entry) {
        this.semaphore = new Semaphore(entry.limits); // Nofair, 带 Spin 性能好一些
        this.entry = entry;
    }

    public boolean allocateConnection(final int timeoutInMillis) throws InterruptedException {
        return semaphore.tryAcquire(timeoutInMillis, TimeUnit.MILLISECONDS);
    }

    public int getAvailableConnections() {
        return semaphore.availablePermits();
    }

    public int getConnections() {
        return entry.limits - semaphore.availablePermits();
    }

    public int getLimits() {
        return entry.limits;
    }

    public void freeConnection() {
        semaphore.release();
    }

    public String toString() {
        return "ConnRestrictSlot: @" + Integer.toHexString(hashCode()) + " " + Arrays.toString(entry.keys) + " "
                + entry.limits;
    }
}
