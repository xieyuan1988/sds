package com.taobao.tddl.common.utils;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.RemovalListener;

import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 基于google concurrent的LinkedHashMap实现LRU cache
 *
 * @author jianghang 2013-10-23 下午5:02:50
 * @since 5.0.0
 */
public class GoogleConcurrentLruCache<K, V> {

    private Cache<K, V> cache;
    private static final int DEFAULT_CAPACITY = 389;
    public static final int DEFAULT_CONCURRENCY_LEVEL = 64;
    private AtomicLong get = new AtomicLong(0);
    private AtomicLong hit = new AtomicLong(0);

    private int capacity;

    public GoogleConcurrentLruCache() {
        this(DEFAULT_CAPACITY);
    }

    public GoogleConcurrentLruCache(int capacity) {
        this.capacity = (capacity <= 0) ? DEFAULT_CAPACITY : capacity;

        cache = CacheBuilder.newBuilder()
                .concurrencyLevel(DEFAULT_CONCURRENCY_LEVEL)
                .maximumSize(this.capacity)
                .build();
    }

    public GoogleConcurrentLruCache(int capacity, RemovalListener<K, V> listener) {
        this.capacity = (capacity <= 0) ? DEFAULT_CAPACITY : capacity;

        cache = CacheBuilder.newBuilder()
                .concurrencyLevel(DEFAULT_CONCURRENCY_LEVEL)
                .maximumSize(this.capacity)
                .removalListener(listener)
                .build();
    }

    public long capacity() {
        return this.capacity;
    }

    public boolean isEmpty() {
        return cache.size() <= 0;
    }

    public int size() {
        return (int)cache.size();
    }

    public void clear() {
        cache.invalidateAll();
    }

    public V get(K key) {
        V v = cache.getIfPresent(key);
        get.addAndGet(1);
        if (v != null) {
            hit.addAndGet(1);
        }

        return v;
    }

    public void put(K key, V value) {
        cache.put(key, value);
    }

    public void remove(K key) {
        cache.invalidate(key);
    }

    public ConcurrentMap<K, V> asMap() {
        return cache.asMap();
    }

    public boolean containsKey(K key) {
        return cache.getIfPresent(key) != null;
    }

    public String getStatus() {
        StringBuilder sb = new StringBuilder();
        sb.append("current size:");
        sb.append(cache.size());
        sb.append(" get:");
        sb.append(get);
        sb.append(" hit:");
        sb.append(hit);
        sb.append(" hit ratio:");
        if (get.longValue() > 0) {
            sb.append((hit.doubleValue() / get.doubleValue()) * 100);
            sb.append("%");
        } else {
            sb.append("--");
        }

        get.set(0);
        hit.set(0);

        return sb.toString();
    }
}
