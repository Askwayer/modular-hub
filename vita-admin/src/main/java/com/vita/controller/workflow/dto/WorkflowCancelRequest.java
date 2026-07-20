package com.vita.controller.workflow.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * @BelongsProject: be-vita
 * @BelongsPackage: com.vita.controller.workflow.dto
 * @Author: znk
 * @CreateTime: 2026-07-20
 * @Description: 工作流撤销请求
 * @Version: 1.0
 */
@Data
public class WorkflowCancelRequest {

    @Size(max = 500, message = "撤销原因长度不能超过500")
    private String message;
}
