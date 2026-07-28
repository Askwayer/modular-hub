package com.vita.market.constant;

import java.time.ZoneId;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * @BelongsProject: be-vita
 * @BelongsPackage: com.vita.market.constant
 * @Author: znk
 * @CreateTime: 2026-07-25
 * @Description: 行情领域常量
 * @Version: 1.0
 */
public final class MarketConstants {

    public static final String LISTED = "LISTED";
    public static final String SUSPENDED = "SUSPENDED";
    public static final String DELISTED = "DELISTED";
    public static final int MAX_MONITORED_STOCKS = 10;
    public static final int MONITOR_PAGE_SIZE = 4;
    public static final int SAMPLE_BUCKET_SECONDS = 40;
    public static final ZoneId SHANGHAI_ZONE = ZoneId.of("Asia/Shanghai");
    public static final String REFRESH_LOCK_KEY = "vita:market:lock:refresh";
    public static final String STOCK_MANAGE_LOCK_KEY = "vita:market:lock:stock-manage";
    public static final String DICTIONARY_SYNC_LOCK_KEY = "vita:market:lock:dictionary-sync";
    public static final String MANUAL_REFRESH_RATE_KEY = "vita:market:rate:manual-refresh";
    public static final String RECENT_TRADE_DATE_KEY = "vita:market:recent-trade-date";
    public static final String TRADE_CALENDAR_KEY = "vita:market:trade-calendar";
    public static final String CLOSE_FINALIZED_PREFIX = "vita:market:close-finalized:";
    public static final String CLOSE_MODULE_FINALIZED_PREFIX = "vita:market:close-module-finalized:";

    public static final List<IndexDefinition> INDEX_DEFINITIONS = List.of(
            new IndexDefinition("000001", "上证指数", "sh000001"),
            new IndexDefinition("399001", "深证成指", "sz399001"),
            new IndexDefinition("399006", "创业板指", "sz399006"),
            new IndexDefinition("000688", "科创50", "sh000688"),
            new IndexDefinition("000680", "科创综指", "sh000680"),
            new IndexDefinition("000510", "中证A500", "sh000510"),
            new IndexDefinition("000300", "沪深300", "sh000300"),
            new IndexDefinition("000016", "上证50", "sh000016"),
            new IndexDefinition("399330", "深证100", "sz399330")
    );
    public static final Map<String, String> INDEX_NAMES = buildIndexNames();

    private MarketConstants() {
    }

    /**
     * 构造固定指数白名单，调用方不得由请求参数扩展该集合。
     *
     * @return 指数代码和名称映射
     */
    private static Map<String, String> buildIndexNames() {
        Map<String, String> indices = new LinkedHashMap<>();
        INDEX_DEFINITIONS.forEach(index -> indices.put(index.code(), index.name()));
        return Collections.unmodifiableMap(indices);
    }

    public static String snapshotKey(String tradeDate, String module) {
        return "vita:market:snapshot:" + tradeDate + ":" + module;
    }

    public static String moduleStateKey(String module) {
        return "vita:market:state:" + module;
    }

    public static String sectorTopStocksKey(String tradeDate,
                                            String sectorType,
                                            String sectorCode,
                                            String metric) {
        return "vita:market:sector-top:" + tradeDate + ":" + sectorType + ":" + sectorCode + ":" + metric;
    }

    public static String closeModuleFinalizedKey(String tradeDate, String module) {
        return CLOSE_MODULE_FINALIZED_PREFIX + tradeDate + ":" + module;
    }

    public record IndexDefinition(String code, String name, String sourceSymbol) {
    }
}
