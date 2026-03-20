package com.vita.captcha.service.impl;

import com.google.code.kaptcha.Producer;
import com.google.code.kaptcha.impl.DefaultKaptcha;
import com.google.code.kaptcha.util.Config;
import com.vita.captcha.dto.CaptchaVerifyRequest;
import com.vita.captcha.enums.CaptchaType;
import com.vita.captcha.model.CaptchaChallenge;
import com.vita.captcha.model.CaptchaState;
import com.vita.captcha.model.CaptchaVerifyResult;
import com.vita.captcha.properties.CaptchaProperties;
import com.vita.captcha.properties.CharCaptchaProperties;
import com.vita.captcha.properties.MathCaptchaProperties;
import com.vita.captcha.properties.SlideCaptchaProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @version 1.0
 * @BelongsProject: vita
 * @BelongsPackage: com.vita.captcha.service.impl
 * @Author znk
 * @CreateTime 2026-03-20
 * @Description: 验证码处理器测试
 */
class CaptchaHandlerTest {

    private CaptchaProperties captchaProperties;

    private CharCaptchaProperties charProperties;

    private MathCaptchaProperties mathProperties;

    private SlideCaptchaProperties slideProperties;

    private InMemoryCaptchaStore store;

    @BeforeEach
    void setUp() {
        captchaProperties = new CaptchaProperties();
        captchaProperties.setEnabled(true);
        captchaProperties.setStore(CaptchaProperties.StoreType.IN_MEMORY);

        charProperties = new CharCaptchaProperties();
        mathProperties = new MathCaptchaProperties();
        slideProperties = new SlideCaptchaProperties();

        store = new InMemoryCaptchaStore(captchaProperties);
    }

    @Test
    void mathCaptchaShouldVerifyAndConsumeChallenge() {
        MathCaptchaHandler handler = new MathCaptchaHandler(captchaProperties, mathProperties, store, createProducer(
                mathProperties.getWidth(), mathProperties.getHeight(), null, null,
                mathProperties.getBorder(), mathProperties.getFontNames(), mathProperties.getFontSize(),
                mathProperties.getFontColor(), mathProperties.getNoiseColor(),
                mathProperties.getBackgroundFrom(), mathProperties.getBackgroundTo()));
        CaptchaChallenge challenge = handler.generate();
        String answer = String.valueOf(loadState(challenge.getCaptchaId()).getAttributes().get("answer"));

        CaptchaVerifyRequest verifyRequest = new CaptchaVerifyRequest();
        verifyRequest.setCaptchaId(challenge.getCaptchaId());
        verifyRequest.setAnswer(answer);

        CaptchaVerifyResult result = handler.verify(verifyRequest);

        assertThat(result.isSuccess()).isTrue();
        assertThat(store.load(challenge.getCaptchaId())).isEmpty();
    }

    @Test
    void charCaptchaShouldBeCaseSensitive() {
        CharCaptchaHandler handler = new CharCaptchaHandler(captchaProperties, charProperties, store, createProducer(
                charProperties.getWidth(), charProperties.getHeight(), charProperties.getLength(), charProperties.getCharString(),
                charProperties.getBorder(), charProperties.getFontNames(), charProperties.getFontSize(),
                charProperties.getFontColor(), charProperties.getNoiseColor(),
                charProperties.getBackgroundFrom(), charProperties.getBackgroundTo()));
        CaptchaChallenge challenge = null;
        String answer = null;
        for (int index = 0; index < 8; index++) {
            challenge = handler.generate();
            answer = String.valueOf(loadState(challenge.getCaptchaId()).getAttributes().get("answer"));
            if (answer.chars().anyMatch(Character::isLetter)) {
                break;
            }
        }

        CaptchaVerifyRequest verifyRequest = new CaptchaVerifyRequest();
        verifyRequest.setCaptchaId(challenge.getCaptchaId());
        verifyRequest.setAnswer(swapCase(answer));

        CaptchaVerifyResult result = handler.verify(verifyRequest);

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getRemainingAttempts()).isEqualTo(captchaProperties.getMaxFailures() - 1);
    }

    @Test
    void slideCaptchaShouldRejectIllegalTrack() {
        SlideCaptchaHandler handler = new SlideCaptchaHandler(captchaProperties, slideProperties, store);
        CaptchaChallenge challenge = handler.generate();
        int targetX = (Integer) loadState(challenge.getCaptchaId()).getAttributes().get("targetX");

        CaptchaVerifyRequest verifyRequest = new CaptchaVerifyRequest();
        verifyRequest.setCaptchaId(challenge.getCaptchaId());
        verifyRequest.setOffset(targetX);
        verifyRequest.setTrack(List.of(10, 8, targetX));

        CaptchaVerifyResult result = handler.verify(verifyRequest);

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getMessage()).contains("轨迹");
    }

    @Test
    void facadeShouldResolveTypeFromStateWhenVerifying() {
        CharCaptchaHandler charHandler = new CharCaptchaHandler(captchaProperties, charProperties, store, createProducer(
                charProperties.getWidth(), charProperties.getHeight(), charProperties.getLength(), charProperties.getCharString(),
                charProperties.getBorder(), charProperties.getFontNames(), charProperties.getFontSize(),
                charProperties.getFontColor(), charProperties.getNoiseColor(),
                charProperties.getBackgroundFrom(), charProperties.getBackgroundTo()));
        SlideCaptchaHandler slideHandler = new SlideCaptchaHandler(captchaProperties, slideProperties, store);

        captchaProperties.setCaptchaType(CaptchaType.CHAR);
        DefaultCaptchaFacade facade = new DefaultCaptchaFacade(captchaProperties, store, List.of(charHandler, slideHandler));
        CaptchaChallenge challenge = facade.generate();
        String answer = String.valueOf(loadState(challenge.getCaptchaId()).getAttributes().get("answer"));

        captchaProperties.setCaptchaType(CaptchaType.SLIDE);
        CaptchaVerifyRequest verifyRequest = new CaptchaVerifyRequest();
        verifyRequest.setCaptchaId(challenge.getCaptchaId());
        verifyRequest.setAnswer(answer);

        CaptchaVerifyResult result = facade.verify(verifyRequest);

        assertThat(result.isSuccess()).isTrue();
    }

    private Producer createProducer(int width, int height, Integer length, String charString, String border,
                                    String fontNames, int fontSize, String fontColor, String noiseColor,
                                    String backgroundFrom, String backgroundTo) {
        Properties properties = new Properties();
        properties.setProperty("kaptcha.border", border);
        properties.setProperty("kaptcha.image.width", String.valueOf(width));
        properties.setProperty("kaptcha.image.height", String.valueOf(height));
        properties.setProperty("kaptcha.textproducer.font.names", fontNames);
        properties.setProperty("kaptcha.textproducer.font.size", String.valueOf(fontSize));
        properties.setProperty("kaptcha.textproducer.font.color", fontColor);
        properties.setProperty("kaptcha.noise.color", noiseColor);
        properties.setProperty("kaptcha.background.clear.from", backgroundFrom);
        properties.setProperty("kaptcha.background.clear.to", backgroundTo);
        properties.setProperty("kaptcha.obscurificator.impl", "com.google.code.kaptcha.impl.ShadowGimpy");
        if (length != null) {
            properties.setProperty("kaptcha.textproducer.char.length", String.valueOf(length));
        }
        if (charString != null) {
            properties.setProperty("kaptcha.textproducer.char.string", charString);
        }
        DefaultKaptcha producer = new DefaultKaptcha();
        producer.setConfig(new Config(properties));
        return producer;
    }

    private CaptchaState loadState(String captchaId) {
        Optional<CaptchaState> state = store.load(captchaId);
        assertThat(state).isPresent();
        return state.get();
    }

    private String swapCase(String value) {
        StringBuilder builder = new StringBuilder(value.length());
        for (char current : value.toCharArray()) {
            if (Character.isUpperCase(current)) {
                builder.append(Character.toLowerCase(current));
            } else if (Character.isLowerCase(current)) {
                builder.append(Character.toUpperCase(current));
            } else {
                builder.append(current);
            }
        }
        return builder.toString().toUpperCase(Locale.ROOT).equals(value.toUpperCase(Locale.ROOT))
                ? value.toLowerCase(Locale.ROOT) : builder.toString();
    }
}
