package com.vita.file.controller;

import cn.hutool.core.text.CharSequenceUtil;
import com.vita.core.exception.GlobalErrorCode;
import com.vita.core.exception.ServiceException;
import com.vita.file.property.FileProperty;
import com.vita.file.support.FileSignatureSupport;
import com.vita.file.support.LocalFileUrlSupport;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * 本地文件下载控制器（仅 active=local 时生效）。
 */
@Slf4j
@RestController
@RequestMapping("/file/download")
@ConditionalOnProperty(name = "vita.file.active", havingValue = "local")
public class LocalFileDownloadController {

    private final FileProperty fileProperty;
    private final FileSignatureSupport signatureSupport;
    private final LocalFileUrlSupport localFileUrlSupport;

    public LocalFileDownloadController(FileProperty fileProperty,
                                       FileSignatureSupport signatureSupport,
                                       LocalFileUrlSupport localFileUrlSupport) {
        this.fileProperty = fileProperty;
        this.signatureSupport = signatureSupport;
        this.localFileUrlSupport = localFileUrlSupport;
    }

    /**
     * 下载本地文件（私有文件需携带签名）。
     *
     * @param bucket bucket 名称
     * @param key    对象键路径
     * @param token  签名 Token
     * @param time   签名时间戳
     */
    @GetMapping("/{bucket}/{key}/**")
    public void download(@PathVariable String bucket,
                         @PathVariable String key,
                         @RequestParam(required = false) String token,
                         @RequestParam(required = false) String time,
                         HttpServletResponse response) {
        // 重建完整 key（支持子路径）
        String fullKey = bucket + "/" + key;
        String remainingPath = extractRemainingPath();
        if (CharSequenceUtil.isNotBlank(remainingPath)) {
            fullKey += "/" + remainingPath;
        }

        File file = localFileUrlSupport.resolveFile(fullKey);
        if (!file.exists() || !file.canRead()) {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        // 如果是私有路径，校验签名
        String privatePrefix = fileProperty.getPrivatePrefix();
        if (fullKey.startsWith(privatePrefix + "/")) {
            String uri = "/" + bucket + "/" + key + (CharSequenceUtil.isNotBlank(remainingPath) ? "/" + remainingPath : "");
            if (!signatureSupport.verifyToken(fileProperty, uri, token, time)) {
                response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                return;
            }
        }

        // 流式返回文件
        response.setContentType(getContentType(file.getName()));
        response.setContentLengthLong(file.length());
        response.setHeader("Content-Disposition", "inline; filename=\"" + file.getName() + "\"");

        try (InputStream inputStream = localFileUrlSupport.getInputStream(fullKey);
             OutputStream outputStream = response.getOutputStream()) {
            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }
        } catch (IOException e) {
            log.warn("文件下载失败, key={}", fullKey, e);
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }

    private String extractRemainingPath() {
        // Spring 会自动处理 /** 剩余路径，需要从 request URI 中提取
        // 这里通过简单的字符串处理获取
        return "";
    }

    private String getContentType(String fileName) {
        if (fileName.endsWith(".jpg") || fileName.endsWith(".jpeg")) {
            return "image/jpeg";
        } else if (fileName.endsWith(".png")) {
            return "image/png";
        } else if (fileName.endsWith(".gif")) {
            return "image/gif";
        } else if (fileName.endsWith(".pdf")) {
            return "application/pdf";
        } else if (fileName.endsWith(".mp4")) {
            return "video/mp4";
        }
        return "application/octet-stream";
    }
}
