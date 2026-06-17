package com.vita.hot.constant;

/**
 * @BelongsProject: be-vita
 * @BelongsPackage: com.vita.hot.constant
 * @Author: Codex
 * @CreateTime: 2026-05-11
 * @Description: 热榜业务常量
 * @Version: 1.0
 */
public final class HotConstants {

    private HotConstants() {
        // 常量类不允许实例化
    }

    /**
     * 单源热榜缓存 Key 前缀。
     */
    public static final String HOT_CACHE_KEY_PREFIX = "hot:source:";

    /**
     * 源列表缓存 Key。
     */
    public static final String HOT_SOURCE_META_KEY = "hot:meta:sources";

    /**
     * 单源刷新锁 Key 前缀。
     */
    public static final String HOT_REFRESH_LOCK_KEY_PREFIX = "hot:lock:refresh:";

    /**
     * 全量刷新锁 Key。
     */
    public static final String HOT_REFRESH_ALL_LOCK_KEY = "hot:lock:refresh:all";

    /**
     * 单源热榜缓存 TTL，单位：分钟。
     */
    public static final long HOT_CACHE_TTL_MINUTES = 20L;

    /**
     * 源列表缓存 TTL，单位：分钟。
     */
    public static final long HOT_SOURCE_META_TTL_MINUTES = 1440L;

    /**
     * 刷新锁租约时间，单位：秒。
     */
    public static final long HOT_REFRESH_LOCK_LEASE_SECONDS = 300L;

    /**
     * 默认缓存过期判断阈值，单位：分钟。
     */
    public static final long DEFAULT_STALE_THRESHOLD_MINUTES = 20L;

    public static String buildSourceCacheKey(String sourceCode) {
        return HOT_CACHE_KEY_PREFIX + sourceCode;
    }

    public static String buildRefreshLockKey(String sourceCode) {
        return HOT_REFRESH_LOCK_KEY_PREFIX + sourceCode;
    }
}
