package com.vita.log.persistence;

/**
 * 登录日志归属地回填扩展点，由上层模块提供具体实现。
 *
 * @author znk
 */
public interface LoginLocationPersistence {

    /**
     * 回填登录日志归属地。
     *
     * @param logId     登录日志主键ID
     * @param location 归属地
     */
    void updateLoginLocation(Long logId, String location);
}
