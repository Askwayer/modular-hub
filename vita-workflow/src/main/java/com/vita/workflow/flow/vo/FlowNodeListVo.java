package com.vita.workflow.flow.vo;

import lombok.Data;

/**
 * @BelongsProject: vita
 * @BelongsPackage: com.vita.workflow.flow.vo
 * @Author znk
 * @CreateTime 2026-07-14 15:51:28
 * @Description: 流程节点表 列表 VO
 * @version 1.0
 */
@Data
public class FlowNodeListVo {
    /**
     * 节点类型（0开始节点 1中间节点 2结束节点 3互斥网关 4并行网关）
     */
    private Boolean nodeType;

    /**
     * 流程定义id
     */
    private Long definitionId;

    /**
     * 流程节点编码
     */
    private String nodeCode;

    /**
     * 流程节点名称
     */
    private String nodeName;

    /**
     * 权限标识（权限类型:权限标识，可以多个，用@@隔开)
     */
    private String permissionFlag;

    /**
     * 流程签署比例值
     */
    private String nodeRatio;

    /**
     * 坐标
     */
    private String coordinate;

    /**
     * 任意结点跳转
     */
    private String anyNodeSkip;

    /**
     * 监听器类型
     */
    private String listenerType;

    /**
     * 监听器路径
     */
    private String listenerPath;

    /**
     * 审批表单是否自定义（Y是 N否）
     */
    private String formCustom;

    /**
     * 审批表单路径
     */
    private String formPath;

    /**
     * 节点扩展属性
     */
    private String ext;

    /**
     * 删除标志
     */
    private String delFlag;

}
