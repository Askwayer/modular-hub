package com.vita.config;

import cn.dev33.satoken.fun.strategy.SaCorsHandleFunction;
import cn.dev33.satoken.interceptor.SaInterceptor;
import cn.dev33.satoken.jwt.StpLogicJwtForSimple;
import cn.dev33.satoken.router.SaHttpMethod;
import cn.dev33.satoken.router.SaRouter;
import cn.dev33.satoken.stp.StpLogic;
import cn.dev33.satoken.stp.StpUtil;
import com.vita.core.exception.GlobalErrorCode;
import com.vita.core.exception.ServiceException;
import com.vita.interceptor.RequestTraceInterceptor;
import jakarta.annotation.Resource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * @BelongsProject: be-vita
 * @BelongsPackage: com.vita.config
 * @Author: znk
 * @CreateTime: 2026-03-07  14:01:49
 * @Description: Sa-Token配置类
 * @Version: 1.0
 */
@Configuration
public class SaTokenConfigure implements WebMvcConfigurer {

    @Resource
    private RequestTraceInterceptor requestTraceInterceptor;

    /**
     * Sa-Token 整合 jwt (Simple 简单模式)
     *
     * @return StpLogic
     */
    @Bean
    public StpLogic getStpLogicJwt() {
        return new StpLogicJwtForSimple();
    }

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
                    .setHeader("Access-Control-Allow-Methods", "GET,POST,PUT,DELETE,OPTIONS")
                    .setHeader("Access-Control-Max-Age", "3600")
                    // 只保留一次，显式列出需要的头
                    .setHeader("Access-Control-Allow-Headers",
                            "Authorization, Content-Type, Accept, X-Requested-With")
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

        // 注册 Sa-Token 拦截器，定义详细认证规则
        registry.addInterceptor(new SaInterceptor(handle -> {
                    // 检查是否已登录
                    try {
                        StpUtil.checkLogin();
                    } catch (Exception e) {
                        throw new ServiceException(GlobalErrorCode.UNAUTHORIZED);
                    }
                }))
                // 注册 Sa-Token 拦截器，校验规则为 StpUtil.checkLogin() 登录校验。
                .addPathPatterns("/**")
                .excludePathPatterns("/auth/login",
                        "/captcha/**",
                        "/public/**",
                        "/chat/conversations/*/messages/stream");
    }
}
