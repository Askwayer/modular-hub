package com.vita.manager;

import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * @BelongsProject: be-vita
 * @BelongsPackage: com.vita.manager
 * @Author: znk
 * @CreateTime: 2026-03-19  15:35:43
 * @Description: 确保系统异步任务调度器在应用退出时完成必要的关闭动作
 * @Version: 1.0
 */
@Component
public class ShutdownManager {

    private static final Logger LOG = LoggerFactory.getLogger(ShutdownManager.class);

    private final AsyncManager asyncManager;

    public ShutdownManager(AsyncManager asyncManager) {
        this.asyncManager = asyncManager;
    }

    @PreDestroy
    public void destroy() {
        shutdownAsyncManager();
    }

    /**
     * 停止异步执行任务
     */
    private void shutdownAsyncManager() {
        try {
            LOG.info("====关闭系统异步任务调度器====");
            asyncManager.shutdown();
        } catch (Exception e) {
            LOG.error("关闭系统异步任务调度器失败", e);
        }
    }
}
