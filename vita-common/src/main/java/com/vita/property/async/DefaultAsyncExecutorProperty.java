package com.vita.property.async;

/**
 * 非核心异步默认线程池预置参数。
 *
 * @author Codex
 */
public class DefaultAsyncExecutorProperty extends AsyncExecutorProperty {

    public DefaultAsyncExecutorProperty() {
        super(50, 200, 1000, 300, "default-async-", 30);
    }
}
