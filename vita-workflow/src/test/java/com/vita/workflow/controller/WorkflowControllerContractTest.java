package com.vita.workflow.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.vita.repeat.annotation.RepeatSubmit;
import com.vita.workflow.constant.WorkflowConstants;
import com.vita.workflow.controller.api.WorkflowDefinitionControllerApi;
import com.vita.workflow.controller.api.WorkflowInstanceControllerApi;
import com.vita.workflow.controller.api.WorkflowTaskControllerApi;
import com.vita.workflow.controller.impl.DefaultWorkflowDefinitionController;
import com.vita.workflow.controller.impl.DefaultWorkflowInstanceController;
import com.vita.workflow.runtime.dto.WorkflowDefinitionSearchDto;
import com.vita.workflow.runtime.dto.WorkflowInstanceSearchDto;
import org.dromara.warm.flow.core.dto.DefJson;
import org.junit.jupiter.api.Test;
import org.springframework.aop.aspectj.AspectJExpressionPointcut;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @BelongsProject: be-vita
 * @BelongsPackage: com.vita.workflow.controller
 * @Author: znk
 * @CreateTime: 2026-07-23
 * @Description: 工作流 Controller 路由、权限和 AOP 契约测试
 * @Version: 1.0
 */
class WorkflowControllerContractTest {

    /**
     * 验证三组接口保留迁移前的全部路由和 HTTP 方法。
     */
    @Test
    void controllerApisShouldKeepExistingRouteContract() {
        Set<String> routes = new LinkedHashSet<>();
        collectRoutes(WorkflowDefinitionControllerApi.class, routes);
        collectRoutes(WorkflowInstanceControllerApi.class, routes);
        collectRoutes(WorkflowTaskControllerApi.class, routes);

        assertThat(routes).containsExactlyInAnyOrder(
                "GET /workflow/definitions/published",
                "GET /system/workflow/definitions/page",
                "GET /system/workflow/definitions/{definitionId}",
                "PUT /system/workflow/definitions/{definitionId}/publish",
                "PUT /system/workflow/definitions/{definitionId}/unpublish",
                "POST /system/workflow/definitions/{definitionId}/copy",
                "DELETE /system/workflow/definitions/{definitionId}",
                "POST /system/workflow/importJson",
                "POST /workflow/instances/start",
                "GET /workflow/instances/mine",
                "GET /workflow/instances/{instanceId}",
                "GET /workflow/instances/{instanceId}/history",
                "POST /workflow/instances/{instanceId}/cancel",
                "GET /system/workflow/instances/running",
                "GET /system/workflow/instances/finished",
                "GET /system/workflow/instances/{instanceId}",
                "GET /system/workflow/instances/{instanceId}/history",
                "GET /workflow/tasks/pending",
                "GET /workflow/tasks/completed",
                "GET /workflow/tasks/{taskId}",
                "POST /workflow/tasks/{taskId}/pass",
                "POST /workflow/tasks/{taskId}/reject-last",
                "POST /workflow/tasks/{taskId}/reject-to"
        );
    }

    /**
     * 验证管理端默认实现直接声明正确权限码。
     *
     * @throws NoSuchMethodException 方法不存在
     */
    @Test
    void managementMethodsShouldDeclarePermissionDirectly() throws NoSuchMethodException {
        assertPermission(DefaultWorkflowDefinitionController.class, "definitionPage",
                WorkflowConstants.WORKFLOW_DEFINITION_VIEW_PERMISSION, WorkflowDefinitionSearchDto.class);
        assertPermission(DefaultWorkflowDefinitionController.class, "definitionDetail",
                WorkflowConstants.WORKFLOW_DEFINITION_VIEW_PERMISSION, Long.class);
        assertPermission(DefaultWorkflowDefinitionController.class, "publishDefinition",
                WorkflowConstants.WORKFLOW_DEFINITION_MANAGE_PERMISSION, Long.class);
        assertPermission(DefaultWorkflowDefinitionController.class, "unpublishDefinition",
                WorkflowConstants.WORKFLOW_DEFINITION_MANAGE_PERMISSION, Long.class);
        assertPermission(DefaultWorkflowDefinitionController.class, "copyDefinition",
                WorkflowConstants.WORKFLOW_DEFINITION_MANAGE_PERMISSION, Long.class);
        assertPermission(DefaultWorkflowDefinitionController.class, "deleteDefinition",
                WorkflowConstants.WORKFLOW_DEFINITION_MANAGE_PERMISSION, Long.class);
        assertPermission(DefaultWorkflowDefinitionController.class, "importJson",
                WorkflowConstants.WORKFLOW_IMPORT_PERMISSION, DefJson.class);

        assertPermission(DefaultWorkflowInstanceController.class, "runningInstances",
                WorkflowConstants.WORKFLOW_INSTANCE_VIEW_PERMISSION, WorkflowInstanceSearchDto.class);
        assertPermission(DefaultWorkflowInstanceController.class, "finishedInstances",
                WorkflowConstants.WORKFLOW_INSTANCE_VIEW_PERMISSION, WorkflowInstanceSearchDto.class);
        assertPermission(DefaultWorkflowInstanceController.class, "managedInstanceDetail",
                WorkflowConstants.WORKFLOW_INSTANCE_VIEW_PERMISSION, Long.class);
        assertPermission(DefaultWorkflowInstanceController.class, "managedInstanceHistory",
                WorkflowConstants.WORKFLOW_INSTANCE_VIEW_PERMISSION, Long.class);

        Method importMethod = DefaultWorkflowDefinitionController.class.getMethod("importJson", DefJson.class);
        assertThat(importMethod.getAnnotation(RepeatSubmit.class)).isNotNull();
    }

    /**
     * 验证 workflow 模块 Controller 仍命中重复提交切点。
     *
     * @throws NoSuchMethodException 方法不存在
     */
    @Test
    void workflowControllerShouldMatchRepeatSubmitPointcut() throws NoSuchMethodException {
        AspectJExpressionPointcut pointcut = new AspectJExpressionPointcut();
        pointcut.setExpression("execution(public * com.vita..controller..*Controller.*(..))");
        Method method = DefaultWorkflowDefinitionController.class.getMethod("importJson", DefJson.class);

        assertThat(pointcut.matches(method, DefaultWorkflowDefinitionController.class)).isTrue();
    }

    private void assertPermission(Class<?> controllerType, String methodName, String permission,
                                  Class<?>... parameterTypes) throws NoSuchMethodException {
        Method method = controllerType.getMethod(methodName, parameterTypes);
        SaCheckPermission annotation = method.getAnnotation(SaCheckPermission.class);
        assertThat(annotation).as(controllerType.getSimpleName() + "#" + methodName).isNotNull();
        assertThat(annotation.value()).containsExactly(permission);
    }

    private void collectRoutes(Class<?> apiType, Set<String> routes) {
        Arrays.stream(apiType.getMethods()).forEach(method -> {
            putRoutes(routes, method.getAnnotation(GetMapping.class), "GET");
            putRoutes(routes, method.getAnnotation(PostMapping.class), "POST");
            putRoutes(routes, method.getAnnotation(PutMapping.class), "PUT");
            putRoutes(routes, method.getAnnotation(DeleteMapping.class), "DELETE");
        });
    }

    private void putRoutes(Set<String> routes, GetMapping mapping, String httpMethod) {
        if (mapping != null) {
            Arrays.stream(mapping.value()).map(path -> httpMethod + " " + path).forEach(routes::add);
        }
    }

    private void putRoutes(Set<String> routes, PostMapping mapping, String httpMethod) {
        if (mapping != null) {
            Arrays.stream(mapping.value()).map(path -> httpMethod + " " + path).forEach(routes::add);
        }
    }

    private void putRoutes(Set<String> routes, PutMapping mapping, String httpMethod) {
        if (mapping != null) {
            Arrays.stream(mapping.value()).map(path -> httpMethod + " " + path).forEach(routes::add);
        }
    }

    private void putRoutes(Set<String> routes, DeleteMapping mapping, String httpMethod) {
        if (mapping != null) {
            Arrays.stream(mapping.value()).map(path -> httpMethod + " " + path).forEach(routes::add);
        }
    }
}
