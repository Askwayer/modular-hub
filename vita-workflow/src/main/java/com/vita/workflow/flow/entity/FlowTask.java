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
 * @Description: 待办任务表 实体类
 * @version 1.0
 */
@Data
@TableName("flow_task")
@EqualsAndHashCode(callSuper = true)
public class FlowTask extends BaseEntity {

    /**
     * 对应flow_definition表的id
     */
    @TableField("definition_id")
    private Long definitionId;

    /**
     * 对应flow_instance表的id
     */
    @TableField("instance_id")
    private Long instanceId;

    /**
     * 节点编码
     */
    @TableField("node_code")
    private String nodeCode;

    /**
     * 节点名称
     */
    @TableField("node_name")
    private String nodeName;

    /**
     * 节点类型（0开始节点 1中间节点 2结束节点 3互斥网关 4并行网关）
     */
    @TableField("node_type")
    private Boolean nodeType;

    /**
     * 流程状态（0待提交 1审批中 2审批通过 4终止 5作废 6撤销 8已完成 9已退回 10失效 11拿回）
     */
    @TableField("flow_status")
    private String flowStatus;

    /**
     * 审批表单是否自定义（Y是 N否）
     */
    @TableField("form_custom")
    private String formCustom;

    /**
     * 审批表单路径
     */
    @TableField("form_path")
    private String formPath;

    /**
     * 删除标志
     */
    @TableField("del_flag")
    private String delFlag;
}
