package com.vita.log.service;

import com.vita.log.model.OperLogRecord;
import com.vita.manager.factory.AsyncFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;

/**
 * 系统操作日志异步执行器，负责提交并兜底执行异步日志任务。
 */
@Component
public class OperLogAsyncExecutor {

    private final Executor operLogTaskExecutor;

    private final ObjectProvider<OperLogPersistence> operLogPersistenceProvider;

    public OperLogAsyncExecutor(@Qualifier("operLogTaskExecutor") Executor operLogTaskExecutor,
                                ObjectProvider<OperLogPersistence> operLogPersistenceProvider) {
        this.operLogTaskExecutor = operLogTaskExecutor;
        this.operLogPersistenceProvider = operLogPersistenceProvider;
    }

    /**
     * 提交系统操作日志任务。
     *
     * @param requestId 请求ID
     * @param record    操作日志快照
     */
    public void submit(String requestId, OperLogRecord record) {
        if (record == null) {
            return;
        }

        OperLogPersistence operLogPersistence = operLogPersistenceProvider.getIfAvailable();
        Runnable task = AsyncFactory.recordOper(requestId, snapshot(record), operLogPersistence);
        try {
            operLogTaskExecutor.execute(task);
        } catch (RejectedExecutionException ex) {
            task.run();
        }
    }

    private OperLogRecord snapshot(OperLogRecord source) {
        OperLogRecord target = new OperLogRecord();
        BeanUtils.copyProperties(source, target);
        return target;
    }
}
