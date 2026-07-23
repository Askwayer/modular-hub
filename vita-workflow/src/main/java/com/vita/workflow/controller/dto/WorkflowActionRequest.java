package com.vita.workflow.controller.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.HashMap;
import java.util.Map;

/**
 * @BelongsProject: be-vita
 * @BelongsPackage: com.vita.workflow.controller.dto
 * @Author: znk
 * @CreateTime: 2026-07-23
 * @Description: 工作流任务操作请求
 * @Version: 1.0
 */
@Data
public class WorkflowActionRequest {

    @Size(max = 500, message = "办理意见长度不能超过500")
    private String message;

    private Map<String, Object> variables = new HashMap<>();
}
