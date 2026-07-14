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
 * @Description: 历史任务记录表 实体类
 * @version 1.0
 */
@Data
@TableName("flow_his_task")
@EqualsAndHashCode(callSuper = true)
public class FlowHisTask extends BaseEntity {

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
     * 对应flow_task表的id
     */
    @TableField("task_id")
    private Long taskId;

    /**
     * 开始节点编码
     */
    @TableField("node_code")
    private String nodeCode;

    /**
     * 开始节点名称
     */
    @TableField("node_name")
    private String nodeName;

    /**
     * 开始节点类型（0开始节点 1中间节点 2结束节点 3互斥网关 4并行网关）
     */
    @TableField("node_type")
    private Boolean nodeType;

    /**
     * 目标节点编码
     */
    @TableField("target_node_code")
    private String targetNodeCode;

    /**
     * 结束节点名称
     */
    @TableField("target_node_name")
    private String targetNodeName;

    /**
     * 审批人
     */
    @TableField("approver")
    private String approver;

    /**
     * 协作方式(1审批 2转办 3委派 4会签 5票签 6加签 7减签)
     */
    @TableField("cooperate_type")
    private Boolean cooperateType;

    /**
     * 协作人
     */
    @TableField("collaborator")
    private String collaborator;

    /**
     * 流转类型（PASS通过 REJECT退回 NONE无动作）
     */
    @TableField("skip_type")
    private String skipType;

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
     * 审批意见
     */
    @TableField("message")
    private String message;

    /**
     * 任务变量
     */
    @TableField("variable")
    private String variable;

    /**
     * 业务详情 存业务表对象json字符串
     */
    @TableField("ext")
    private String ext;

    /**
     * 删除标志
     */
    @TableField("del_flag")
    private String delFlag;
}
