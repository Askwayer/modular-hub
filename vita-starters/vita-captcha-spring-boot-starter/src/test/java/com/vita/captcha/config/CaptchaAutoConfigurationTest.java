package com.vita.captcha.config;

import com.vita.captcha.service.CaptchaApplicationService;
import com.vita.captcha.service.GraphCaptchaServiceImpl;
import com.vita.captcha.service.SlideCaptchaServiceImpl;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

class CaptchaAutoConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(CaptchaAutoConfiguration.class));

    @Test
    void shouldLoadSlideBeansWhenDefaultTypeIsAllSlide() {
        contextRunner
                .withPropertyValues("vita.captcha.enabled=true")
                .run(context -> {
                    assertThat(context).hasSingleBean(CaptchaApplicationService.class);
                    assertThat(context).hasSingleBean(SlideCaptchaServiceImpl.class);
                    assertThat(context).doesNotHaveBean(GraphCaptchaServiceImpl.class);
                });
    }

    @Test
    void shouldLoadGraphBeansWhenTypeIsChar() {
        contextRunner
                .withUserConfiguration(GraphCaptchaConfig.class)
                .withPropertyValues(
                        "vita.captcha.enabled=true",
                        "vita.captcha.captcha-type=char"
                )
                .run(context -> {
                    assertThat(context).hasSingleBean(CaptchaApplicationService.class);
                    assertThat(context).hasSingleBean(GraphCaptchaServiceImpl.class);
                    assertThat(context).doesNotHaveBean(SlideCaptchaServiceImpl.class);
                });
    }

    @Test
    void shouldNotLoadCaptchaBeansWhenDisabled() {
        contextRunner
                .withUserConfiguration(GraphCaptchaConfig.class)
                .withPropertyValues(
                        "vita.captcha.enabled=false",
                        "vita.captcha.captcha-type=slide"
                )
                .run(context -> {
                    assertThat(context).doesNotHaveBean(CaptchaApplicationService.class);
                    assertThat(context).doesNotHaveBean(GraphCaptchaServiceImpl.class);
                    assertThat(context).doesNotHaveBean(SlideCaptchaServiceImpl.class);
                });
    }

    @Test
    void shouldFailFastWhenCaptchaTypeIsInvalid() {
        contextRunner
                .withPropertyValues(
                        "vita.captcha.enabled=true",
                        "vita.captcha.captcha-type=unsupported"
                )
                .run(context -> assertThat(context.getStartupFailure()).isNotNull());
    }
}
