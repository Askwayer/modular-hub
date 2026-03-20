package com.vita.captcha.util;

import cn.hutool.core.io.IoUtil;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;

/**
 * @version 1.0
 * @BelongsProject: vita
 * @BelongsPackage: com.vita.captcha.util
 * @Author znk
 * @CreateTime 2026-03-20
 * @Description: 验证码图片工具
 */
public final class CaptchaImageUtils {

    private CaptchaImageUtils() {
    }

    /**
     * 将 PNG 图片转换成 Data URI。
     *
     * @param image PNG 图片
     * @return Data URI
     */
    public static String toPngDataUri(BufferedImage image) {
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            ImageIO.write(image, "png", outputStream);
            return toDataUri("image/png", outputStream.toByteArray());
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to build captcha image", ex);
        }
    }

    /**
     * 加载 classpath 模板。
     *
     * @param location 模板路径
     * @return 模板内容
     */
    public static String loadTemplate(String location) {
        try (InputStream inputStream = CaptchaImageUtils.class.getClassLoader().getResourceAsStream(location)) {
            if (inputStream == null) {
                throw new IllegalArgumentException("Template not found: " + location);
            }
            return IoUtil.read(inputStream, StandardCharsets.UTF_8);
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to load template " + location, ex);
        }
    }

    /**
     * 渲染 SVG 模板变量。
     *
     * @param template 模板内容
     * @param variables 模板变量
     * @return 渲染后的内容
     */
    public static String renderTemplate(String template, Map<String, String> variables) {
        String rendered = template;
        for (Map.Entry<String, String> entry : variables.entrySet()) {
            rendered = rendered.replace("${" + entry.getKey() + "}", entry.getValue());
        }
        return rendered;
    }

    /**
     * 将 SVG 内容转换成 Data URI。
     *
     * @param svgContent SVG 内容
     * @return Data URI
     */
    public static String toSvgDataUri(String svgContent) {
        return toDataUri("image/svg+xml", svgContent.getBytes(StandardCharsets.UTF_8));
    }

    private static String toDataUri(String mimeType, byte[] bytes) {
        return "data:" + mimeType + ";base64," + Base64.getEncoder().encodeToString(bytes);
    }
}
