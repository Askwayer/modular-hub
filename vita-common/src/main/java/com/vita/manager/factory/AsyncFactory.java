package com.vita.manager.factory;

import com.vita.log.model.OperLogRecord;
import com.vita.log.service.OperLogPersistence;
import com.vita.interceptor.RequestTraceInterceptor;
import com.vita.manager.task.SystemAsyncTask;
import com.vita.manager.task.SystemAsyncTaskExecutorType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

/**
 * @BelongsProject: be-vita
 * @BelongsPackage: com.vita.manager.factory
 * @Author: znk
 * @CreateTime: 2026-03-19  15:40:19
 * @Description: 系统异步任务工厂，负责生成系统级异步任务定义
 * @Version: 1.0
 */
public class AsyncFactory {

    private static final String SYSTEM_OPER_LOG_TASK_NAME = "system-oper-log";

    private static final Logger ACCESS_LOGGER = LoggerFactory.getLogger("sys-access");

    private static final Logger LOG = LoggerFactory.getLogger(AsyncFactory.class);

    private AsyncFactory() {
    }

    /**
     * 操作日志记录
     *
     * @param requestId 请求ID
     * @param operLog 操作日志信息
     * @param operLogPersistence 操作日志持久化器
     * @return 系统操作日志异步任务定义
     */
    public static SystemAsyncTask recordOper(final String requestId,
                                             final OperLogRecord operLog,
                                             final OperLogPersistence operLogPersistence) {
        return SystemAsyncTask.immediate(
                SYSTEM_OPER_LOG_TASK_NAME,
                SystemAsyncTaskExecutorType.OPER_LOG,
                () -> {
                    try {
                        if (requestId != null) {
                            MDC.put(RequestTraceInterceptor.REQUEST_ID_KEY, requestId);
                        }
                        ACCESS_LOGGER.info(
                                "requestId={} module={} businessType={} status={} requestMethod={} operUrl={} operIp={} operUserId={} operName={} costTime={} errorMsg={}",
                                requestId,
                                operLog.getModule(),
                                operLog.getBusinessType(),
                                operLog.getStatus(),
                                operLog.getRequestMethod(),
                                operLog.getOperUrl(),
                                operLog.getOperIp(),
                                operLog.getOperUserId(),
                                operLog.getOperName(),
                                operLog.getCostTime(),
                                operLog.getErrorMsg()
                        );
                        if (operLogPersistence != null) {
                            operLogPersistence.persist(operLog);
                        }
                    } catch (Exception ex) {
                        LOG.error("异步保存系统操作日志失败, requestId={}", requestId, ex);
                    } finally {
                        MDC.remove(RequestTraceInterceptor.REQUEST_ID_KEY);
                    }
                }
        );
    }
}
