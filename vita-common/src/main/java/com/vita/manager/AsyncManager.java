package com.vita.manager;

import com.vita.manager.task.SystemAsyncTask;
import com.vita.manager.task.SystemAsyncTaskExecutorType;
import com.vita.utils.ThreadUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * @BelongsProject: be-vita
 * @BelongsPackage: com.vita.manager
 * @Author: znk
 * @CreateTime: 2026-03-19  15:34:07
 * @Description: 系统级异步任务协调器，统一负责系统任务的路由、调度与兜底
 * @Version: 1.0
 */
@Component
public class AsyncManager {

    private static final Logger LOG = LoggerFactory.getLogger(AsyncManager.class);

    private final ScheduledExecutorService scheduledExecutorService;

    private final Executor operLogTaskExecutor;

    public AsyncManager(@Qualifier("scheduledExecutorService") ScheduledExecutorService scheduledExecutorService,
                        @Qualifier("operLogTaskExecutor") Executor operLogTaskExecutor) {
        this.scheduledExecutorService = scheduledExecutorService;
        this.operLogTaskExecutor = operLogTaskExecutor;
    }

    /**
     * 执行系统级异步任务。
     *
     * @param task 系统异步任务定义
     */
    public void execute(SystemAsyncTask task) {
        if (task == null) {
            return;
        }

        if (task.hasDelay()) {
            schedule(task);
            return;
        }

        dispatch(task);
    }

    /**
     * 停止需要手动关闭的系统异步调度线程池。
     */
    public void shutdown() {
        ThreadUtils.shutdownAndAwaitTermination(scheduledExecutorService);
    }

    private void schedule(SystemAsyncTask task) {
        try {
            scheduledExecutorService.schedule(
                    () -> dispatch(task.withoutDelay()),
                    task.getDelayMillis(),
                    TimeUnit.MILLISECONDS
            );
        } catch (RejectedExecutionException ex) {
            fallback(task, ex);
        }
    }

    private void dispatch(SystemAsyncTask task) {
        Executor executor = resolveExecutor(task.getExecutorType());
        try {
            executor.execute(task.getTask());
        } catch (RejectedExecutionException ex) {
            fallback(task, ex);
        }
    }

    private Executor resolveExecutor(SystemAsyncTaskExecutorType executorType) {
        return switch (executorType) {
            case OPER_LOG -> operLogTaskExecutor;
            case SCHEDULED -> scheduledExecutorService;
        };
    }

    private void fallback(SystemAsyncTask task, RejectedExecutionException ex) {
        LOG.warn(
                "系统异步任务提交失败，回退到调用线程执行, taskName={}, executorType={}",
                task.getTaskName(),
                task.getExecutorType(),
                ex
        );
        task.getTask().run();
    }
}
