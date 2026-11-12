package com.vita.auth.config;

import cn.dev33.satoken.jwt.StpLogicJwtForSimple;
import cn.dev33.satoken.stp.StpLogic;
import cn.dev33.satoken.stp.StpUtil;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @BelongsProject: be-vita
 * @BelongsPackage: com.vita.auth.config
 * @Author: znk
 * @CreateTime: 2026-07-10  16:40:48
 * @Description: Sa-Token共享配置类
 * @Version: 1.0
 */
@Configuration
public class SaTokenSharedConfiguration {

    /**
     * 创建 Sa-Token JWT 逻辑处理器。
     *
     * @return Sa-Token 逻辑处理器
     */
    @Bean
    @ConditionalOnMissingBean(StpLogic.class)
    public StpLogic getStpLogicJwt() {
        return new StpLogicJwtForSimple(StpUtil.TYPE);
    }
}
