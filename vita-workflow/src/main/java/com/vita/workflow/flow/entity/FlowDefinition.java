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
 * @Description: 流程定义表 实体类
 * @version 1.0
 */
@Data
@TableName("flow_definition")
@EqualsAndHashCode(callSuper = true)
public class FlowDefinition extends BaseEntity {

    /**
     * 流程编码
     */
    @TableField("flow_code")
    private String flowCode;

    /**
     * 流程名称
     */
    @TableField("flow_name")
    private String flowName;

    /**
     * 设计器模型（CLASSICS经典模型 MIMIC仿钉钉模型）
     */
    @TableField("model_value")
    private String modelValue;

    /**
     * 流程类别
     */
    @TableField("category")
    private String category;

    /**
     * 是否发布（0未发布 1已发布 9失效）
     */
    @TableField("is_publish")
    private Boolean isPublish;

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
     * 流程激活状态（0挂起 1激活）
     */
    @TableField("activity_status")
    private Boolean activityStatus;

    /**
     * 监听器类型
     */
    @TableField("listener_type")
    private String listenerType;

    /**
     * 监听器路径
     */
    @TableField("listener_path")
    private String listenerPath;

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
