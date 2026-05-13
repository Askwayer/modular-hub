package com.vita.hot.service;

import com.vita.hot.vo.HotSourceStatusVo;

import java.util.List;

/**
 * 热榜刷新执行结果。
 */
public record HotRefreshExecutionResult(
        HotRefreshExecutionMode executionMode,
        List<HotSourceStatusVo> statusList
) {

    public static HotRefreshExecutionResult remote(List<HotSourceStatusVo> statusList) {
        return new HotRefreshExecutionResult(HotRefreshExecutionMode.REMOTE, statusList);
    }

    public static HotRefreshExecutionResult cacheFallback(List<HotSourceStatusVo> statusList) {
        return new HotRefreshExecutionResult(HotRefreshExecutionMode.CACHE_FALLBACK, statusList);
    }

    public static HotRefreshExecutionResult skipped() {
        return new HotRefreshExecutionResult(HotRefreshExecutionMode.SKIPPED, List.of());
    }
}
