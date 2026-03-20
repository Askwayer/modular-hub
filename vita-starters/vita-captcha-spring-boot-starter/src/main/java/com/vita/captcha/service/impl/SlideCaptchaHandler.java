package com.vita.captcha.service.impl;

import com.vita.captcha.dto.CaptchaVerifyRequest;
import com.vita.captcha.enums.CaptchaType;
import com.vita.captcha.model.CaptchaState;
import com.vita.captcha.properties.CaptchaProperties;
import com.vita.captcha.properties.SlideCaptchaProperties;
import com.vita.captcha.service.CaptchaStore;
import com.vita.captcha.util.CaptchaImageUtils;

import java.time.LocalDateTime;
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
 * @Description: 滑动验证码处理器
 */
public class SlideCaptchaHandler extends AbstractStatefulCaptchaHandler {

    private static final String TARGET_X_KEY = "targetX";

    private final SlideCaptchaProperties properties;

    private final String trackTemplate = CaptchaImageUtils.loadTemplate("captcha/templates/slide-track.svg");

    public SlideCaptchaHandler(CaptchaProperties captchaProperties, SlideCaptchaProperties properties, CaptchaStore store) {
        super(CaptchaType.SLIDE, captchaProperties, store);
        this.properties = properties;
    }

    @Override
    protected CaptchaState buildState(String captchaId, LocalDateTime expireAt) {
        int minTarget = Math.max(properties.getHandleWidth() + 24, 70);
        int maxTarget = Math.max(minTarget + 1, properties.getWidth() - properties.getHandleWidth() - 16);
        int targetX = maxTarget <= minTarget ? minTarget : ThreadLocalRandom.current().nextInt(minTarget, maxTarget);

        CaptchaState state = createState(captchaId, expireAt);
        state.getAttributes().put(TARGET_X_KEY, targetX);
        return state;
    }

    @Override
    protected Object buildPayload(CaptchaState state) {
        Map<String, String> variables = new LinkedHashMap<>();
        variables.put("width", String.valueOf(properties.getWidth()));
        variables.put("height", String.valueOf(properties.getHeight()));
        variables.put("trackWidth", String.valueOf(properties.getWidth() - 28));
        variables.put("goalX", String.valueOf(state.getAttributes().get(TARGET_X_KEY)));
        variables.put("trackY", String.valueOf((properties.getHeight() / 2) - 8));
        variables.put("trackColor", toRgb(properties.getTrackColor()));
        variables.put("goalColor", toRgb(properties.getGoalColor()));
        String track = CaptchaImageUtils.renderTemplate(trackTemplate, variables);

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("trackImage", CaptchaImageUtils.toSvgDataUri(track));
        payload.put("width", properties.getWidth());
        payload.put("height", properties.getHeight());
        payload.put("handleWidth", properties.getHandleWidth());
        payload.put("minTrackPoints", properties.getMinTrackPoints());
        return payload;
    }

    @Override
    protected VerificationOutcome doVerify(CaptchaState state, CaptchaVerifyRequest request) {
        int submittedOffset = requireOffset(request.getOffset());
        List<Integer> track = request.getTrack();
        if (!isTrackValid(track, submittedOffset)) {
            return VerificationOutcome.failure("拖动轨迹不合法");
        }

        int targetX = (Integer) state.getAttributes().get(TARGET_X_KEY);
        return Math.abs(submittedOffset - targetX) <= properties.getTolerance()
                ? VerificationOutcome.success("验证码校验通过")
                : VerificationOutcome.failure("滑动验证码错误");
    }

    private boolean isTrackValid(List<Integer> track, int offset) {
        if (track == null || track.size() < properties.getMinTrackPoints()) {
            return false;
        }
        int previous = -1;
        for (Integer point : track) {
            if (point == null || point < 0) {
                return false;
            }
            if (previous > point) {
                return false;
            }
            previous = point;
        }
        return Math.abs(previous - offset) <= properties.getTolerance();
    }

    private String toRgb(String color) {
        return "rgb(" + color + ")";
    }
}
