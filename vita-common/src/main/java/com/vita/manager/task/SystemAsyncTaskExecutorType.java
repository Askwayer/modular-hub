package com.vita.manager.task;

/**
 * 系统级异步任务可选的执行器类型。
 * @author zhuningkang
 */
public enum SystemAsyncTaskExecutorType {

    /**
     * 系统操作日志专用执行器。
     */
    OPER_LOG,

    /**
     * 通用调度执行器。
     */
    SCHEDULED
}
