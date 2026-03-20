package com.vita.captcha.config;

import com.google.code.kaptcha.Producer;
import com.google.code.kaptcha.impl.DefaultKaptcha;
import com.google.code.kaptcha.util.Config;
import com.vita.captcha.properties.CaptchaProperties;
import com.vita.captcha.properties.CharCaptchaProperties;
import com.vita.captcha.properties.MathCaptchaProperties;
import com.vita.captcha.properties.SlideCaptchaProperties;
import com.vita.captcha.service.CaptchaFacade;
import com.vita.captcha.service.CaptchaHandler;
import com.vita.captcha.service.CaptchaStore;
import com.vita.captcha.service.impl.CharCaptchaHandler;
import com.vita.captcha.service.impl.DefaultCaptchaFacade;
import com.vita.captcha.service.impl.InMemoryCaptchaStore;
import com.vita.captcha.service.impl.MathCaptchaHandler;
import com.vita.captcha.service.impl.RedisCaptchaStore;
import com.vita.captcha.service.impl.SlideCaptchaHandler;
import com.vita.core.RedisCache;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Properties;

/**
 * @version 1.0
 * @BelongsProject: vita
 * @BelongsPackage: com.vita.captcha.config
 * @Author znk
 * @CreateTime 2026-03-20
 * @Description: 验证码自动装配
 */
@AutoConfiguration
@EnableConfigurationProperties({
        CaptchaProperties.class,
        CharCaptchaProperties.class,
        MathCaptchaProperties.class,
        SlideCaptchaProperties.class
})
@ConditionalOnProperty(prefix = "vita.captcha", name = "enabled", havingValue = "true")
public class CaptchaAutoConfiguration {

    static {
        System.setProperty("java.awt.headless", "true");
    }

    /**
     * 装配验证码状态存储。
     *
     * @param properties 基础配置
     * @param redisCacheProvider Redis 缓存提供者
     * @return 验证码状态存储
     */
    @Bean
    @ConditionalOnMissingBean
    public CaptchaStore captchaStore(CaptchaProperties properties, ObjectProvider<RedisCache> redisCacheProvider) {
        RedisCache redisCache = redisCacheProvider.getIfAvailable();
        return switch (properties.getStore()) {
            case REDIS -> {
                if (redisCache == null) {
                    throw new IllegalStateException("Captcha store is configured as REDIS, but RedisCache is unavailable");
                }
                yield new RedisCaptchaStore(redisCache, properties);
            }
            case IN_MEMORY -> new InMemoryCaptchaStore(properties);
            case AUTO ->
                    redisCache == null ? new InMemoryCaptchaStore(properties) : new RedisCaptchaStore(redisCache, properties);
        };
    }

    /**
     * 装配统一验证码门面。
     *
     * @param properties 基础配置
     * @param store 验证码存储
     * @param handlers 验证码处理器集合
     * @return 验证码门面
     */
    @Bean
    @ConditionalOnMissingBean
    public CaptchaFacade captchaFacade(CaptchaProperties properties, CaptchaStore store, List<CaptchaHandler> handlers) {
        return new DefaultCaptchaFacade(properties, store, handlers);
    }

    /**
     * 装配图形文本验证码图片生成器。
     *
     * @param properties 图形文本验证码配置
     * @return 图形文本验证码图片生成器
     */
    @Bean("charCaptchaProducer")
    @ConditionalOnProperty(prefix = "vita.captcha", name = "captcha-type", havingValue = "char")
    public Producer charCaptchaProducer(CharCaptchaProperties properties) {
        return buildProducer(properties.getWidth(), properties.getHeight(), properties.getLength(), properties.getCharString(),
                properties.getBorder(), properties.getFontNames(), properties.getFontSize(), properties.getFontColor(),
                properties.getNoiseColor(), properties.getBackgroundFrom(), properties.getBackgroundTo());
    }

    /**
     * 装配图形文本验证码处理器。
     *
     * @param captchaProperties 基础配置
     * @param properties 图形文本验证码配置
     * @param store 验证码存储
     * @param producer 图形文本验证码图片生成器
     * @return 图形文本验证码处理器
     */
    @Bean
    @ConditionalOnProperty(prefix = "vita.captcha", name = "captcha-type", havingValue = "char")
    public CharCaptchaHandler charCaptchaHandler(CaptchaProperties captchaProperties, CharCaptchaProperties properties,
                                                 CaptchaStore store, @Qualifier("charCaptchaProducer") Producer producer) {
        return new CharCaptchaHandler(captchaProperties, properties, store, producer);
    }

    /**
     * 装配图形答案验证码图片生成器。
     *
     * @param properties 图形答案验证码配置
     * @return 图形答案验证码图片生成器
     */
    @Bean("mathCaptchaProducer")
    @ConditionalOnProperty(prefix = "vita.captcha", name = "captcha-type", havingValue = "math")
    public Producer mathCaptchaProducer(MathCaptchaProperties properties) {
        return buildProducer(properties.getWidth(), properties.getHeight(), null, null,
                properties.getBorder(), properties.getFontNames(), properties.getFontSize(), properties.getFontColor(),
                properties.getNoiseColor(), properties.getBackgroundFrom(), properties.getBackgroundTo());
    }

    /**
     * 装配图形答案验证码处理器。
     *
     * @param captchaProperties 基础配置
     * @param properties 图形答案验证码配置
     * @param store 验证码存储
     * @param producer 图形答案验证码图片生成器
     * @return 图形答案验证码处理器
     */
    @Bean
    @ConditionalOnProperty(prefix = "vita.captcha", name = "captcha-type", havingValue = "math")
    public MathCaptchaHandler mathCaptchaHandler(CaptchaProperties captchaProperties, MathCaptchaProperties properties,
                                                 CaptchaStore store, @Qualifier("mathCaptchaProducer") Producer producer) {
        return new MathCaptchaHandler(captchaProperties, properties, store, producer);
    }

    /**
     * 装配滑动验证码处理器。
     *
     * @param captchaProperties 基础配置
     * @param properties 滑动验证码配置
     * @param store 验证码存储
     * @return 滑动验证码处理器
     */
    @Bean
    @ConditionalOnProperty(prefix = "vita.captcha", name = "captcha-type", havingValue = "slide", matchIfMissing = true)
    public SlideCaptchaHandler slideCaptchaHandler(CaptchaProperties captchaProperties, SlideCaptchaProperties properties,
                                                   CaptchaStore store) {
        return new SlideCaptchaHandler(captchaProperties, properties, store);
    }

    private Producer buildProducer(int width, int height, Integer length, String charString, String border,
                                   String fontNames, int fontSize, String fontColor, String noiseColor,
                                   String backgroundFrom, String backgroundTo) {
        Properties config = new Properties();
        config.setProperty("kaptcha.border", border);
        config.setProperty("kaptcha.border.color", "220,223,230");
        config.setProperty("kaptcha.image.width", String.valueOf(width));
        config.setProperty("kaptcha.image.height", String.valueOf(height));
        config.setProperty("kaptcha.textproducer.font.names", fontNames);
        config.setProperty("kaptcha.textproducer.font.size", String.valueOf(fontSize));
        config.setProperty("kaptcha.textproducer.font.color", fontColor);
        config.setProperty("kaptcha.noise.color", noiseColor);
        config.setProperty("kaptcha.background.clear.from", backgroundFrom);
        config.setProperty("kaptcha.background.clear.to", backgroundTo);
        config.setProperty("kaptcha.obscurificator.impl", "com.google.code.kaptcha.impl.ShadowGimpy");
        if (length != null) {
            config.setProperty("kaptcha.textproducer.char.length", String.valueOf(length));
        }
        if (StringUtils.hasText(charString)) {
            config.setProperty("kaptcha.textproducer.char.string", charString);
        }
        DefaultKaptcha producer = new DefaultKaptcha();
        producer.setConfig(new Config(config));
        return producer;
    }
}
