package com.vita.market.support;

import cn.hutool.core.text.CharSequenceUtil;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONException;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.vita.core.exception.GlobalErrorCode;
import com.vita.core.exception.ServiceException;
import com.vita.market.constant.MarketConstants;
import com.vita.market.model.MarketDataModels;
import com.vita.market.property.MarketProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;

/**
 * AKTools公开函数适配器。该组件只负责HTTP调用和单接口字段标准化，跨源合并由采集器完成。
 */
@Component
public class AkToolsMarketClient {

    private static final Logger LOGGER = LoggerFactory.getLogger(AkToolsMarketClient.class);
    private static final DateTimeFormatter DATE_TIME_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final List<String> RANKING_PERIODS = List.of("today", "5d", "10d");

    private final MarketProperty property;
    private final MarketHttpTransport transport;

    @Autowired
    public AkToolsMarketClient(MarketProperty property) {
        HttpClient httpClient = createHttpClient(property);
        this.property = property;
        this.transport = url -> {
            HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                    .timeout(Duration.ofMillis(property.getReadTimeoutMs()))
                    .GET()
                    .build();
            HttpResponse<String> response = httpClient.send(
                    request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            return new MarketHttpResponse(response.statusCode(), response.body());
        };
    }

    static HttpClient createHttpClient(MarketProperty property) {
        return HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .connectTimeout(Duration.ofMillis(property.getConnectTimeoutMs()))
                .build();
    }

    AkToolsMarketClient(MarketProperty property, MarketHttpTransport transport) {
        this.property = property;
        this.transport = transport;
    }

    public List<MarketDataModels.StockDictionaryItem> fetchStockDictionary() {
        Map<String, MarketDataModels.StockDictionaryItem> items = new LinkedHashMap<>();
        LocalDateTime synchronizedAt = LocalDateTime.now(MarketConstants.SHANGHAI_ZONE);
        appendDictionary(items, fetchRows("stock_info_sh_name_code", Map.of("symbol", "主板A股")),
                "SH", MarketConstants.LISTED, synchronizedAt);
        appendDictionary(items, fetchRows("stock_info_sh_name_code", Map.of("symbol", "科创板")),
                "SH", MarketConstants.LISTED, synchronizedAt);
        appendDictionary(items, fetchRows("stock_info_sz_name_code", Map.of("symbol", "A股列表")),
                "SZ", MarketConstants.LISTED, synchronizedAt);
        appendDictionary(items, fetchRows("stock_info_bj_name_code", Map.of()),
                "BJ", MarketConstants.LISTED, synchronizedAt);
        appendDictionary(items, fetchRows("stock_info_sh_delist", Map.of("symbol", "暂停上市")),
                "SH", MarketConstants.SUSPENDED, synchronizedAt);
        appendDictionary(items, fetchRows("stock_info_sz_delist", Map.of("symbol", "暂停上市公司")),
                "SZ", MarketConstants.SUSPENDED, synchronizedAt);
        appendDictionary(items, fetchRows("stock_info_sh_delist", Map.of("symbol", "终止上市")),
                "SH", MarketConstants.DELISTED, synchronizedAt);
        appendDictionary(items, fetchRows("stock_info_sz_delist", Map.of("symbol", "终止上市公司")),
                "SZ", MarketConstants.DELISTED, synchronizedAt);
        return requireNotEmpty(new ArrayList<>(items.values()), "股票字典");
    }

    public List<MarketDataModels.StockQuote> fetchMarketSnapshot() {
        List<MarketDataModels.StockQuote> result = new ArrayList<>();
        for (JSONObject row : fetchRows("stock_zh_a_spot_em", Map.of())) {
            String code = stockCode(value(row, "代码", "股票代码"));
            if (code == null) {
                continue;
            }
            result.add(new MarketDataModels.StockQuote(
                    code,
                    text(value(row, "名称", "股票简称")),
                    marketOf(code),
                    decimal(value(row, "最新价")),
                    decimal(value(row, "涨跌幅")),
                    money(value(row, "成交额")),
                    decimal(value(row, "换手率")),
                    decimal(value(row, "涨停", "涨停价")),
                    decimal(value(row, "跌停", "跌停价")),
                    parseDateTime(value(row, "更新时间", "时间"), null)
            ));
        }
        return requireNotEmpty(result, "全市场行情");
    }

    public Map<String, MarketDataModels.IndexSpot> fetchIndexSpots() {
        Map<String, MarketDataModels.IndexSpot> result = new LinkedHashMap<>();
        for (JSONObject row : fetchRows("stock_zh_index_spot_sina", Map.of())) {
            String code = stockCode(value(row, "代码", "指数代码", "symbol"));
            if (code == null || !MarketConstants.INDEX_NAMES.containsKey(code)) {
                continue;
            }
            result.put(code, new MarketDataModels.IndexSpot(
                    code,
                    decimal(value(row, "最新价", "最新")),
                    decimal(value(row, "涨跌额", "涨跌")),
                    decimal(value(row, "涨跌幅")),
                    money(value(row, "成交额", "amount"))
            ));
        }
        return result;
    }

    public List<MarketDataModels.CurvePoint> fetchIndexCurve(String indexCode) {
        Map<LocalDateTime, MarketDataModels.CurvePoint> minutePoints = new LinkedHashMap<>();
        for (JSONObject row : fetchRows("index_zh_a_hist_min_em",
                Map.of("symbol", indexCode, "period", "1"))) {
            LocalDateTime time = parseDateTime(value(row, "时间", "datetime"), null);
            BigDecimal close = decimal(value(row, "收盘", "close"));
            if (time == null || close == null) {
                continue;
            }
            LocalDateTime minute = time.withSecond(0).withNano(0);
            minutePoints.put(minute, new MarketDataModels.CurvePoint(minute, close));
        }
        return minutePoints.values().stream()
                .sorted(Comparator.comparing(MarketDataModels.CurvePoint::time))
                .toList();
    }

    public List<MarketDataModels.IndexDailyPoint> fetchIndexDaily(String sourceSymbol) {
        List<MarketDataModels.IndexDailyPoint> result = new ArrayList<>();
        LocalDate today = LocalDate.now(MarketConstants.SHANGHAI_ZONE);
        for (JSONObject row : fetchRows("stock_zh_index_daily_tx", Map.of(
                "symbol", sourceSymbol,
                "start_date", today.minusDays(45).format(DateTimeFormatter.BASIC_ISO_DATE),
                "end_date", today.format(DateTimeFormatter.BASIC_ISO_DATE)))) {
            LocalDate date = parseDate(value(row, "date", "日期"));
            BigDecimal close = decimal(value(row, "close", "收盘"));
            if (date != null && close != null) {
                result.add(new MarketDataModels.IndexDailyPoint(date, close));
            }
        }
        return result.stream()
                .sorted(Comparator.comparing(MarketDataModels.IndexDailyPoint::tradeDate))
                .toList();
    }

    public List<MarketDataModels.SectorQuote> fetchIndustryMarketSectors() {
        List<MarketDataModels.SectorQuote> result = new ArrayList<>();
        appendSectorRows(
                result, fetchRows("stock_board_industry_name_em", Map.of()),
                "industry", BigDecimal.ONE);
        return result;
    }

    public List<MarketDataModels.SectorQuote> fetchIndustrySummarySectors() {
        List<MarketDataModels.SectorQuote> result = new ArrayList<>();
        appendSectorRows(
                result, fetchRows("stock_board_industry_summary_ths", Map.of()),
                "industry", new BigDecimal("100000000"));
        return result;
    }

    public List<MarketDataModels.SectorQuote> fetchConceptSectors() {
        List<MarketDataModels.SectorQuote> result = new ArrayList<>();
        appendSectorRows(
                result, fetchRows("stock_board_concept_name_em", Map.of()),
                "concept", BigDecimal.ONE);
        return result;
    }

    public List<MarketDataModels.SectorRanking> fetchSectorRankings() {
        List<MarketDataModels.SectorRanking> result = new ArrayList<>();
        for (String period : RANKING_PERIODS) {
            appendRanking(result, period, "industry");
            appendRanking(result, period, "concept");
        }
        return requireNotEmpty(result, "板块资金排行");
    }

    public List<MarketDataModels.MarketMutation> fetchMutations() {
        List<MarketDataModels.MarketMutation> result = new ArrayList<>();
        for (JSONObject row : fetchRows("stock_board_change_em", Map.of())) {
            String name = text(value(row, "板块名称", "名称"));
            if (name == null) {
                continue;
            }
            result.add(new MarketDataModels.MarketMutation(
                    text(value(row, "板块代码", "代码")),
                    name,
                    text(value(row,
                            "板块异动最频繁个股及所属类型-买卖方向", "异动方向", "异动类型")),
                    integer(value(row, "板块异动总次数", "异动次数")),
                    decimal(value(row, "涨跌幅")),
                    money(value(row, "主力净流入"), new BigDecimal("10000")),
                    text(value(row,
                            "板块异动最频繁个股及所属类型-股票名称", "异动最频繁股票", "相关股票")),
                    text(value(row, "板块具体异动类型列表及出现次数", "异动描述", "相关信息")),
                    parseDateTime(value(row, "异动时间", "时间"), null)
            ));
        }
        return requireNotEmpty(result, "板块异动");
    }

    public List<MarketDataModels.MarketFundPoint> fetchMarketFundFlow() {
        List<MarketDataModels.MarketFundPoint> result = new ArrayList<>();
        for (JSONObject row : fetchRows("stock_market_fund_flow", Map.of())) {
            LocalDate date = parseDate(value(row, "日期", "date"));
            if (date == null) {
                continue;
            }
            result.add(new MarketDataModels.MarketFundPoint(
                    date,
                    money(value(row, "主力净流入-净额", "主力净流入")),
                    money(value(row, "超大单净流入-净额", "超大单净流入")),
                    money(value(row, "大单净流入-净额", "大单净流入")),
                    money(value(row, "中单净流入-净额", "中单净流入")),
                    money(value(row, "小单净流入-净额", "小单净流入"))
            ));
        }
        return requireNotEmpty(result.stream()
                .sorted(Comparator.comparing(MarketDataModels.MarketFundPoint::tradeDate))
                .toList(), "市场资金趋势");
    }

    public List<MarketDataModels.StockFundSnapshot> fetchIndividualFundFlow() {
        List<MarketDataModels.StockFundSnapshot> result = new ArrayList<>();
        LocalDateTime sampledAt = LocalDateTime.now(MarketConstants.SHANGHAI_ZONE);
        for (JSONObject row : fetchRows("stock_fund_flow_individual", Map.of("symbol", "即时"))) {
            String code = stockCode(value(row, "股票代码", "代码"));
            if (code == null) {
                continue;
            }
            LocalDateTime sourceTime = parseDateTime(value(row, "更新时间", "时间"), null);
            result.add(new MarketDataModels.StockFundSnapshot(
                    code,
                    sourceTime == null ? sampledAt.toLocalDate() : sourceTime.toLocalDate(),
                    sampledAt,
                    money(value(row, "流入资金", "主力流入")),
                    positive(money(value(row, "流出资金", "主力流出"))),
                    money(value(row, "净额", "主力净流入")),
                    decimal(value(row, "最新价")),
                    decimal(value(row, "涨跌幅")),
                    money(value(row, "成交额")),
                    decimal(value(row, "换手率")),
                    sourceTime
            ));
        }
        return requireNotEmpty(result, "个股资金");
    }

    public List<MarketDataModels.SectorTopStock> fetchSectorConstituents(String sectorType,
                                                                        String sectorCode) {
        String function = "concept".equals(sectorType)
                ? "stock_board_concept_cons_em"
                : "stock_board_industry_cons_em";
        List<MarketDataModels.SectorTopStock> result = new ArrayList<>();
        for (JSONObject row : fetchRows(function, Map.of("symbol", sectorCode))) {
            String code = stockCode(value(row, "代码", "股票代码"));
            if (code == null) {
                continue;
            }
            result.add(new MarketDataModels.SectorTopStock(
                    code,
                    text(value(row, "名称", "股票名称")),
                    decimal(value(row, "最新价")),
                    decimal(value(row, "涨跌幅")),
                    money(value(row, "成交额")),
                    decimal(value(row, "换手率")),
                    null,
                    null,
                    null,
                    false
            ));
        }
        return requireNotEmpty(result, "板块成分");
    }

    public List<LocalDate> fetchTradeDates() {
        List<LocalDate> dates = new ArrayList<>();
        for (JSONObject row : fetchRows("tool_trade_date_hist_sina", Map.of())) {
            LocalDate date = parseDate(value(row, "trade_date", "交易日", "日期"));
            if (date != null) {
                dates.add(date);
            }
        }
        return requireNotEmpty(dates, "交易日历");
    }

    private void appendDictionary(Map<String, MarketDataModels.StockDictionaryItem> target,
                                  List<JSONObject> rows,
                                  String market,
                                  String listingStatus,
                                  LocalDateTime synchronizedAt) {
        for (JSONObject row : rows) {
            String code = stockCode(value(row, "证券代码", "A股代码", "公司代码", "代码"));
            String name = text(value(row, "证券简称", "A股简称", "公司简称", "名称"));
            if (code != null && name != null) {
                target.put(code, new MarketDataModels.StockDictionaryItem(
                        code, name, market, listingStatus, synchronizedAt));
            }
        }
    }

    private void appendSectorRows(List<MarketDataModels.SectorQuote> target,
                                  List<JSONObject> rows,
                                  String sectorType,
                                  BigDecimal sourceMoneyMultiplier) {
        for (JSONObject row : rows) {
            String name = text(value(row, "板块名称", "名称", "板块"));
            if (name == null) {
                continue;
            }
            target.add(new MarketDataModels.SectorQuote(
                    sectorType,
                    text(value(row, "板块代码", "代码")),
                    name,
                    decimal(value(row, "涨跌幅")),
                    "industry".equals(sectorType)
                            ? money(value(row, "成交额", "总成交额"), sourceMoneyMultiplier) : null,
                    money(value(row, "总市值")),
                    decimal(value(row, "换手率")),
                    integer(value(row, "上涨家数", "上涨数")),
                    integer(value(row, "下跌家数", "下跌数")),
                    text(value(row, "领涨股票", "领涨股")),
                    money(value(row, "主力净流入", "主力净流入-净额", "净流入"),
                            sourceMoneyMultiplier)
            ));
        }
    }

    private void appendRanking(List<MarketDataModels.SectorRanking> target,
                               String period,
                               String sectorType) {
        for (JSONObject row : fetchRows("stock_sector_fund_flow_rank",
                Map.of("indicator", period, "sector_type", sectorType))) {
            String name = text(value(row, "名称", "板块名称"));
            if (name == null) {
                continue;
            }
            String sourcePeriod = switch (period) {
                case "5d" -> "5日";
                case "10d" -> "10日";
                default -> "今日";
            };
            target.add(new MarketDataModels.SectorRanking(
                    period,
                    sectorType,
                    text(value(row, "板块代码", "代码")),
                    name,
                    decimal(value(row, sourcePeriod + "涨跌幅", "涨跌幅")),
                    money(value(row,
                            sourcePeriod + "主力净流入-净额", "主力净流入-净额", "主力净流入"))
            ));
        }
    }

    /**
     * 仅网络异常和5xx重试；4xx、空响应和字段错误立即失败，避免无意义重复请求。
     */
    private List<JSONObject> fetchRows(String function, Map<String, ?> query) {
        String path = "/api/public/" + function;
        String url = buildUrl(path, query);
        int attempts = Math.min(2, Math.max(1, property.getRetryTimes() + 1));
        Exception lastError = null;
        for (int attempt = 1; attempt <= attempts; attempt++) {
            try {
                MarketHttpResponse response = transport.get(url);
                if (response.statusCode() >= 400 && response.statusCode() < 500) {
                    throw unavailable("AKTools HTTP状态异常: " + response.statusCode());
                }
                if (response.statusCode() >= 500) {
                    lastError = unavailable("AKTools HTTP状态异常: " + response.statusCode());
                    continue;
                }
                if (response.statusCode() < 200 || response.statusCode() >= 300) {
                    throw unavailable("AKTools HTTP状态异常: " + response.statusCode());
                }
                return parseRows(response.body());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw unavailable("AKTools请求被中断");
            } catch (ServiceException e) {
                throw e;
            } catch (Exception e) {
                lastError = e;
                LOGGER.warn("AKTools网络请求失败，attempt={}/{}, function={}, error={}",
                        attempt, attempts, function, e.getMessage());
            }
        }
        throw unavailable(lastError == null ? "AKTools请求失败" : lastError.getMessage());
    }

    private List<JSONObject> parseRows(String body) {
        if (CharSequenceUtil.isBlank(body)) {
            throw unavailable("AKTools返回空响应");
        }
        try {
            Object root = JSONUtil.parse(body);
            JSONArray array;
            if (root instanceof JSONArray jsonArray) {
                array = jsonArray;
            } else if (root instanceof JSONObject jsonObject
                    && jsonObject.get("data") instanceof JSONArray jsonArray) {
                array = jsonArray;
            } else {
                throw unavailable("AKTools响应缺少data数组");
            }
            if (array.isEmpty()) {
                throw unavailable("AKTools返回空数据");
            }
            List<JSONObject> rows = new ArrayList<>(array.size());
            for (Object item : array) {
                rows.add(item instanceof JSONObject object ? object : JSONUtil.parseObj(item));
            }
            return rows;
        } catch (JSONException e) {
            throw unavailable("AKTools响应JSON解析失败");
        }
    }

    private String buildUrl(String path, Map<String, ?> query) {
        String baseUrl = property.getAktoolsBaseUrl().replaceAll("/+$", "");
        if (query.isEmpty()) {
            return baseUrl + path;
        }
        String queryString = query.entrySet().stream()
                .map(entry -> encode(entry.getKey()) + "=" + encode(String.valueOf(entry.getValue())))
                .reduce((left, right) -> left + "&" + right)
                .orElse("");
        return baseUrl + path + "?" + queryString;
    }

    private String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private Object value(JSONObject row, String... aliases) {
        for (String alias : aliases) {
            Object candidate = row.get(alias);
            if (candidate != null) {
                return candidate;
            }
        }
        return null;
    }

    private String text(Object value) {
        if (value == null) {
            return null;
        }
        String result = String.valueOf(value).trim();
        return result.isEmpty() || "--".equals(result)
                || "null".equalsIgnoreCase(result) || "nan".equalsIgnoreCase(result)
                ? null : result;
    }

    private String stockCode(Object value) {
        String code = text(value);
        if (code == null) {
            return null;
        }
        code = code.replaceAll("\\.0$", "")
                .replaceFirst("(?i)^(SH|SZ|BJ)", "")
                .trim();
        if (!code.chars().allMatch(Character::isDigit) || code.length() > 6) {
            return null;
        }
        return "0".repeat(6 - code.length()) + code;
    }

    private String marketOf(String stockCode) {
        if (stockCode.startsWith("4") || stockCode.startsWith("8") || stockCode.startsWith("9")) {
            return "BJ";
        }
        return stockCode.startsWith("6") ? "SH" : "SZ";
    }

    private BigDecimal decimal(Object value) {
        String raw = text(value);
        if (raw == null) {
            return null;
        }
        String normalized = raw.replace(",", "").replace("%", "").replace("元", "").trim();
        try {
            return new BigDecimal(normalized);
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private BigDecimal money(Object value) {
        String raw = text(value);
        if (raw == null) {
            return null;
        }
        String normalized = raw.replace(",", "").replace("元", "").trim();
        BigDecimal multiplier = BigDecimal.ONE;
        if (normalized.endsWith("亿元") || normalized.endsWith("亿")) {
            multiplier = new BigDecimal("100000000");
            normalized = normalized.replaceFirst("亿元?$", "");
        } else if (normalized.endsWith("万元") || normalized.endsWith("万")) {
            multiplier = new BigDecimal("10000");
            normalized = normalized.replaceFirst("万元?$", "");
        }
        try {
            return new BigDecimal(normalized).multiply(multiplier);
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private BigDecimal money(Object value, BigDecimal sourceMultiplier) {
        String raw = text(value);
        BigDecimal amount = money(value);
        if (amount == null || raw == null
                || raw.contains("万") || raw.contains("亿")) {
            return amount;
        }
        return amount.multiply(sourceMultiplier);
    }

    private BigDecimal positive(BigDecimal value) {
        return value == null ? null : value.abs();
    }

    private Integer integer(Object value) {
        BigDecimal decimal = decimal(value);
        return decimal == null ? null : decimal.setScale(0, RoundingMode.DOWN).intValue();
    }

    private LocalDate parseDate(Object value) {
        String raw = text(value);
        if (raw == null || raw.length() < 10) {
            return null;
        }
        try {
            return LocalDate.parse(raw.substring(0, 10));
        } catch (DateTimeParseException ignored) {
            return null;
        }
    }

    private LocalDateTime parseDateTime(Object value, LocalDate fallbackDate) {
        String raw = text(value);
        if (raw == null) {
            return null;
        }
        try {
            if (raw.length() == 8 && raw.charAt(2) == ':' && fallbackDate != null) {
                return LocalDateTime.of(fallbackDate, LocalTime.parse(raw));
            }
            if (raw.length() == 16) {
                return LocalDateTime.parse(raw + ":00", DATE_TIME_FORMATTER);
            }
            return LocalDateTime.parse(raw, DATE_TIME_FORMATTER);
        } catch (DateTimeParseException ignored) {
            return null;
        }
    }

    private <T> List<T> requireNotEmpty(List<T> values, String module) {
        if (values == null || values.isEmpty()) {
            throw unavailable(module + "返回空数据");
        }
        return values;
    }

    private ServiceException unavailable(String message) {
        return new ServiceException(GlobalErrorCode.SERVICE_UNAVAILABLE.getCode(),
                CharSequenceUtil.blankToDefault(message, "AKTools服务不可用"));
    }

    @FunctionalInterface
    interface MarketHttpTransport {
        MarketHttpResponse get(String url) throws Exception;
    }

    record MarketHttpResponse(int statusCode, String body) {
    }
}
