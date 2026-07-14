package com.vita.workflow.flow.vo;

import lombok.Data;

/**
 * @BelongsProject: vita
 * @BelongsPackage: com.vita.workflow.flow.vo
 * @Author znk
 * @CreateTime 2026-07-14 15:51:28
 * @Description: 历史任务记录表 列表 VO
 * @version 1.0
 */
@Data
public class FlowHisTaskListVo {
    /**
     * 对应flow_definition表的id
     */
    private Long definitionId;

    /**
     * 对应flow_instance表的id
     */
    private Long instanceId;

    /**
     * 对应flow_task表的id
     */
    private Long taskId;

    /**
     * 开始节点编码
     */
    private String nodeCode;

    /**
     * 开始节点名称
     */
    private String nodeName;

    /**
     * 开始节点类型（0开始节点 1中间节点 2结束节点 3互斥网关 4并行网关）
     */
    private Boolean nodeType;

    /**
     * 目标节点编码
     */
    private String targetNodeCode;

    /**
     * 结束节点名称
     */
    private String targetNodeName;

    /**
     * 审批人
     */
    private String approver;

    /**
     * 协作方式(1审批 2转办 3委派 4会签 5票签 6加签 7减签)
     */
    private Boolean cooperateType;

    /**
     * 协作人
     */
    private String collaborator;

    /**
     * 流转类型（PASS通过 REJECT退回 NONE无动作）
     */
    private String skipType;

    /**
     * 流程状态（0待提交 1审批中 2审批通过 4终止 5作废 6撤销 8已完成 9已退回 10失效 11拿回）
     */
    private String flowStatus;

    /**
     * 审批表单是否自定义（Y是 N否）
     */
    private String formCustom;

    /**
     * 审批表单路径
     */
    private String formPath;

    /**
     * 审批意见
     */
    private String message;

    /**
     * 任务变量
     */
    private String variable;

    /**
     * 业务详情 存业务表对象json字符串
     */
    private String ext;

    /**
     * 删除标志
     */
    private String delFlag;

}
