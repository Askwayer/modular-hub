package com.vita.hot.constant;

import java.util.List;
import java.util.Map;

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
    public static final String HOT_REFRESH_LOCK_KEY_PREFIX = "hot:lock:refresh:";
    public static final long HOT_REFRESH_LOCK_LEASE_SECONDS = 300L;
    public static final String DEFAULT_REFRESH_CRON = "0 0/10 * * * ?";
    public static final long DEFAULT_STALE_THRESHOLD_MINUTES = 30L;
    public static final List<String> DEFAULT_ENABLED_SOURCES = List.of(
            "weibo",
            "zhihu",
            "baidu",
            "bilibili",
            "juejin",
            "ithome",
            "36kr",
            "sspai"
    );
    public static final Map<String, String> SOURCE_NAME_MAPPING = Map.of(
            "weibo", "微博热搜",
            "zhihu", "知乎热榜",
            "baidu", "百度热搜",
            "bilibili", "哔哩哔哩",
            "juejin", "稀土掘金",
            "ithome", "IT之家",
            "36kr", "36氪",
            "sspai", "少数派"
    );

    public static String buildSourceCacheKey(String sourceCode) {
        return HOT_CACHE_KEY_PREFIX + sourceCode;
    }

    public static String buildRefreshLockKey(String sourceCode) {
        return HOT_REFRESH_LOCK_KEY_PREFIX + sourceCode;
    }
}
