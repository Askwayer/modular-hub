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

    public static final String HOT_CACHE_KEY_PREFIX = "hot:source:";
    public static final String HOT_SOURCE_REGISTRY_CACHE_KEY = "hot:meta:sources";
    public static final String HOT_REFRESH_LOCK_KEY_PREFIX = "hot:lock:refresh:";
    public static final long HOT_CACHE_TTL_MINUTES = 20L;
    public static final long DEFAULT_REGISTRY_CACHE_TTL_MINUTES = 24 * 60L;
    public static final long HOT_REFRESH_LOCK_LEASE_SECONDS = 300L;
    public static final long DEFAULT_STALE_THRESHOLD_MINUTES = 20L;
    public static final int DEFAULT_REFRESH_PARALLELISM = 6;

    public static String buildSourceCacheKey(String sourceCode) {
        return HOT_CACHE_KEY_PREFIX + sourceCode;
    }

    public static String buildRefreshLockKey(String sourceCode) {
        return HOT_REFRESH_LOCK_KEY_PREFIX + sourceCode;
    }
}
