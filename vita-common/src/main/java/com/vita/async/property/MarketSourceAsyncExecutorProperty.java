package com.vita.async.property;

/**
 * 九个固定指数的数据源并发请求线程池属性。
 */
public class MarketSourceAsyncExecutorProperty extends AsyncExecutorProperty {

    public MarketSourceAsyncExecutorProperty() {
        super(9, 9, 32, 60, "market-source-", 30);
    }
}
