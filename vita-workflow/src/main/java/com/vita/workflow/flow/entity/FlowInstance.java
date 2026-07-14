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
 * @Description: 流程实例表 实体类
 * @version 1.0
 */
@Data
@TableName("flow_instance")
@EqualsAndHashCode(callSuper = true)
public class FlowInstance extends BaseEntity {

    /**
     * 对应flow_definition表的id
     */
    @TableField("definition_id")
    private Long definitionId;

    /**
     * 业务id
     */
    @TableField("business_id")
    private String businessId;

    /**
     * 节点类型（0开始节点 1中间节点 2结束节点 3互斥网关 4并行网关）
     */
    @TableField("node_type")
    private Boolean nodeType;

    /**
     * 流程节点编码
     */
    @TableField("node_code")
    private String nodeCode;

    /**
     * 流程节点名称
     */
    @TableField("node_name")
    private String nodeName;

    /**
     * 任务变量
     */
    @TableField("variable")
    private String variable;

    /**
     * 流程状态（0待提交 1审批中 2审批通过 4终止 5作废 6撤销 8已完成 9已退回 10失效 11拿回）
     */
    @TableField("flow_status")
    private String flowStatus;

    /**
     * 流程激活状态（0挂起 1激活）
     */
    @TableField("activity_status")
    private Boolean activityStatus;

    /**
     * 流程定义json
     */
    @TableField("def_json")
    private String defJson;

    /**
     * 扩展字段，预留给业务系统使用
     */
    @TableField("ext")
    private String ext;

    /**
     * 删除标志
     */
    @TableField("del_flag")
    private String delFlag;
}
