package com.vita.manager.task;

/**
 * 系统级异步任务可选的执行器类型。
 * @author znk
 */
public enum SystemAsyncTaskExecutorType {

    /**
     * 系统操作日志专用执行器。
     */
    OPER_AUDIT,

    /**
     * 登录审计与登录信息更新执行器。
     */
    LOGIN_AUDIT,

    /**
     * 登录地理位置补全执行器。
     */
    GEO_ENRICH,

    /**
     * 通用调度执行器。
     */
    SCHEDULED
}
