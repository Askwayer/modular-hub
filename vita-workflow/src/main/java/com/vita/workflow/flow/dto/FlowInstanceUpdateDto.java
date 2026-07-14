package com.vita.workflow.flow.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @BelongsProject: vita
 * @BelongsPackage: com.vita.workflow.flow.dto
 * @Author znk
 * @CreateTime 2026-07-14 15:51:28
 * @Description: 流程实例表 更新 DTO
 * @version 1.0
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class FlowInstanceUpdateDto extends FlowInstanceCreateDto {

    /**
     * 主键ID。
     */
    @NotNull(message = "id不能为空")
    private Long id;
}
