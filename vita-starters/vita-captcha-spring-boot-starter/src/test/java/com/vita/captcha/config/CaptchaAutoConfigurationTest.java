package com.vita.captcha.config;

import com.vita.captcha.enums.CaptchaType;
import com.vita.captcha.service.CaptchaFacade;
import com.vita.captcha.service.CaptchaHandler;
import com.vita.captcha.service.CaptchaStore;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @version 1.0
 * @BelongsProject: vita
 * @BelongsPackage: com.vita.captcha.config
 * @Author znk
 * @CreateTime 2026-03-20
 * @Description: 验证码自动装配测试
 */
class CaptchaAutoConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(CaptchaAutoConfiguration.class));

    @Test
    void shouldNotCreateCaptchaBeansWhenDisabled() {
        contextRunner.run(context -> {
            assertThat(context).doesNotHaveBean(CaptchaFacade.class);
            assertThat(context).doesNotHaveBean(CaptchaStore.class);
        });
    }

    @Test
    void shouldCreateOnlySlideHandlerWhenCaptchaTypeIsSlide() {
        contextRunner.withPropertyValues(
                "vita.captcha.enabled=true",
                "vita.captcha.captcha-type=slide")
                .run(context -> {
                    assertThat(context).hasSingleBean(CaptchaFacade.class);
                    assertThat(context).hasSingleBean(CaptchaStore.class);

                    Map<String, CaptchaHandler> handlers = context.getBeansOfType(CaptchaHandler.class);
                    assertThat(handlers).hasSize(1);
                    assertThat(handlers.values().iterator().next().getType()).isEqualTo(CaptchaType.SLIDE);
                    assertThat(context).doesNotHaveBean("charCaptchaProducer");
                    assertThat(context).doesNotHaveBean("mathCaptchaProducer");
                });
    }

    @Test
    void shouldCreateOnlyMathHandlerWhenCaptchaTypeIsMath() {
        contextRunner.withPropertyValues(
                "vita.captcha.enabled=true",
                "vita.captcha.captcha-type=math")
                .run(context -> {
                    Map<String, CaptchaHandler> handlers = context.getBeansOfType(CaptchaHandler.class);
                    assertThat(handlers).hasSize(1);
                    assertThat(handlers.values().iterator().next().getType()).isEqualTo(CaptchaType.MATH);
                    assertThat(context).hasBean("mathCaptchaProducer");
                    assertThat(context).doesNotHaveBean("charCaptchaProducer");
                });
    }
}
