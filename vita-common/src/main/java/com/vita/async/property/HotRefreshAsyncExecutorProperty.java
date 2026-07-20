package com.vita.async.property;

/**
 * @BelongsProject: be-vita
 * @BelongsPackage: com.vita.async.property
 * @Author: znk
 * @CreateTime: 2026-07-10  16:40:48
 * @Description: 热刷新线程池属性配置
 * @Version: 1.0
 */
public class HotRefreshAsyncExecutorProperty extends AsyncExecutorProperty {

    public HotRefreshAsyncExecutorProperty() {
        super(6, 6, 100, 300, "hot-refresh-", 30);
    }
}
