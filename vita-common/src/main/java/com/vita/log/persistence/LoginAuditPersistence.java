package com.vita.log.persistence;

import com.vita.log.model.LoginAuditRecord;

/**
 * 登录审计持久化扩展点，由上层模块提供具体实现。
 *
 * @author zhuningkang
 */
public interface LoginAuditPersistence {

    /**
     * 持久化登录审计记录。
     *
     * @param record 登录审计记录
     */
    void persist(LoginAuditRecord record);
}
