package com.vita.hot.support;

import com.vita.core.exception.GlobalErrorCode;
import com.vita.core.exception.ServiceException;
import com.vita.hot.property.HotProperty;
import com.vita.hot.vo.HotSourceSnapshotVo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.*;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

/**
 * @BelongsProject: be-vita
 * @BelongsPackage: com.vita.hot.support
 * @Author: Codex
 * @CreateTime: 2026-05-11
 * @Description: 热榜 Python 服务客户端
 * @Version: 1.0
 */
@Component
public class HotEngineClient {

    private static final Logger LOGGER = LoggerFactory.getLogger(HotEngineClient.class);

    private final HotProperty hotProperty;
    private final RestTemplate restTemplate;

    public HotEngineClient(HotProperty hotProperty) {
        this.hotProperty = hotProperty;
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        int timeoutMs = resolveTimeoutMs(hotProperty);
        requestFactory.setConnectTimeout(timeoutMs);
        requestFactory.setReadTimeout(timeoutMs);
        this.restTemplate = new RestTemplate(requestFactory);
    }

    /**
     * 调用 Python 服务获取单源热榜快照。
     *
     * @param sourceCode 源编码
     * @return 快照结果
     */
    public HotSourceSnapshotVo fetchSourceSnapshot(String sourceCode) {
        HotProperty.Python python = hotProperty.getPython();
        if (python == null || !StringUtils.hasText(python.getBaseUrl()) || !StringUtils.hasText(python.getApiKey())) {
            throw new ServiceException(GlobalErrorCode.SERVICE_UNAVAILABLE.getCode(), "热榜抓取服务未配置");
        }

        String url = buildSourceUrl(python.getBaseUrl(), sourceCode);
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(python.getApiKey().trim());
        headers.setAccept(MediaType.parseMediaTypes(MediaType.APPLICATION_JSON_VALUE));
        HttpEntity<Void> requestEntity = new HttpEntity<>(headers);

        try {
            ResponseEntity<HotSourceSnapshotVo> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    requestEntity,
                    HotSourceSnapshotVo.class
            );
            HotSourceSnapshotVo body = response.getBody();
            if (body == null || !StringUtils.hasText(body.getSourceCode())) {
                throw new ServiceException(GlobalErrorCode.SERVICE_UNAVAILABLE.getCode(), "热榜抓取服务返回空数据");
            }
            return body;
        } catch (ResourceAccessException ex) {
            LOGGER.warn("热榜抓取服务访问失败，sourceCode={}", sourceCode, ex);
            throw new ServiceException(GlobalErrorCode.GATEWAY_TIMEOUT.getCode(), "热榜抓取服务响应超时");
        } catch (HttpStatusCodeException ex) {
            LOGGER.warn("热榜抓取服务返回异常，sourceCode={}, status={}", sourceCode, ex.getStatusCode(), ex);
            if (ex.getStatusCode() == HttpStatus.UNAUTHORIZED || ex.getStatusCode() == HttpStatus.FORBIDDEN) {
                throw new ServiceException(GlobalErrorCode.SERVICE_UNAVAILABLE.getCode(), "热榜抓取服务鉴权失败");
            }
            throw new ServiceException(
                    GlobalErrorCode.SERVICE_UNAVAILABLE.getCode(),
                    "热榜抓取服务调用失败"
            );
        } catch (ServiceException ex) {
            throw ex;
        } catch (Exception ex) {
            LOGGER.warn("热榜抓取服务处理失败，sourceCode={}", sourceCode, ex);
            throw new ServiceException(GlobalErrorCode.SERVICE_UNAVAILABLE.getCode(), "热榜抓取服务不可用");
        }
    }

    private String buildSourceUrl(String baseUrl, String sourceCode) {
        String normalizedBaseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        return normalizedBaseUrl + "/api/v1/hot/" + sourceCode;
    }

    private int resolveTimeoutMs(HotProperty property) {
        HotProperty.Python python = property.getPython();
        if (python == null || python.getTimeoutMs() == null || python.getTimeoutMs() <= 0) {
            return 8000;
        }
        return python.getTimeoutMs();
    }
}
