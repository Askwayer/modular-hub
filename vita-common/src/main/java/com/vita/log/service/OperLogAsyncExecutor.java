package com.vita.log.service;

import com.vita.log.model.OperLogRecord;
import com.vita.manager.AsyncManager;
import com.vita.manager.factory.AsyncFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

/**
 * 系统操作日志场景执行器，负责日志快照收口、依赖解析与统一任务投递。
 *
 * @author zhuningkang
 */
@Component
public class OperLogAsyncExecutor {

    private final AsyncManager asyncManager;

    private final ObjectProvider<OperLogPersistence> operLogPersistenceProvider;

    public OperLogAsyncExecutor(AsyncManager asyncManager,
                                ObjectProvider<OperLogPersistence> operLogPersistenceProvider) {
        this.asyncManager = asyncManager;
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
        asyncManager.execute(AsyncFactory.recordOper(requestId, snapshot(record), operLogPersistence));
    }

    /**
     * 创建操作日志记录的快照，避免异步执行过程中数据被修改。
     *
     * @param source 日志记录源对象
     * @return 日志记录快照
     */
    private OperLogRecord snapshot(OperLogRecord source) {
        OperLogRecord target = new OperLogRecord();
        BeanUtils.copyProperties(source, target);
        return target;
    }
}
