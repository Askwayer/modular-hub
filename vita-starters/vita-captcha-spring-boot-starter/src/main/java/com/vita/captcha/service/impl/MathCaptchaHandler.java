package com.vita.captcha.service.impl;

import com.google.code.kaptcha.Producer;
import com.vita.captcha.dto.CaptchaVerifyRequest;
import com.vita.captcha.enums.CaptchaType;
import com.vita.captcha.model.CaptchaState;
import com.vita.captcha.properties.CaptchaProperties;
import com.vita.captcha.properties.MathCaptchaProperties;
import com.vita.captcha.service.CaptchaStore;
import com.vita.captcha.util.CaptchaImageUtils;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

/**
 * @version 1.0
 * @BelongsProject: vita
 * @BelongsPackage: com.vita.captcha.service.impl
 * @Author znk
 * @CreateTime 2026-03-20
 * @Description: 图形答案验证码处理器
 */
public class MathCaptchaHandler extends AbstractStatefulCaptchaHandler {

    private static final String ANSWER_KEY = "answer";
    private static final String EXPRESSION_KEY = "expression";

    private final MathCaptchaProperties properties;

    private final Producer producer;

    public MathCaptchaHandler(CaptchaProperties captchaProperties, MathCaptchaProperties properties,
                              CaptchaStore store, Producer producer) {
        super(CaptchaType.MATH, captchaProperties, store);
        this.properties = properties;
        this.producer = producer;
    }

    @Override
    protected CaptchaState buildState(String captchaId, LocalDateTime expireAt) {
        Expression expression = buildExpression();
        CaptchaState state = createState(captchaId, expireAt);
        state.getAttributes().put(ANSWER_KEY, expression.answer());
        state.getAttributes().put(EXPRESSION_KEY, expression.text());
        return state;
    }

    @Override
    protected Object buildPayload(CaptchaState state) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("imageBase64", CaptchaImageUtils.toPngDataUri(producer.createImage(String.valueOf(state.getAttributes().get(EXPRESSION_KEY)))));
        payload.put("width", properties.getWidth());
        payload.put("height", properties.getHeight());
        return payload;
    }

    @Override
    protected VerificationOutcome doVerify(CaptchaState state, CaptchaVerifyRequest request) {
        String submitted = requireText(request.getAnswer(), "图形答案验证码不能为空");
        String expected = String.valueOf(state.getAttributes().get(ANSWER_KEY));
        return expected.equals(submitted) ? VerificationOutcome.success("验证码校验通过")
                : VerificationOutcome.failure("图形答案验证码错误");
    }

    private Expression buildExpression() {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        int first = random.nextInt(properties.getMinNumber(), properties.getMaxNumber() + 1);
        int second = random.nextInt(properties.getMinNumber(), properties.getMaxNumber() + 1);
        String operator = resolveOperators().get(random.nextInt(resolveOperators().size()));

        int answer;
        String normalizedOperator;
        switch (operator) {
            case "-" -> {
                if (second > first) {
                    int temp = first;
                    first = second;
                    second = temp;
                }
                answer = first - second;
                normalizedOperator = "-";
            }
            case "*", "x", "X" -> {
                answer = first * second;
                normalizedOperator = "x";
            }
            default -> {
                answer = first + second;
                normalizedOperator = "+";
            }
        }
        return new Expression(first + " " + normalizedOperator + " " + second + " = ?", String.valueOf(answer));
    }

    private List<String> resolveOperators() {
        List<String> operators = Arrays.stream(properties.getOperators().split(","))
                .map(String::trim)
                .filter(StringUtils::hasText)
                .toList();
        return operators.isEmpty() ? List.of("+") : operators;
    }

    private record Expression(String text, String answer) {
    }
}
