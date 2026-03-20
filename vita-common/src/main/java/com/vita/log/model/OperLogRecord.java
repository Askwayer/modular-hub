package com.vita.log.model;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * @BelongsProject: vita
 * @BelongsPackage: com.vita.log.model
 * @Author znk
 * @CreateTime 2026-03-12
 * @Description: 审计日志记录模型，供 common 层采集并由下游适配层持久化。
 * @version 1.0
 */
@Data
public class OperLogRecord {

    private String module;

    private String businessType;

    private String requestMethod;

    private String operUrl;

    private String operIp;

    private String operLocation;

    private String operName;

    private Long operUserId;

    private String className;

    private String methodName;

    private String requestParam;

    private String responseResult;

    private Byte status;

    private String errorMsg;

    private Long costTime;

    private LocalDateTime operTime;
}
