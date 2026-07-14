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
 * @Description: 节点跳转关联表 实体类
 * @version 1.0
 */
@Data
@TableName("flow_skip")
@EqualsAndHashCode(callSuper = true)
public class FlowSkip extends BaseEntity {

    /**
     * 流程定义id
     */
    @TableField("definition_id")
    private Long definitionId;

    /**
     * 当前流程节点的编码
     */
    @TableField("now_node_code")
    private String nowNodeCode;

    /**
     * 当前节点类型（0开始节点 1中间节点 2结束节点 3互斥网关 4并行网关）
     */
    @TableField("now_node_type")
    private Boolean nowNodeType;

    /**
     * 下一个流程节点的编码
     */
    @TableField("next_node_code")
    private String nextNodeCode;

    /**
     * 下一个节点类型（0开始节点 1中间节点 2结束节点 3互斥网关 4并行网关）
     */
    @TableField("next_node_type")
    private Boolean nextNodeType;

    /**
     * 跳转名称
     */
    @TableField("skip_name")
    private String skipName;

    /**
     * 跳转类型（PASS审批通过 REJECT退回）
     */
    @TableField("skip_type")
    private String skipType;

    /**
     * 跳转条件
     */
    @TableField("skip_condition")
    private String skipCondition;

    /**
     * 坐标
     */
    @TableField("coordinate")
    private String coordinate;

    /**
     * 删除标志
     */
    @TableField("del_flag")
    private String delFlag;
}
