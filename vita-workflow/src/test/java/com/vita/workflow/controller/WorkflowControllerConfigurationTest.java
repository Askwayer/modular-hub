package com.vita.workflow.controller;

import com.vita.core.CommonResult;
import com.vita.core.page.PageResponse;
import com.vita.workflow.controller.api.WorkflowDefinitionControllerApi;
import com.vita.workflow.controller.api.WorkflowInstanceControllerApi;
import com.vita.workflow.controller.api.WorkflowTaskControllerApi;
import com.vita.workflow.controller.impl.DefaultWorkflowDefinitionController;
import com.vita.workflow.runtime.dto.WorkflowDefinitionSearchDto;
import com.vita.workflow.runtime.service.WorkflowRuntimeService;
import com.vita.workflow.runtime.vo.WorkflowDefinitionVo;
import com.vita.workflow.service.IWorkflowDefinitionService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import java.lang.reflect.Proxy;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @BelongsProject: be-vita
 * @BelongsPackage: com.vita.workflow.controller
 * @Author: znk
 * @CreateTime: 2026-07-23
 * @Description: 工作流 Controller 默认注册与自定义退让测试
 * @Version: 1.0
 */
class WorkflowControllerConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(WorkflowControllerConfiguration.class))
            .withUserConfiguration(DefaultControllerTestConfiguration.class);

    /**
     * 默认注册全部通用工作流 Controller。
     */
    @Test
    void defaultControllersShouldBeRegistered() {
        contextRunner.run(context -> {
            assertThat(context).hasSingleBean(WorkflowDefinitionControllerApi.class);
            assertThat(context).hasSingleBean(WorkflowInstanceControllerApi.class);
            assertThat(context).hasSingleBean(WorkflowTaskControllerApi.class);
        });
    }

    /**
     * 工作流总开关关闭后不注册 Controller。
     */
    @Test
    void controllersShouldNotBeRegisteredWhenWorkflowIsDisabled() {
        contextRunner.withPropertyValues("vita.workflow.enabled=false")
                .run(context -> {
                    assertThat(context).doesNotHaveBean(WorkflowDefinitionControllerApi.class);
                    assertThat(context).doesNotHaveBean(WorkflowInstanceControllerApi.class);
                    assertThat(context).doesNotHaveBean(WorkflowTaskControllerApi.class);
                });
    }

    /**
     * 自定义接口实现存在时默认实现自动退让。
     */
    @Test
    void customControllerShouldReplaceDefaultController() {
        new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(WorkflowControllerConfiguration.class))
                .withUserConfiguration(CustomControllerTestConfiguration.class)
                .run(context -> {
                    assertThat(context).hasSingleBean(WorkflowDefinitionControllerApi.class);
                    assertThat(context.getBean(WorkflowDefinitionControllerApi.class))
                            .isInstanceOf(CustomWorkflowDefinitionController.class);
                    assertThat(context).hasSingleBean(WorkflowInstanceControllerApi.class);
                    assertThat(context).hasSingleBean(WorkflowTaskControllerApi.class);
                });
    }

    /**
     * 接口上的 Spring MVC 映射必须被默认实现继承。
     */
    @Test
    void interfaceMappingsShouldBeRegisteredBySpringMvc() {
        new WebApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(WorkflowControllerConfiguration.class))
                .withUserConfiguration(WebControllerTestConfiguration.class)
                .run(context -> {
                    RequestMappingHandlerMapping handlerMapping =
                            context.getBean(RequestMappingHandlerMapping.class);
                    Set<String> routes = handlerMapping.getHandlerMethods().keySet().stream()
                            .flatMap(info -> info.getPatternValues().stream())
                            .filter(path -> path.startsWith("/workflow/")
                                    || path.startsWith("/system/workflow/"))
                            .collect(Collectors.toSet());
                    assertThat(routes).contains(
                            "/workflow/definitions/published",
                            "/workflow/instances/start",
                            "/workflow/tasks/pending",
                            "/system/workflow/definitions/page",
                            "/system/workflow/instances/running",
                            "/system/workflow/importJson"
                    );
                });
    }

    @Configuration(proxyBeanMethods = false)
    static class DefaultControllerTestConfiguration {

        @Bean
        WorkflowRuntimeService workflowRuntimeService() {
            return workflowRuntimeServiceProxy();
        }

        @Bean
        IWorkflowDefinitionService workflowDefinitionService() {
            return defJson -> true;
        }
    }

    @Configuration(proxyBeanMethods = false)
    static class CustomControllerTestConfiguration {

        @Bean
        WorkflowRuntimeService workflowRuntimeService() {
            return workflowRuntimeServiceProxy();
        }

        @Bean
        IWorkflowDefinitionService workflowDefinitionService() {
            return defJson -> true;
        }

        @Bean
        WorkflowDefinitionControllerApi customWorkflowDefinitionController(
                WorkflowRuntimeService workflowRuntimeService,
                IWorkflowDefinitionService workflowDefinitionService) {
            return new CustomWorkflowDefinitionController(workflowRuntimeService, workflowDefinitionService);
        }
    }

    @Configuration(proxyBeanMethods = false)
    @EnableWebMvc
    static class WebControllerTestConfiguration {

        @Bean
        WorkflowRuntimeService workflowRuntimeService() {
            return workflowRuntimeServiceProxy();
        }

        @Bean
        IWorkflowDefinitionService workflowDefinitionService() {
            return defJson -> true;
        }
    }

    static class CustomWorkflowDefinitionController extends DefaultWorkflowDefinitionController {

        CustomWorkflowDefinitionController(WorkflowRuntimeService workflowRuntimeService,
                                           IWorkflowDefinitionService workflowDefinitionService) {
            super(workflowRuntimeService, workflowDefinitionService);
        }

        @Override
        public CommonResult<PageResponse<WorkflowDefinitionVo>> published(
                WorkflowDefinitionSearchDto searchDto) {
            return CommonResult.success(null);
        }
    }

    private static WorkflowRuntimeService workflowRuntimeServiceProxy() {
        return (WorkflowRuntimeService) Proxy.newProxyInstance(
                WorkflowRuntimeService.class.getClassLoader(),
                new Class<?>[]{WorkflowRuntimeService.class},
                (proxy, method, args) -> {
                    if (method.getReturnType() == boolean.class) {
                        return false;
                    }
                    return null;
                });
    }
}
