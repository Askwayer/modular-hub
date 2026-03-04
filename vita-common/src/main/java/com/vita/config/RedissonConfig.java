package com.vita.config;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.impl.LaissezFaireSubTypeValidator;
import com.vita.property.RedissonProperty;
import jakarta.annotation.Resource;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.beans.factory.annotation.Configurable;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

/**
 * @BelongsProject: be-vita
 * @BelongsPackage: com.vita.config
 * @Author: znk
 * @CreateTime: 2026-03-02  17:19:32
 * @Description: RedissonConfig Redisson 配置类
 * @Version: 1.0
 */
@Configurable
@EnableCaching
@ConditionalOnExpression("${spring.data.redis.enabled:false}")
public class RedissonConfig {

    @Resource
    private RedissonProperty redissonProperty;

    /**
     * redisson客户端
     */
    @Bean
    public RedissonClient redissonClient() {
        Config config = new Config();
        config.useSingleServer().setAddress(String.format("redis://%s:%s", redissonProperty.getHost(), redissonProperty.getPort()));
        config.useSingleServer().setPassword(redissonProperty.getPassword());
        return Redisson.create(config);
    }

    /**
     * redis配置
     *
     * @param connectionFactory redis连接工厂      * @return redisTemplate
     * @throws Exception 异常
     */
    @Bean
    @SuppressWarnings(value = {"unchecked", "rawtypes"})
    public RedisTemplate<Object, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<Object, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);
        ObjectMapper mapper = new ObjectMapper();
        mapper.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.ANY);
        mapper.activateDefaultTyping(LaissezFaireSubTypeValidator.instance, ObjectMapper.DefaultTyping.NON_FINAL, JsonTypeInfo.As.PROPERTY);
        template.setValueSerializer(RedisSerializer.json());
        template.setKeySerializer(new StringRedisSerializer());
        template.setHashKeySerializer(new StringRedisSerializer());
        template.setHashValueSerializer(RedisSerializer.json());
        template.afterPropertiesSet();
        return template;
    }


}
