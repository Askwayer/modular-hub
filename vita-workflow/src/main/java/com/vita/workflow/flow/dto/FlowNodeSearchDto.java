package com.vita.workflow.flow.dto;

import com.vita.core.page.PageRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @BelongsProject: vita
 * @BelongsPackage: com.vita.workflow.flow.dto
 * @Author znk
 * @CreateTime 2026-07-14 15:51:28
 * @Description: 流程节点表 搜索 DTO
 * @version 1.0
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class FlowNodeSearchDto extends PageRequest {

    /**
     * 关键字。
     */
    private String keyword;
}
