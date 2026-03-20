package com.vita.captcha.service;

import com.vita.captcha.common.enums.CaptchaTypeEnum;
import com.vita.captcha.config.GraphCaptchaConfig;
import com.vita.captcha.config.property.CaptchaProperty;
import com.vita.captcha.dto.CaptchaRequestDto;
import com.vita.captcha.dto.CaptchaVerifyRequestDto;
import com.vita.captcha.utils.CaptchaUtils;
import com.vita.captcha.vo.CaptchaResponseVo;
import com.vita.core.exception.ServiceException;
import com.vita.utils.AesUtils;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class GraphCaptchaServiceImplTest {

    @BeforeAll
    static void setHeadlessMode() {
        System.setProperty("java.awt.headless", "true");
    }

    @Test
    void shouldGenerateAndValidateCharCaptcha() {
        CaptchaProperty captchaProperty = createCaptchaProperty(CaptchaTypeEnum.CHAR.getType());
        GraphCaptchaConfig graphCaptchaConfig = new GraphCaptchaConfig();
        GraphCaptchaServiceImpl graphCaptchaService = new GraphCaptchaServiceImpl(
                captchaProperty,
                graphCaptchaConfig.captchaProducer(),
                graphCaptchaConfig.captchaProducerMath()
        );
        CaptchaApplicationService captchaApplicationService = new CaptchaApplicationService(
                captchaProperty,
                Map.of("graphCaptchaService", graphCaptchaService)
        );

        CaptchaResponseVo captchaResponseVo = captchaApplicationService.getCaptcha(new CaptchaRequestDto());

        assertEquals(CaptchaTypeEnum.CHAR.getType(), captchaResponseVo.getCaptchaType());
        assertNotNull(captchaResponseVo.getCaptchaImg());
        assertTrue(captchaResponseVo.getCaptchaImg().startsWith("data:image/"));
        assertNotNull(captchaResponseVo.getEncryptData());

        String captchaCode = CaptchaUtils.aesDecrypt(
                captchaResponseVo.getUuid(),
                captchaResponseVo.getEncryptData(),
                captchaProperty
        );
        CaptchaVerifyRequestDto verifyRequest = new CaptchaVerifyRequestDto();
        verifyRequest.setUuid(captchaResponseVo.getUuid());
        verifyRequest.setCaptchaCode(captchaCode);
        verifyRequest.setEncryptData(captchaResponseVo.getEncryptData());
        assertDoesNotThrow(() -> captchaApplicationService.checkCaptcha(verifyRequest));
    }

    @Test
    void shouldRejectWrongCaseForCharCaptcha() {
        CaptchaProperty captchaProperty = createCaptchaProperty(CaptchaTypeEnum.CHAR.getType());
        GraphCaptchaConfig graphCaptchaConfig = new GraphCaptchaConfig();
        GraphCaptchaServiceImpl graphCaptchaService = new GraphCaptchaServiceImpl(
                captchaProperty,
                graphCaptchaConfig.captchaProducer(),
                graphCaptchaConfig.captchaProducerMath()
        );
        String encryptData = CaptchaUtils.aesEncode("user-1", "AbCd", captchaProperty);

        assertDoesNotThrow(() -> graphCaptchaService.checkCaptcha("user-1", "AbCd", encryptData));
        assertThrows(ServiceException.class, () -> graphCaptchaService.checkCaptcha("user-1", "abcd", encryptData));
    }

    @Test
    void shouldGenerateAndValidateMathCaptcha() {
        CaptchaProperty captchaProperty = createCaptchaProperty(CaptchaTypeEnum.MATH.getType());
        GraphCaptchaConfig graphCaptchaConfig = new GraphCaptchaConfig();
        GraphCaptchaServiceImpl graphCaptchaService = new GraphCaptchaServiceImpl(
                captchaProperty,
                graphCaptchaConfig.captchaProducer(),
                graphCaptchaConfig.captchaProducerMath()
        );
        CaptchaApplicationService captchaApplicationService = new CaptchaApplicationService(
                captchaProperty,
                Map.of("graphCaptchaService", graphCaptchaService)
        );

        CaptchaResponseVo captchaResponseVo = captchaApplicationService.getCaptcha(new CaptchaRequestDto());
        String captchaCode = CaptchaUtils.aesDecrypt(
                captchaResponseVo.getUuid(),
                captchaResponseVo.getEncryptData(),
                captchaProperty
        );

        CaptchaVerifyRequestDto verifyRequest = new CaptchaVerifyRequestDto();
        verifyRequest.setUuid(captchaResponseVo.getUuid());
        verifyRequest.setCaptchaCode(captchaCode);
        verifyRequest.setEncryptData(captchaResponseVo.getEncryptData());
        assertDoesNotThrow(() -> captchaApplicationService.checkCaptcha(verifyRequest));

        verifyRequest.setCaptchaCode(String.valueOf(Integer.parseInt(captchaCode) + 1));
        assertThrows(ServiceException.class, () -> captchaApplicationService.checkCaptcha(verifyRequest));
    }

    @Test
    void shouldRejectExpiredCaptcha() {
        CaptchaProperty captchaProperty = createCaptchaProperty(CaptchaTypeEnum.CHAR.getType());
        GraphCaptchaConfig graphCaptchaConfig = new GraphCaptchaConfig();
        GraphCaptchaServiceImpl graphCaptchaService = new GraphCaptchaServiceImpl(
                captchaProperty,
                graphCaptchaConfig.captchaProducer(),
                graphCaptchaConfig.captchaProducerMath()
        );
        long expiredAt = System.currentTimeMillis() / 1000 - 1;
        String encryptData = AesUtils.encryptByCbcMode(
                captchaProperty.getCaptchaType() + ":user-1:AbCd:" + expiredAt,
                captchaProperty.getAesSecret(),
                captchaProperty.getAesIv()
        );

        assertThrows(ServiceException.class, () -> graphCaptchaService.checkCaptcha("user-1", "AbCd", encryptData));
    }

    private CaptchaProperty createCaptchaProperty(String captchaType) {
        CaptchaProperty captchaProperty = new CaptchaProperty();
        captchaProperty.setEnabled(true);
        captchaProperty.setCaptchaType(captchaType);
        captchaProperty.setAesSecret("vita123456789012");
        captchaProperty.setAesIv("vita123456789012");
        captchaProperty.setExpired(3);
        captchaProperty.normalizeAndValidate();
        return captchaProperty;
    }
}
