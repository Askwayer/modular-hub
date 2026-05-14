package com.vita.hot.support;

import cn.hutool.core.text.CharSequenceUtil;
import cn.hutool.http.HttpException;
import cn.hutool.http.HttpUtil;
import cn.hutool.json.JSONException;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.vita.core.exception.GlobalErrorCode;
import com.vita.core.exception.ServiceException;
import com.vita.hot.property.HotDailyApiProperty;
import com.vita.hot.vo.HotEngineHealthVo;
import com.vita.hot.vo.HotSourceSnapshotVo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

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

    private final HotDailyApiProperty hotDailyApiProperty;

    /**
     * 构造函数注入热榜 API 配置，并初始化 RestTemplate。
     *
     * @param hotDailyApiProperty 热榜 API 配置对象，包含基础 URL 和 API 密钥
     */
    public HotEngineClient(HotDailyApiProperty hotDailyApiProperty) {
        this.hotDailyApiProperty = hotDailyApiProperty;
    }

    /**
     * 拉取指定热榜源的最新快照。
     *
     * @param sourceCode 热榜源代码，例如 "weibo", "zhihu" 等
     * @return 热榜源快照数据对象，包含热榜项列表和元
     */
    public HotSourceSnapshotVo fetchSourceSnapshot(String sourceCode) {
        String url = buildSourceUrl(hotDailyApiProperty, sourceCode);
        try {
            String responseBody = HttpUtil.createGet(url)
                    .header("Authorization", "Bearer " + hotDailyApiProperty.getApiKey())
                    .timeout(hotDailyApiProperty.getTimeoutMs())
                    .execute()
                    .body();
            if (CharSequenceUtil.isBlank(responseBody)) {
                throw new ServiceException(GlobalErrorCode.BUSINESS_EXCEPTION.getCode(), "热榜抓取服务返回空响应");
            }
            // 解析 JSON 响应并转换为 HotSourceSnapshotVo 对象
            return JSONUtil.toBean(responseBody, HotSourceSnapshotVo.class);
        } catch (HttpException e) {
            LOGGER.error("无法访问热榜抓取服务，url={}, sourceCode={}, error={}", url, sourceCode, e.getMessage(), e);
            throw new ServiceException(GlobalErrorCode.SERVICE_UNAVAILABLE.getCode(), "无法访问热榜抓取服务");
        } catch (JSONException e) {
            LOGGER.error("热榜抓取服务响应解析失败，url={}, sourceCode={}, response解析异常={}", url, sourceCode, e.getMessage(), e);
            throw new ServiceException(GlobalErrorCode.SERVICE_UNAVAILABLE.getCode(), "热榜抓取服务响应解析失败");
        } catch (Exception e) {
            LOGGER.error("拉取热榜数据时发生异常，url={}, sourceCode={}, error={}", url, sourceCode, e.getMessage(), e);
            throw new ServiceException(GlobalErrorCode.SERVICE_UNAVAILABLE.getCode(), "拉取热榜数据时发生异常");
        }
    }

    /**
     * 检查热榜引擎服务的健康状态。
     *
     * @return 热榜引擎健康状态字符串，例如 "ok"
     */
    public HotEngineHealthVo getHealthStatus() {
        try {
            String responseBody = HttpUtil.createGet(buildHealthCheckUrl())
                    .header("Authorization", "Bearer " + hotDailyApiProperty.getApiKey())
                    .timeout(hotDailyApiProperty.getTimeoutMs())
                    .execute()
                    .body();
            if (CharSequenceUtil.isBlank(responseBody)) {
                throw new ServiceException(GlobalErrorCode.INTERNAL_SERVER_ERROR.getCode(), "热榜引擎健康检查服务返回空响应");
            }
            // 解析 JSON 响应并提取健康状态信息
            JSONObject jsonObject = JSONUtil.parseObj(responseBody);
            String status = jsonObject.getStr("status", "unknown");
            HotEngineHealthVo vo = new HotEngineHealthVo();
            vo.setHealthy("ok".equalsIgnoreCase(status));
            vo.setBaseUrl(hotDailyApiProperty.getBaseUrl());
            vo.setMessage(jsonObject.getStr("environment", ""));
            return vo;
        } catch (HttpException e) {
            LOGGER.error("无法访问热榜引擎健康检查服务，url={}, error={}", buildHealthCheckUrl(), e.getMessage(), e);
            throw new ServiceException(GlobalErrorCode.SERVICE_UNAVAILABLE.getCode(), "无法访问热榜引擎健康检查服务");
        } catch (Exception e) {
            LOGGER.error("热榜引擎健康检查时发生异常，url={}, error={}", buildHealthCheckUrl(), e.getMessage(), e);
            throw new ServiceException(GlobalErrorCode.SERVICE_UNAVAILABLE.getCode(), "热榜引擎健康检查时发生异常");
        }
    }


    /**
     * 构建热榜引擎健康检查 URL 地址。
     *
     * @return 热榜引擎健康检查 URL 地址，例如 "http://hot-engine-service/api/v1/health"
     */
    public String buildHealthCheckUrl() {
        return normalizeBaseUrl(hotDailyApiProperty.getBaseUrl()) + "/api/v1/health";
    }

    private String buildSourceUrl(HotDailyApiProperty property, String sourceCode) {
        return normalizeBaseUrl(property.getBaseUrl()) + "/api/v1/hot/" + sourceCode;
    }

    /**
     * 标准化基础URL地址
     * 去除首尾空白字符并移除末尾的斜杠，确保URL格式统一
     *
     * @param baseUrl 待标准化的基础URL地址
     * @return 标准化后的URL地址
     */
    private String normalizeBaseUrl(String baseUrl) {
        return CharSequenceUtil.removeSuffix(CharSequenceUtil.trim(baseUrl), "/");
    }
}
