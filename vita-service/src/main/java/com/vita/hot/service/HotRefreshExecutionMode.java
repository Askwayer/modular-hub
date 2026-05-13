package com.vita.hot.service;

/**
 * 热榜刷新执行模式。
 */
public enum HotRefreshExecutionMode {

    REMOTE,

    CACHE_FALLBACK,

    SKIPPED
}
