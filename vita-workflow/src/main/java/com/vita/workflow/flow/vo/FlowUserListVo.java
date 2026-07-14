package com.vita.workflow.flow.vo;

import lombok.Data;

/**
 * @BelongsProject: vita
 * @BelongsPackage: com.vita.workflow.flow.vo
 * @Author znk
 * @CreateTime 2026-07-14 15:51:28
 * @Description: 流程用户表 列表 VO
 * @version 1.0
 */
@Data
public class FlowUserListVo {
    /**
     * 人员类型（1待办任务的审批人权限 2待办任务的转办人权限 3待办任务的委托人权限）
     */
    private String type;

    /**
     * 权限人
     */
    private String processedBy;

    /**
     * 任务表id
     */
    private Long associated;

    /**
     * 删除标志
     */
    private String delFlag;

}
