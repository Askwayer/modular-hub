package com.vita.log.persistence;

import com.vita.log.model.OperLogRecord;

/**
 * 操作日志持久化扩展点，由上层模块提供具体实现。
 *
 * @author znk
 */
public interface OperLogPersistence {

    /**
     * 持久化审计日志。
     *
     * @param record 审计日志记录
     */
    void persist(OperLogRecord record);
}
