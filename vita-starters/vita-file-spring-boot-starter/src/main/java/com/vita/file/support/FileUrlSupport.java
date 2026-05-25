package com.vita.file.support;

import cn.hutool.core.text.CharSequenceUtil;
import org.springframework.stereotype.Component;

/**
 * 文件访问 URL 组装支持。
 */
@Component
public class FileUrlSupport {

    public String resolveBaseUrl(String publicBaseUrl, String privateBaseUrl, boolean isPublic) {
        if (isPublic) {
            return normalizeBaseUrl(publicBaseUrl);
        }
        return normalizeBaseUrl(CharSequenceUtil.isNotBlank(privateBaseUrl) ? privateBaseUrl : publicBaseUrl);
    }

    public String join(String baseUrl, String... segments) {
        StringBuilder builder = new StringBuilder(normalizeBaseUrl(baseUrl));
        for (String segment : segments) {
            if (CharSequenceUtil.isBlank(segment)) {
                continue;
            }
            builder.append('/');
            builder.append(trimSlashes(segment));
        }
        return builder.toString();
    }

    public String normalizeBaseUrl(String baseUrl) {
        if (CharSequenceUtil.isBlank(baseUrl)) {
            return "";
        }
        String normalized = baseUrl.trim();
        if (!normalized.startsWith("http://") && !normalized.startsWith("https://")) {
            normalized = "https://" + normalized;
        }
        while (normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        return normalized;
    }

    public String trimSlashes(String value) {
        String result = value;
        while (result.startsWith("/")) {
            result = result.substring(1);
        }
        while (result.endsWith("/")) {
            result = result.substring(0, result.length() - 1);
        }
        return result;
    }
}
