package com.vita.workflow.flow.vo;

import lombok.Data;

/**
 * @BelongsProject: vita
 * @BelongsPackage: com.vita.workflow.flow.vo
 * @Author znk
 * @CreateTime 2026-07-14 15:51:28
 * @Description: 节点跳转关联表 列表 VO
 * @version 1.0
 */
@Data
public class FlowSkipListVo {
    /**
     * 流程定义id
     */
    private Long definitionId;

    /**
     * 当前流程节点的编码
     */
    private String nowNodeCode;

    /**
     * 当前节点类型（0开始节点 1中间节点 2结束节点 3互斥网关 4并行网关）
     */
    private Boolean nowNodeType;

    /**
     * 下一个流程节点的编码
     */
    private String nextNodeCode;

    /**
     * 下一个节点类型（0开始节点 1中间节点 2结束节点 3互斥网关 4并行网关）
     */
    private Boolean nextNodeType;

    /**
     * 跳转名称
     */
    private String skipName;

    /**
     * 跳转类型（PASS审批通过 REJECT退回）
     */
    private String skipType;

    /**
     * 跳转条件
     */
    private String skipCondition;

    /**
     * 坐标
     */
    private String coordinate;

    /**
     * 删除标志
     */
    private String delFlag;

}
