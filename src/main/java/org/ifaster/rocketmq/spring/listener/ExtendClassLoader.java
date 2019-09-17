package org.ifaster.rocketmq.spring.listener;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * spi方式加载类
 * @author yangnan
 */
public final class ExtendClassLoader {

    /**
     * 缓存集合数据
     */
    private static ConcurrentMap<Class<?>, List> CACHE_MAP = new ConcurrentHashMap<>();

    /**
     * 获取listener
     *
     * @param <T>
     * @return
     */
    public static <T> List<T> getListener(Class<T> tClass) {
        List<T> ts = CACHE_MAP.get(tClass);
        if (ts != null) {
            return ts;
        }
        synchronized (tClass) {
            ts = CACHE_MAP.get(tClass);
            if (ts == null) {
                ts = new ArrayList<>();
                Iterator<T> iterator = ServiceLoader.load(tClass).iterator();
                while (iterator.hasNext()) {
                    T t = iterator.next();
                    ts.add(t);
                }
                CACHE_MAP.putIfAbsent(tClass, Collections.unmodifiableList(ts));
            }
        }
        return ts;
    }
}
