package com.vita.workflow.config;

import cn.dev33.satoken.stp.StpUtil;
import com.vita.constant.AuthConstants;
import com.vita.core.exception.GlobalErrorCode;
import com.vita.core.exception.ServiceException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Warm-Flow 管理端 Web 配置。
 *
 * @author Codex
 */
@Configuration
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
public class WorkflowWebConfiguration implements WebMvcConfigurer {

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new HandlerInterceptor() {
                    @Override
                    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
                        if (!"POST".equalsIgnoreCase(request.getMethod())) {
                            return true;
                        }
                        checkWorkflowCreatorRole();
                        return true;
                    }
                })
                .addPathPatterns("/warm-flow/save-json");
    }

    private void checkWorkflowCreatorRole() {
        try {
            StpUtil.checkLogin();
        } catch (Exception ex) {
            throw new ServiceException(GlobalErrorCode.UNAUTHORIZED);
        }
        if (StpUtil.hasRole(AuthConstants.SUPER_ADMIN_ROLE_CODE) || StpUtil.hasRole(AuthConstants.SUB_ADMIN_ROLE_CODE)) {
            return;
        }
        throw new ServiceException(GlobalErrorCode.FORBIDDEN.getCode(), "只有子管理员或超级管理员可以创建流程");
    }
}
