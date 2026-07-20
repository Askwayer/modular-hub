package com.vita.config;

import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.dev33.satoken.filter.SaTokenContextFilterForJakartaServlet;
import cn.dev33.satoken.spring.SaBeanInject;
import cn.dev33.satoken.spring.SaBeanRegister;
import cn.dev33.satoken.spring.SaTokenContextRegister;
import cn.dev33.satoken.stp.StpInterface;
import cn.dev33.satoken.stp.StpUtil;
import com.vita.auth.config.SaTokenConfigure;
import com.vita.auth.constant.AuthConstants;
import com.vita.auth.handler.SaTokenExceptionHandler;
import com.vita.auth.property.AuthProperty;
import com.vita.controller.workflow.WorkflowAdminAccessChecker;
import com.vita.controller.workflow.WorkflowController;
import com.vita.core.exception.ControllerExceptionHandler;
import com.vita.log.interceptor.RequestTraceInterceptor;
import com.vita.workflow.constant.WorkflowConstants;
import com.vita.workflow.service.IWorkflowDefinitionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.ConfigDataApplicationContextInitializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.support.DependencyInjectionTestExecutionListener;
import org.springframework.test.context.support.DirtiesContextBeforeModesTestExecutionListener;
import org.springframework.test.context.support.DirtiesContextTestExecutionListener;
import org.springframework.test.context.web.ServletTestExecutionListener;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup;

/**
 * @BelongsProject: be-vita
 * @BelongsPackage: com.vita.config
 * @Author: znk
 * @CreateTime: 2026-07-14
 * @Description: 工作流路由权限回归测试
 * @Version: 1.0
 */
@ExtendWith(SpringExtension.class)
@WebAppConfiguration
@ContextConfiguration(
        classes = WorkflowRouteSecurityTest.RouteTestConfiguration.class,
        initializers = ConfigDataApplicationContextInitializer.class
)
@TestExecutionListeners(
        listeners = {
                ServletTestExecutionListener.class,
                DirtiesContextBeforeModesTestExecutionListener.class,
                DependencyInjectionTestExecutionListener.class,
                DirtiesContextTestExecutionListener.class
        },
        inheritListeners = false
)
class WorkflowRouteSecurityTest {

    private static final String CONTEXT_PATH = "/admin/api";

    private static final String NORMAL_USER_ID = "1001";

    private static final String SUB_ADMIN_ID = "2001";

    private static final String SUPER_ADMIN_ID = "2002";

    private static final String ROLE_ONLY_ADMIN_ID = "2003";

    private static final String PERMISSION_ONLY_USER_ID = "2004";

    @Autowired
    private WebApplicationContext applicationContext;

    @Autowired
    private SaTokenContextFilterForJakartaServlet saTokenContextFilter;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = webAppContextSetup(applicationContext)
                .addFilters(saTokenContextFilter)
                .build();
    }

    /**
     * UI 配置与静态资源保持匿名访问。
     *
     * @throws Exception 请求异常
     */
    @Test
    void uiConfigAndStaticResourcesShouldRemainPublic() throws Exception {
        assertPublic("/warm-flow-ui/config");
        assertPublic("/warm-flow-ui/index.html");
        assertPublic("/warm-flow-ui/css/index.css");
        assertPublic("/warm-flow-ui/js/index.js");
        assertPublic("/warm-flow-ui/ico/favicon.ico");
    }

    /**
     * 未登录用户不能访问 Warm-Flow 接口和本地管理接口。
     *
     * @throws Exception 请求异常
     */
    @Test
    void protectedWorkflowRoutesShouldRejectAnonymousRequests() throws Exception {
        assertUnauthorized(adminGet("/warm-flow/query-def"));
        assertUnauthorized(adminGet("/warm-flow/published-form"));
        assertUnauthorized(adminGet("/warm-flow/form-content/1"));
        assertUnauthorized(adminGet("/warm-flow/execute/load/1"));
        assertUnauthorized(adminGet("/warm-flow/execute/hisLoad/1"));
        assertUnauthorized(adminPost("/warm-flow/execute/handle"));
        assertUnauthorized(adminGet("/workflow/definitions/published"));
        assertUnauthorized(adminGet("/workflow/instances/mine"));
        assertUnauthorized(adminGet("/workflow/tasks/pending"));
        assertUnauthorized(adminPost("/workflow/tasks/1/pass"));
        assertUnauthorized(adminGet("/system/workflow/definitions/page"));
        assertUnauthorized(adminGet("/system/workflow/instances/running"));
        assertUnauthorized(importJsonRequest(null));
    }

    /**
     * 普通登录用户可以进入 Warm-Flow 内置接口。
     *
     * @throws Exception 请求异常
     */
    @Test
    void loggedInUserShouldAccessBuiltInWorkflowRoutes() throws Exception {
        String token = login(NORMAL_USER_ID);

        assertEntered(adminGet("/warm-flow/query-def"), token);
        assertEntered(adminGet("/warm-flow/published-form"), token);
        assertEntered(adminGet("/warm-flow/form-content/1"), token);
        assertEntered(adminGet("/warm-flow/execute/load/1"), token);
        assertEntered(adminGet("/warm-flow/execute/hisLoad/1"), token);
        assertEntered(adminPost("/warm-flow/execute/handle"), token);
        assertEntered(adminGet("/workflow/definitions/published"), token);
        assertEntered(adminGet("/workflow/instances/mine"), token);
        assertEntered(adminGet("/workflow/tasks/pending"), token);
        assertEntered(adminPost("/workflow/tasks/1/pass"), token);
    }

    /**
     * 新增工作流管理接口必须同时满足权限码和管理员角色。
     *
     * @throws Exception 请求异常
     */
    @Test
    void workflowManagementRoutesShouldRequirePermissionAndAdminRole() throws Exception {
        assertForbidden(withToken(adminGet("/system/workflow/definitions/page"), login(NORMAL_USER_ID)));
        assertForbidden(withToken(adminGet("/system/workflow/definitions/page"), login(ROLE_ONLY_ADMIN_ID)));
        assertForbidden(withToken(adminGet("/system/workflow/definitions/page"), login(PERMISSION_ONLY_USER_ID)));
        assertForbidden(withToken(adminGet("/system/workflow/instances/running"), login(NORMAL_USER_ID)));

        assertEntered(adminGet("/system/workflow/definitions/page"), login(SUB_ADMIN_ID));
        assertEntered(adminGet("/system/workflow/definitions/page"), login(SUPER_ADMIN_ID));
        assertEntered(adminGet("/system/workflow/instances/running"), login(SUB_ADMIN_ID));
        assertEntered(adminGet("/system/workflow/instances/running"), login(SUPER_ADMIN_ID));
    }

    /**
     * 本地管理接口必须同时满足权限码和管理员角色。
     *
     * @throws Exception 请求异常
     */
    @Test
    void localManagementRouteShouldRequirePermissionAndAdminRole() throws Exception {
        assertForbidden(importJsonRequest(login(NORMAL_USER_ID)));
        assertForbidden(importJsonRequest(login(ROLE_ONLY_ADMIN_ID)));
        assertForbidden(importJsonRequest(login(PERMISSION_ONLY_USER_ID)));

        assertImportSucceeded(login(SUB_ADMIN_ID));
        assertImportSucceeded(login(SUPER_ADMIN_ID));
    }

    private void assertPublic(String path) throws Exception {
        mockMvc.perform(adminGet(path))
                .andExpect(status().isOk())
                .andExpect(content().string("entered"));
    }

    private void assertUnauthorized(MockHttpServletRequestBuilder request) throws Exception {
        mockMvc.perform(request)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(401));
    }

    private void assertForbidden(MockHttpServletRequestBuilder request) throws Exception {
        mockMvc.perform(request)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(403));
    }

    private void assertEntered(MockHttpServletRequestBuilder request, String token) throws Exception {
        mockMvc.perform(withToken(request, token))
                .andExpect(status().isOk())
                .andExpect(content().string("entered"));
    }

    private void assertImportSucceeded(String token) throws Exception {
        mockMvc.perform(importJsonRequest(token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.content").value(true));
    }

    private String login(String loginId) throws Exception {
        return mockMvc.perform(adminPost("/auth/login").param("loginId", loginId))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
    }

    private MockHttpServletRequestBuilder importJsonRequest(String token) {
        MockHttpServletRequestBuilder request = adminPost("/system/workflow/importJson")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}");
        return token == null ? request : withToken(request, token);
    }

    private MockHttpServletRequestBuilder withToken(MockHttpServletRequestBuilder request, String token) {
        return request.header("Authorization", "Bearer " + token);
    }

    private MockHttpServletRequestBuilder adminGet(String path) {
        return get(CONTEXT_PATH + path).contextPath(CONTEXT_PATH);
    }

    private MockHttpServletRequestBuilder adminPost(String path) {
        return post(CONTEXT_PATH + path).contextPath(CONTEXT_PATH);
    }

    @Configuration(proxyBeanMethods = false)
    @EnableWebMvc
    @EnableConfigurationProperties(AuthProperty.class)
    @Import({
            SaTokenConfigure.class,
            SaBeanRegister.class,
            SaBeanInject.class,
            SaTokenContextRegister.class,
            RequestTraceInterceptor.class,
            ControllerExceptionHandler.class,
            SaTokenExceptionHandler.class,
            WorkflowController.class,
            WorkflowRouteProbeController.class
    })
    static class RouteTestConfiguration {

        @Bean
        IWorkflowDefinitionService workflowDefinitionService() {
            return defJson -> true;
        }

        @Bean
        StpInterface workflowRouteStpInterface() {
            return new StpInterface() {
                @Override
                public List<String> getPermissionList(Object loginId, String loginType) {
                    String id = String.valueOf(loginId);
                    if (SUB_ADMIN_ID.equals(id) || SUPER_ADMIN_ID.equals(id)
                            || PERMISSION_ONLY_USER_ID.equals(id)) {
                        return List.of(
                                WorkflowConstants.WORKFLOW_IMPORT_PERMISSION,
                                WorkflowConstants.WORKFLOW_DEFINITION_VIEW_PERMISSION,
                                WorkflowConstants.WORKFLOW_DEFINITION_MANAGE_PERMISSION,
                                WorkflowConstants.WORKFLOW_INSTANCE_VIEW_PERMISSION
                        );
                    }
                    return List.of();
                }

                @Override
                public List<String> getRoleList(Object loginId, String loginType) {
                    String id = String.valueOf(loginId);
                    if (SUB_ADMIN_ID.equals(id) || ROLE_ONLY_ADMIN_ID.equals(id)) {
                        return List.of(AuthConstants.SUB_ADMIN_ROLE_CODE);
                    }
                    if (SUPER_ADMIN_ID.equals(id)) {
                        return List.of(AuthConstants.SUPER_ADMIN_ROLE_CODE);
                    }
                    return List.of();
                }
            };
        }
    }

    @RestController
    static class WorkflowRouteProbeController {

        @PostMapping("/auth/login")
        String login(@RequestParam("loginId") String loginId) {
            StpUtil.login(loginId);
            return StpUtil.getTokenValue();
        }

        @GetMapping({
                "/warm-flow-ui/config",
                "/warm-flow-ui/index.html",
                "/warm-flow-ui/css/index.css",
                "/warm-flow-ui/js/index.js",
                "/warm-flow-ui/ico/favicon.ico",
                "/warm-flow/query-def",
                "/warm-flow/published-form",
                "/warm-flow/form-content/1",
                "/warm-flow/execute/load/1",
                "/warm-flow/execute/hisLoad/1",
                "/workflow/definitions/published",
                "/workflow/instances/mine",
                "/workflow/tasks/pending"
        })
        String getRoute() {
            return "entered";
        }

        @PostMapping({
                "/warm-flow/execute/handle",
                "/workflow/tasks/1/pass"
        })
        String postRoute() {
            return "entered";
        }

        @GetMapping("/system/workflow/definitions/page")
        @SaCheckPermission(WorkflowConstants.WORKFLOW_DEFINITION_VIEW_PERMISSION)
        String definitionManagementRoute() {
            WorkflowAdminAccessChecker.check();
            return "entered";
        }

        @GetMapping("/system/workflow/instances/running")
        @SaCheckPermission(WorkflowConstants.WORKFLOW_INSTANCE_VIEW_PERMISSION)
        String instanceManagementRoute() {
            WorkflowAdminAccessChecker.check();
            return "entered";
        }
    }
}
