package com.vita.log.model;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 操作日志快照，供 common 层采集并由下游持久化适配器落库。
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
