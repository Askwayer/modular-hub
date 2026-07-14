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
 * @Description: 流程节点表 实体类
 * @version 1.0
 */
@Data
@TableName("flow_node")
@EqualsAndHashCode(callSuper = true)
public class FlowNode extends BaseEntity {

    /**
     * 节点类型（0开始节点 1中间节点 2结束节点 3互斥网关 4并行网关）
     */
    @TableField("node_type")
    private Boolean nodeType;

    /**
     * 流程定义id
     */
    @TableField("definition_id")
    private Long definitionId;

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
     * 权限标识（权限类型:权限标识，可以多个，用@@隔开)
     */
    @TableField("permission_flag")
    private String permissionFlag;

    /**
     * 流程签署比例值
     */
    @TableField("node_ratio")
    private String nodeRatio;

    /**
     * 坐标
     */
    @TableField("coordinate")
    private String coordinate;

    /**
     * 任意结点跳转
     */
    @TableField("any_node_skip")
    private String anyNodeSkip;

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
     * 节点扩展属性
     */
    @TableField("ext")
    private String ext;

    /**
     * 删除标志
     */
    @TableField("del_flag")
    private String delFlag;
}
