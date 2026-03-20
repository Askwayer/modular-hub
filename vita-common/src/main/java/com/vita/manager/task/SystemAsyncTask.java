package com.vita.manager.task;

import lombok.Getter;

import java.util.Objects;

/**
 * 系统级异步任务定义，统一描述任务名称、执行器类型、任务体和延迟参数。
 */
@Getter
public final class SystemAsyncTask {

    private final String taskName;

    private final SystemAsyncTaskExecutorType executorType;

    private final Runnable task;

    private final long delayMillis;

    private SystemAsyncTask(String taskName,
                            SystemAsyncTaskExecutorType executorType,
                            Runnable task,
                            long delayMillis) {
        this.taskName = taskName == null || taskName.isBlank() ? "unknown-system-task" : taskName;
        this.executorType = Objects.requireNonNull(executorType, "executorType must not be null");
        this.task = Objects.requireNonNull(task, "task must not be null");
        this.delayMillis = Math.max(0L, delayMillis);
    }

    /**
     * 创建立即执行的系统异步任务。
     *
     * @param taskName     任务名称
     * @param executorType 执行器类型
     * @param task         任务体
     * @return 系统异步任务
     */
    public static SystemAsyncTask immediate(String taskName,
                                            SystemAsyncTaskExecutorType executorType,
                                            Runnable task) {
        return new SystemAsyncTask(taskName, executorType, task, 0L);
    }

    /**
     * 创建延迟执行的系统异步任务。
     *
     * @param taskName     任务名称
     * @param executorType 执行器类型
     * @param task         任务体
     * @param delayMillis  延迟毫秒数
     * @return 系统异步任务
     */
    public static SystemAsyncTask delayed(String taskName,
                                          SystemAsyncTaskExecutorType executorType,
                                          Runnable task,
                                          long delayMillis) {
        return new SystemAsyncTask(taskName, executorType, task, delayMillis);
    }

    public boolean hasDelay() {
        return delayMillis > 0;
    }

    /**
     * 返回一个等价的立即执行任务，用于调度器延迟结束后继续路由。
     *
     * @return 无延迟的系统异步任务
     */
    public SystemAsyncTask withoutDelay() {
        if (!hasDelay()) {
            return this;
        }
        return new SystemAsyncTask(taskName, executorType, task, 0L);
    }
}
