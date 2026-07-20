package com.vita.controller.workflow.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.HashMap;
import java.util.Map;

/**
 * @BelongsProject: be-vita
 * @BelongsPackage: com.vita.controller.workflow.dto
 * @Author: znk
 * @CreateTime: 2026-07-20
 * @Description: 工作流指定节点退回请求
 * @Version: 1.0
 */
@Data
public class WorkflowRejectToRequest {

    @NotBlank(message = "退回节点不能为空")
    @Size(max = 100, message = "退回节点编码长度不能超过100")
    private String targetNodeCode;

    @Size(max = 500, message = "办理意见长度不能超过500")
    private String message;

    private Map<String, Object> variables = new HashMap<>();
}
