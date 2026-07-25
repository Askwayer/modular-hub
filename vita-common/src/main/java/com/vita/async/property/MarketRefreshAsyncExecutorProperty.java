package com.vita.async.property;

/**
 * @BelongsProject: be-vita
 * @BelongsPackage: com.vita.async.property
 * @Author: znk
 * @CreateTime: 2026-07-25
 * @Description: 行情模块并行刷新线程池属性
 * @Version: 1.0
 */
public class MarketRefreshAsyncExecutorProperty extends AsyncExecutorProperty {

    public MarketRefreshAsyncExecutorProperty() {
        super(4, 6, 20, 60, "market-refresh-", 30);
    }
}
