package com.vita.captcha.service;

import com.vita.captcha.common.enums.CaptchaTypeEnum;
import com.vita.captcha.config.property.CaptchaProperty;
import com.vita.captcha.config.property.SlideCaptchaProperty;
import com.vita.captcha.dto.CaptchaRequestDto;
import com.vita.captcha.dto.CaptchaVerifyRequestDto;
import com.vita.captcha.utils.CaptchaUtils;
import com.vita.captcha.utils.SlideCaptchaUtils;
import com.vita.captcha.vo.CaptchaResponseVo;
import com.vita.captcha.vo.SlideCaptchaResponseVo;
import com.vita.core.exception.ServiceException;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class SlideCaptchaServiceImplTest {

    @BeforeAll
    static void setHeadlessMode() {
        System.setProperty("java.awt.headless", "true");
    }

    @TempDir
    Path tempDir;

    @Test
    void shouldGenerateSlideCaptchaWithLocalBackgroundAndMappedUrl() throws Exception {
        Path localImage = createTestImage(tempDir.resolve("local-slide.png"));
        CaptchaProperty captchaProperty = createSlideProperty(CaptchaTypeEnum.SLIDE.getType());
        captchaProperty.getSlide().setBackImg(localImage.toString());
        captchaProperty.getSlide().setBackImgUrl("https://static.example.com/captcha/local-slide.png");
        captchaProperty.normalizeAndValidate();

        SlideCaptchaServiceImpl slideCaptchaService = new SlideCaptchaServiceImpl(captchaProperty);
        CaptchaApplicationService captchaApplicationService = new CaptchaApplicationService(
                captchaProperty,
                Map.of("slideCaptchaService", slideCaptchaService)
        );

        CaptchaResponseVo captchaResponseVo = captchaApplicationService.getCaptcha(new CaptchaRequestDto());
        assertEquals(CaptchaTypeEnum.SLIDE.getType(), captchaResponseVo.getCaptchaType());
        assertInstanceOf(SlideCaptchaResponseVo.class, captchaResponseVo.getCaptchaData());
        SlideCaptchaResponseVo slideCaptchaResponseVo = (SlideCaptchaResponseVo) captchaResponseVo.getCaptchaData();
        assertEquals("https://static.example.com/captcha/local-slide.png", slideCaptchaResponseVo.getBackgroundUrl());
        assertNotNull(slideCaptchaResponseVo.getBlockImg());

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

        verifyRequest.setCaptchaCode(String.valueOf(Integer.parseInt(captchaCode) + captchaProperty.getSlide().getDeviation() + 1));
        assertThrows(ServiceException.class, () -> captchaApplicationService.checkCaptcha(verifyRequest));
    }

    @Test
    void shouldGenerateAllSlideWithoutBlockImage() throws Exception {
        Path localImage = createTestImage(tempDir.resolve("all-slide.png"));
        CaptchaProperty captchaProperty = createSlideProperty(CaptchaTypeEnum.ALL_SLIDE.getType());
        captchaProperty.getSlide().setBackImg(localImage.toString());
        captchaProperty.getSlide().setBackImgUrl("https://static.example.com/captcha/all-slide.png");
        captchaProperty.normalizeAndValidate();

        SlideCaptchaServiceImpl slideCaptchaService = new SlideCaptchaServiceImpl(captchaProperty);
        CaptchaApplicationService captchaApplicationService = new CaptchaApplicationService(
                captchaProperty,
                Map.of("slideCaptchaService", slideCaptchaService)
        );

        CaptchaResponseVo captchaResponseVo = captchaApplicationService.getCaptcha(new CaptchaRequestDto());
        assertEquals(CaptchaTypeEnum.ALL_SLIDE.getType(), captchaResponseVo.getCaptchaType());
        SlideCaptchaResponseVo slideCaptchaResponseVo = (SlideCaptchaResponseVo) captchaResponseVo.getCaptchaData();
        assertNotNull(slideCaptchaResponseVo.getBackgroundUrl());
        assertNull(slideCaptchaResponseVo.getBlockImg());
    }

    @Test
    void shouldSupportRemoteBackgroundPath() throws Exception {
        SlideCaptchaProperty slideCaptchaProperty = new SlideCaptchaProperty();
        slideCaptchaProperty.setBackImg("https://static.example.com/captcha/%s.png");
        slideCaptchaProperty.setBackImgUrl("https://static.example.com/captcha/%s.png");

        assertEquals(
                "https://static.example.com/captcha/7.png",
                SlideCaptchaUtils.resolveAccessUrl(slideCaptchaProperty, 7)
        );
    }

    @Test
    void shouldFailWhenLocalBackgroundCannotMapToRemoteUrl() throws Exception {
        Path localImage = createTestImage(tempDir.resolve("no-public-url.png"));
        SlideCaptchaProperty slideCaptchaProperty = new SlideCaptchaProperty();
        slideCaptchaProperty.setBackImg(localImage.toString());
        slideCaptchaProperty.setBackImgUrl(null);

        assertThrows(ServiceException.class, () -> SlideCaptchaUtils.resolveBackground(slideCaptchaProperty));
    }

    @Test
    void shouldFailWhenBackgroundDoesNotExist() {
        SlideCaptchaProperty slideCaptchaProperty = new SlideCaptchaProperty();
        slideCaptchaProperty.setBackImg(tempDir.resolve("missing.png").toString());
        slideCaptchaProperty.setBackImgUrl("https://static.example.com/captcha/missing.png");

        assertThrows(ServiceException.class, () -> SlideCaptchaUtils.resolveBackground(slideCaptchaProperty));
    }

    private CaptchaProperty createSlideProperty(String captchaType) {
        CaptchaProperty captchaProperty = new CaptchaProperty();
        captchaProperty.setEnabled(true);
        captchaProperty.setCaptchaType(captchaType);
        captchaProperty.setAesSecret("vita123456789012");
        captchaProperty.setAesIv("vita123456789012");
        captchaProperty.setExpired(3);
        captchaProperty.normalizeAndValidate();
        return captchaProperty;
    }

    private Path createTestImage(Path imagePath) throws IOException {
        BufferedImage bufferedImage = new BufferedImage(320, 155, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics2D = bufferedImage.createGraphics();
        graphics2D.setColor(Color.ORANGE);
        graphics2D.fillRect(0, 0, 320, 155);
        graphics2D.setColor(Color.BLUE);
        graphics2D.fillRect(40, 20, 120, 80);
        graphics2D.dispose();
        ImageIO.write(bufferedImage, "png", imagePath.toFile());
        return imagePath;
    }
}
