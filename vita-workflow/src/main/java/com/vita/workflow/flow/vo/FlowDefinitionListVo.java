package com.vita.workflow.flow.vo;

import lombok.Data;

/**
 * @BelongsProject: vita
 * @BelongsPackage: com.vita.workflow.flow.vo
 * @Author znk
 * @CreateTime 2026-07-14 15:51:28
 * @Description: 流程定义表 列表 VO
 * @version 1.0
 */
@Data
public class FlowDefinitionListVo {
    /**
     * 流程编码
     */
    private String flowCode;

    /**
     * 流程名称
     */
    private String flowName;

    /**
     * 设计器模型（CLASSICS经典模型 MIMIC仿钉钉模型）
     */
    private String modelValue;

    /**
     * 流程类别
     */
    private String category;

    /**
     * 是否发布（0未发布 1已发布 9失效）
     */
    private Boolean isPublish;

    /**
     * 审批表单是否自定义（Y是 N否）
     */
    private String formCustom;

    /**
     * 审批表单路径
     */
    private String formPath;

    /**
     * 流程激活状态（0挂起 1激活）
     */
    private Boolean activityStatus;

    /**
     * 监听器类型
     */
    private String listenerType;

    /**
     * 监听器路径
     */
    private String listenerPath;

    /**
     * 业务详情 存业务表对象json字符串
     */
    private String ext;

    /**
     * 删除标志
     */
    private String delFlag;

}
