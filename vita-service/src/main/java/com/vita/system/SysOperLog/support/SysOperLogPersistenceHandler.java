package com.vita.system.SysOperLog.support;

import com.vita.log.model.OperLogRecord;
import com.vita.log.service.OperLogPersistence;
import com.vita.system.SysOperLog.dto.SysOperLogCreateDto;
import com.vita.system.SysOperLog.service.ISysOperLogService;
import jakarta.annotation.Resource;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Component;

/**
 * @version 1.0
 * @BelongsProject: vita
 * @BelongsPackage: com.vita.log.model
 * @Author znk
 * @CreateTime 2026-03-12
 * @Description: 审计日志持久化适配器，将 common 层采集结果转换为 SysOperLog 实体。
 */
@Component
public class SysOperLogPersistenceHandler implements OperLogPersistence {

    @Resource
    private ISysOperLogService sysOperLogService;

    /**
     * 持久化审计日志。
     *
     * @param record 审计日志记录
     */
    @Override
    public void persist(OperLogRecord record) {
        if (record == null) {
            return;
        }

        SysOperLogCreateDto createDto = new SysOperLogCreateDto();
        BeanUtils.copyProperties(record, createDto);
        sysOperLogService.create(createDto);
    }
}
