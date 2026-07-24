package com.vita.auth.config;

import cn.dev33.satoken.fun.strategy.SaCorsHandleFunction;
import cn.dev33.satoken.interceptor.SaInterceptor;
import cn.dev33.satoken.router.SaHttpMethod;
import cn.dev33.satoken.router.SaRouter;
import cn.dev33.satoken.stp.StpUtil;
import com.vita.auth.constant.AuthConstants;
import com.vita.auth.property.AuthProperty;
import com.vita.core.exception.GlobalErrorCode;
import com.vita.core.exception.ServiceException;
import com.vita.log.interceptor.RequestTraceInterceptor;
import jakarta.annotation.Resource;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.ArrayList;
import java.util.List;

/**
 * @BelongsProject: be-vita
 * @BelongsPackage: com.vita.auth.config
 * @Author: znk
 * @CreateTime: 2026-03-07  14:01:49
 * @Description: Sa-Token配置类
 * @Version: 1.0
 */
@Configuration
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
public class SaTokenConfigure implements WebMvcConfigurer {

    private static final List<String> WARM_FLOW_USER_PATHS = List.of(
            "/warm-flow/published-form",
            "/warm-flow/form-content",
            "/warm-flow/form-content/**",
            "/warm-flow/execute/load/**",
            "/warm-flow/execute/hisLoad/**",
            "/warm-flow/execute/handle"
    );

    @Resource
    private RequestTraceInterceptor requestTraceInterceptor;

    @Resource
    private AuthProperty authProperty;

    /**
     * CORS 跨域处理策略
     */
    @Bean
    public SaCorsHandleFunction corsHandle() {
        return (req, res, sto) -> {
            String origin = req.getHeader("Origin");
            res
                    .setHeader("Access-Control-Allow-Origin", origin == null ? "*" : origin)
                    .setHeader("Access-Control-Allow-Credentials", "true")
                    .setHeader("Access-Control-Allow-Methods", "GET,POST,OPTIONS")
                    .setHeader("Access-Control-Max-Age", "3600")
                    // 只保留一次，显式列出需要的头
                    .setHeader("Access-Control-Allow-Headers",
                            "Authorization, Content-Type, Accept, X-Requested-With, Cache-Control,Pragma")
                    .setHeader("Access-Control-Expose-Headers",
                            "Authorization");
            SaRouter.match(SaHttpMethod.OPTIONS).free(r -> {
                // 预检直接返回
            }).back();
        };
    }

    /**
     * sa-token 拦截器
     *
     * @param registry registry
     */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(requestTraceInterceptor)
                .addPathPatterns("/**");

        List<String> excludePaths = new ArrayList<>(authProperty.getExtraExcludePaths());


        // 先统一校验登录，再收紧 Warm-Flow 内置管理接口。
        registry.addInterceptor(new SaInterceptor(handle -> {
                    try {
                        StpUtil.checkLogin();
                    } catch (Exception e) {
                        throw new ServiceException(GlobalErrorCode.UNAUTHORIZED);
                    }

                }))
                .addPathPatterns("/**")
                .excludePathPatterns(excludePaths);

        registry.addInterceptor(new SaInterceptor(handle -> checkWorkflowAdministrator()))
                .addPathPatterns("/warm-flow/**")
                .excludePathPatterns(WARM_FLOW_USER_PATHS);
    }

    /**
     * 校验工作流管理角色。
     */
    private void checkWorkflowAdministrator() {
        if (StpUtil.hasRole(AuthConstants.SUPER_ADMIN_ROLE_CODE)
                || StpUtil.hasRole(AuthConstants.SUB_ADMIN_ROLE_CODE)) {
            return;
        }
        throw new ServiceException(GlobalErrorCode.FORBIDDEN);
    }
}
