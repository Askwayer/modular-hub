package com.vita.utils.ip;

import cn.hutool.core.text.CharSequenceUtil;
import cn.hutool.http.HttpUtil;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.vita.property.VitaProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @BelongsProject: be-vita
 * @BelongsPackage: com.vita.utils.ip
 * @Author: znk
 * @CreateTime: 2026-03-12  21:03:09
 * @Description: 地址工具类，提供IP地址查询等相关功能，使用第三方API获取IP地址信息
 * @Version: 1.0
 */
public class AddressUtils {

    private static final Logger log = LoggerFactory.getLogger(AddressUtils.class);

    // IP地址查询
    public static final String IP_URL = "https://whois.pconline.com.cn/ipJson.jsp";

    // 未知地址
    public static final String UNKNOWN = "XX XX";

    public AddressUtils() {
    }

    public static String getRealAddressByIP(String ip) {
        // 内网不查询
        if (IpUtils.internalIp(ip)) {
            return "内网IP";
        }
        if (VitaProperty.isAddressEnabled()) {
            try {
                String rspStr = HttpUtil.get(IP_URL + "?ip=" + ip + "&json=true");
                if (CharSequenceUtil.isEmpty(rspStr)) {
                    log.error("获取地理位置异常 {}", ip);
                    return UNKNOWN;
                }
                JSONObject obj = JSON.parseObject(rspStr);
                String region = obj.getString("pro");
                String city = obj.getString("city");
                return String.format("%s %s", region, city);
            } catch (Exception e) {
                log.error("获取地理位置异常 {}", ip);
            }
        }
        return UNKNOWN;
    }
}
