package com.vita.hot.support;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.text.CharSequenceUtil;
import cn.hutool.core.util.StrUtil;
import com.vita.core.exception.GlobalErrorCode;
import com.vita.core.exception.ServiceException;
import com.vita.hot.property.HotDailyApiProperty;
import com.vita.hot.vo.HotSourceRegistryVo;
import com.vita.hot.vo.HotSourceSnapshotVo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.*;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
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
    private static final int DEFAULT_TIMEOUT_MS = 8000;
    private static final String MISSING_CONFIG_MESSAGE =
            "热榜抓取服务未配置，请检查 vita.hot.daily-hot-api.base-url 和 vita.hot.daily-hot-api.api-key";

    private final HotDailyApiProperty hotDailyApiProperty;
    private final RestTemplate restTemplate;

    public HotEngineClient(HotDailyApiProperty hotDailyApiProperty) {
        this.hotDailyApiProperty = hotDailyApiProperty;
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        int timeoutMs = resolveTimeoutMs(this.hotDailyApiProperty);
        requestFactory.setConnectTimeout(timeoutMs);
        requestFactory.setReadTimeout(timeoutMs);
        this.restTemplate = new RestTemplate(requestFactory);
    }

    /**
     * 拉取指定热榜源的最新快照。
     */
    public HotSourceSnapshotVo fetchSourceSnapshot(String sourceCode) {
        HotSourceSnapshotVo body = exchange(
                buildSourceUrl(requireHotDailyApiProperty(), sourceCode),
                HotSourceSnapshotVo.class,
                sourceCode
        );
        if (body == null || StrUtil.isBlank(body.getSourceCode())) {
            throw new ServiceException(GlobalErrorCode.SERVICE_UNAVAILABLE.getCode(), "热榜抓取服务返回空数据");
        }
        return body;
    }

    /**
     * 拉取当前可用的热榜源目录。
     */
    public HotSourceRegistryVo fetchSourceRegistry() {
        HotSourceRegistryVo body = exchange(
                buildSourcesUrl(requireHotDailyApiProperty()),
                HotSourceRegistryVo.class,
                "sources"
        );
        if (body == null || CollUtil.isEmpty(body.getSources())) {
            throw new ServiceException(GlobalErrorCode.SERVICE_UNAVAILABLE.getCode(), "热榜源目录返回空数据");
        }
        return body;
    }

    private <T> T exchange(String url, Class<T> responseType, String target) {
        HttpEntity<Void> requestEntity = new HttpEntity<>(buildHeaders());
        try {
            ResponseEntity<T> response = restTemplate.exchange(url, HttpMethod.GET, requestEntity, responseType);
            return response.getBody();
        } catch (ResourceAccessException ex) {
            LOGGER.warn("热榜抓取服务访问失败，target={}", target, ex);
            throw new ServiceException(GlobalErrorCode.GATEWAY_TIMEOUT.getCode(), "热榜抓取服务响应超时");
        } catch (HttpStatusCodeException ex) {
            LOGGER.warn("热榜抓取服务返回异常，target={}, status={}", target, ex.getStatusCode(), ex);
            if (ex.getStatusCode() == HttpStatus.UNAUTHORIZED || ex.getStatusCode() == HttpStatus.FORBIDDEN) {
                throw new ServiceException(GlobalErrorCode.SERVICE_UNAVAILABLE.getCode(), "热榜抓取服务鉴权失败");
            }
            throw new ServiceException(GlobalErrorCode.SERVICE_UNAVAILABLE.getCode(), "热榜抓取服务调用失败");
        } catch (ServiceException ex) {
            throw ex;
        } catch (Exception ex) {
            LOGGER.warn("热榜抓取服务处理失败，target={}", target, ex);
            throw new ServiceException(GlobalErrorCode.SERVICE_UNAVAILABLE.getCode(), "热榜抓取服务不可用");
        }
    }

    private HttpHeaders buildHeaders() {
        HotDailyApiProperty property = requireHotDailyApiProperty();
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(StrUtil.trim(property.getApiKey()));
        headers.setAccept(MediaType.parseMediaTypes(MediaType.APPLICATION_JSON_VALUE));
        return headers;
    }

    /**
     * 校验热榜抓取服务配置，避免空地址或空密钥导致请求失败。
     */
    private HotDailyApiProperty requireHotDailyApiProperty() {
        if (hotDailyApiProperty == null
                || CharSequenceUtil.isBlank(hotDailyApiProperty.getBaseUrl())
                || CharSequenceUtil.isBlank(hotDailyApiProperty.getApiKey())) {
            throw new ServiceException(GlobalErrorCode.SERVICE_UNAVAILABLE.getCode(), MISSING_CONFIG_MESSAGE);
        }
        return hotDailyApiProperty;
    }

    private String buildSourceUrl(HotDailyApiProperty property, String sourceCode) {
        return normalizeBaseUrl(property.getBaseUrl()) + "/api/v1/hot/" + sourceCode;
    }

    private String buildSourcesUrl(HotDailyApiProperty property) {
        return normalizeBaseUrl(property.getBaseUrl()) + "/api/v1/hot/sources";
    }

    private String normalizeBaseUrl(String baseUrl) {
        return StrUtil.removeSuffix(StrUtil.trim(baseUrl), "/");
    }

    private int resolveTimeoutMs(HotDailyApiProperty property) {
        if (property == null || property.getTimeoutMs() == null || property.getTimeoutMs() <= 0) {
            return DEFAULT_TIMEOUT_MS;
        }
        return property.getTimeoutMs();
    }
}
