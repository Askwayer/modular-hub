package com.vita.workflow.controller;

import com.vita.workflow.config.ConditionalOnWorkflowEnabled;
import com.vita.workflow.controller.api.WorkflowDefinitionControllerApi;
import com.vita.workflow.controller.api.WorkflowInstanceControllerApi;
import com.vita.workflow.controller.api.WorkflowTaskControllerApi;
import com.vita.workflow.controller.impl.DefaultWorkflowDefinitionController;
import com.vita.workflow.controller.impl.DefaultWorkflowInstanceController;
import com.vita.workflow.controller.impl.DefaultWorkflowTaskController;
import com.vita.workflow.runtime.service.WorkflowRuntimeService;
import com.vita.workflow.service.IWorkflowDefinitionService;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;

/**
 * @BelongsProject: be-vita
 * @BelongsPackage: com.vita.workflow.controller
 * @Author: znk
 * @CreateTime: 2026-07-23
 * @Description: 工作流 Controller 默认实现注册配置
 * @Version: 1.0
 */
@AutoConfiguration
@ConditionalOnWorkflowEnabled
public class WorkflowControllerConfiguration {

    /**
     * 注册默认工作流定义 Controller。
     *
     * @param workflowRuntimeService 工作流运行时服务
     * @param workflowDefinitionService 流程定义导入服务
     * @return 工作流定义 Controller
     */
    @Bean
    @ConditionalOnMissingBean(WorkflowDefinitionControllerApi.class)
    public WorkflowDefinitionControllerApi workflowDefinitionController(
            WorkflowRuntimeService workflowRuntimeService,
            IWorkflowDefinitionService workflowDefinitionService) {
        return new DefaultWorkflowDefinitionController(workflowRuntimeService, workflowDefinitionService);
    }

    /**
     * 注册默认工作流实例 Controller。
     *
     * @param workflowRuntimeService 工作流运行时服务
     * @return 工作流实例 Controller
     */
    @Bean
    @ConditionalOnMissingBean(WorkflowInstanceControllerApi.class)
    public WorkflowInstanceControllerApi workflowInstanceController(
            WorkflowRuntimeService workflowRuntimeService) {
        return new DefaultWorkflowInstanceController(workflowRuntimeService);
    }

    /**
     * 注册默认工作流任务 Controller。
     *
     * @param workflowRuntimeService 工作流运行时服务
     * @return 工作流任务 Controller
     */
    @Bean
    @ConditionalOnMissingBean(WorkflowTaskControllerApi.class)
    public WorkflowTaskControllerApi workflowTaskController(
            WorkflowRuntimeService workflowRuntimeService) {
        return new DefaultWorkflowTaskController(workflowRuntimeService);
    }
}
