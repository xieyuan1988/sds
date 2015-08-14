package com.taobao.tddl.common.utils;

import com.google.common.cache.RemovalListener;
import com.google.common.cache.RemovalNotification;
import org.junit.Test;

public class GoogleConcurrentLruCacheTest {

    @Test
    public void testSimple() {

        GoogleConcurrentLruCache cache = new GoogleConcurrentLruCache(10, new RemovalListener() {
            @Override
            public void onRemoval(RemovalNotification notification) {
                System.out.println("evict key:" + notification.getKey() + " values:" + notification.getValue());
            }
        });


        for (int i = 0; i < 11; i++) {
            cache.put("key" + i, "value" + i);
        }
    }
}
