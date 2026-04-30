package com.vita.config;

import cn.dev33.satoken.jwt.StpLogicJwtForSimple;
import cn.dev33.satoken.stp.StpLogic;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Sa-Token 共享配置。
 *
 * <p>JWT 逻辑对 Servlet/WebFlux 都是通用能力，不能绑在 MVC 配置类上。</p>
 */
@Configuration
public class SaTokenSharedConfiguration {

    /**
     * Sa-Token 整合 jwt（Simple 简单模式）。
     */
    @Bean
    @ConditionalOnMissingBean(StpLogic.class)
    public StpLogic getStpLogicJwt() {
        return new StpLogicJwtForSimple();
    }
}
