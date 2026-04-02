package com.vita.log.resolver;

import cn.hutool.core.text.CharSequenceUtil;
import cn.hutool.http.useragent.UserAgent;
import cn.hutool.http.useragent.UserAgentUtil;
import com.vita.log.model.RequestClientInfo;
import com.vita.utils.ServletUtils;
import com.vita.utils.ip.AddressUtils;
import com.vita.utils.ip.IpUtils;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;

/**
 * 请求客户端信息解析器，负责从当前请求中提取 IP、归属地和终端信息。
 *
 * @author znk
 */
@Component
public class RequestClientInfoResolver {

    /**
     * 解析当前线程绑定请求的客户端信息。
     *
     * @return 客户端信息快照
     */
    public RequestClientInfo resolveCurrentRequest() {
        try {
            return resolve(ServletUtils.getRequest());
        } catch (Exception ignored) {
            return resolve(null);
        }
    }

    /**
     * 基于指定请求解析客户端信息。
     *
     * @param request 请求对象
     * @return 客户端信息快照
     */
    public RequestClientInfo resolve(HttpServletRequest request) {
        RequestClientInfo requestClientInfo = new RequestClientInfo();
        requestClientInfo.setRequestTime(LocalDateTime.now());
        String ip = request == null ? "unknown" : IpUtils.getIpAddr(request);
        requestClientInfo.setIp(ip);
        requestClientInfo.setLocation(StringUtils.hasText(ip) ? AddressUtils.getRealAddressByIP(ip) : AddressUtils.UNKNOWN);

        String userAgentValue = request == null ? null : request.getHeader("User-Agent");
        UserAgent userAgent = CharSequenceUtil.isBlank(userAgentValue) ? null : UserAgentUtil.parse(userAgentValue);
        requestClientInfo.setBrowser(userAgent == null || userAgent.getBrowser() == null ? "Unknown" : userAgent.getBrowser().getName());
        requestClientInfo.setOs(userAgent == null || userAgent.getOs() == null ? "Unknown" : userAgent.getOs().getName());
        return requestClientInfo;
    }
}
