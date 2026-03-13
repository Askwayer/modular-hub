package com.vita.log.service;

import com.vita.log.model.OperLogRecord;

/**
 * @version 1.0
 * @BelongsProject: vita
 * @BelongsPackage: com.vita.log.model
 * @Author znk
 * @CreateTime 2026-03-12
 * @Description: 审计日志持久化扩展点，由上层模块提供具体实现。
 */
public interface OperLogPersistence {

    /**
     * 持久化审计日志。
     *
     * @param record 审计日志记录
     */
    void persist(OperLogRecord record);
}
