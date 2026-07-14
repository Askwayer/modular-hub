package com.vita.workflow.flow.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * @BelongsProject: vita
 * @BelongsPackage: com.vita.workflow.flow.dto
 * @Author znk
 * @CreateTime 2026-07-14 15:51:28
 * @Description: 流程用户表 删除 DTO
 * @version 1.0
 */
@Data
public class FlowUserDeletedDto {

    /**
     * 主键ID。
     */
    @NotNull(message = "id不能为空")
    private Long id;
}
