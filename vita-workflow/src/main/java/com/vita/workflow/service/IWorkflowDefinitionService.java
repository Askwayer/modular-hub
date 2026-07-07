package com.vita.workflow.service;

import org.dromara.warm.flow.core.dto.DefJson;

/**
 * 工作流定义服务接口。
 *
 * @author Codex
 */
public interface IWorkflowDefinitionService {

    /**
     * 导入 Warm-Flow 流程定义 JSON。
     *
     * @param defJson 流程定义对象
     * @return 导入结果
     */
    boolean importJson(DefJson defJson);

    /**
     * 创建并发布报销审批流程 V1。
     *
     * @return 流程定义 ID
     */
    Long createReimburseApproval();
}
