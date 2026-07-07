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

}
