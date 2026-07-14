package com.vita.workflow.flow.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.vita.core.entity.BaseEntity;
import lombok.Data;

import lombok.EqualsAndHashCode;

/**
 * @BelongsProject: vita
 * @BelongsPackage: com.vita.workflow.flow.entity
 * @Author znk
 * @CreateTime 2026-07-14 15:51:28
 * @Description: 流程用户表 实体类
 * @version 1.0
 */
@Data
@TableName("flow_user")
@EqualsAndHashCode(callSuper = true)
public class FlowUser extends BaseEntity {

    /**
     * 人员类型（1待办任务的审批人权限 2待办任务的转办人权限 3待办任务的委托人权限）
     */
    @TableField("type")
    private String type;

    /**
     * 权限人
     */
    @TableField("processed_by")
    private String processedBy;

    /**
     * 任务表id
     */
    @TableField("associated")
    private Long associated;

    /**
     * 删除标志
     */
    @TableField("del_flag")
    private String delFlag;
}
