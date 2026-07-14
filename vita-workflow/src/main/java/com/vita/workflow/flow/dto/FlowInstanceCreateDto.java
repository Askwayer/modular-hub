package com.vita.workflow.flow.dto;

import lombok.Data;

/**
 * @BelongsProject: vita
 * @BelongsPackage: com.vita.workflow.flow.dto
 * @Author znk
 * @CreateTime 2026-07-14 15:51:28
 * @Description: 流程实例表 创建 DTO
 * @version 1.0
 */
@Data
public class FlowInstanceCreateDto {
    /**
     * 对应flow_definition表的id
     */
    private Long definitionId;

    /**
     * 业务id
     */
    private String businessId;

    /**
     * 节点类型（0开始节点 1中间节点 2结束节点 3互斥网关 4并行网关）
     */
    private Boolean nodeType;

    /**
     * 流程节点编码
     */
    private String nodeCode;

    /**
     * 流程节点名称
     */
    private String nodeName;

    /**
     * 任务变量
     */
    private String variable;

    /**
     * 流程状态（0待提交 1审批中 2审批通过 4终止 5作废 6撤销 8已完成 9已退回 10失效 11拿回）
     */
    private String flowStatus;

    /**
     * 流程激活状态（0挂起 1激活）
     */
    private Boolean activityStatus;

    /**
     * 流程定义json
     */
    private String defJson;

    /**
     * 扩展字段，预留给业务系统使用
     */
    private String ext;

    /**
     * 删除标志
     */
    private String delFlag;

}
