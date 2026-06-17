package com.vita.property.async;

/**
 * 热榜全量刷新线程池预置参数。
 *
 * @author Codex
 */
public class HotRefreshAsyncExecutorProperty extends AsyncExecutorProperty {

    public HotRefreshAsyncExecutorProperty() {
        super(6, 6, 100, 300, "hot-refresh-", 30);
    }
}
