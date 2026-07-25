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
 * @BelongsProject: be-vita
 * @BelongsPackage: com.vita.market.support
 * @Author: znk
 * @CreateTime: 2026-07-25
 * @Description: AKTools A股行情固定接口适配器
 * @Version: 1.0
 */
@Component
public class AkToolsMarketClient {

    private static final Logger LOGGER = LoggerFactory.getLogger(AkToolsMarketClient.class);
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final List<String> RANKING_PERIODS = List.of("今日", "5日", "10日");

    private final MarketProperty property;
    private final MarketHttpTransport transport;

    @Autowired
    public AkToolsMarketClient(MarketProperty property) {
        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofMillis(property.getConnectTimeoutMs()))
                .build();
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

    AkToolsMarketClient(MarketProperty property, MarketHttpTransport transport) {
        this.property = property;
        this.transport = transport;
    }

    /**
     * 同步沪深京股票字典。方法名固定在服务端，前端无法注入 AKShare 函数。
     *
     * @return 全量股票字典
     */
    public List<MarketDataModels.StockDictionaryItem> fetchStockDictionary() {
        Map<String, MarketDataModels.StockDictionaryItem> items = new LinkedHashMap<>();
        appendDictionary(items, fetchRows("/api/public/stock_info_sh_name_code", Map.of("symbol", "主板A股")),
                "SH", MarketConstants.LISTED);
        appendDictionary(items, fetchRows("/api/public/stock_info_sh_name_code", Map.of("symbol", "科创板")),
                "SH", MarketConstants.LISTED);
        appendDictionary(items, fetchRows("/api/public/stock_info_sz_name_code", Map.of("symbol", "A股列表")),
                "SZ", MarketConstants.LISTED);
        appendDictionary(items, fetchRows("/api/public/stock_info_bj_name_code", Map.of()),
                "BJ", MarketConstants.LISTED);
        appendDictionary(items, fetchRows("/api/public/stock_info_sh_delist", Map.of("symbol", "暂停上市")),
                "SH", MarketConstants.SUSPENDED);
        appendDictionary(items, fetchRows("/api/public/stock_info_sz_delist", Map.of("symbol", "暂停上市公司")),
                "SZ", MarketConstants.SUSPENDED);
        appendDictionary(items, fetchRows("/api/public/stock_info_sh_delist", Map.of("symbol", "终止上市")),
                "SH", MarketConstants.DELISTED);
        appendDictionary(items, fetchRows("/api/public/stock_info_sz_delist", Map.of("symbol", "终止上市公司")),
                "SZ", MarketConstants.DELISTED);
        return new ArrayList<>(items.values());
    }

    /**
     * 查询全市场实时行情。
     *
     * @return 标准化股票行情
     */
    public List<MarketDataModels.StockQuote> fetchMarketSnapshot() {
        List<MarketDataModels.StockQuote> result = new ArrayList<>();
        LocalDateTime sourceTime = LocalDateTime.now(MarketConstants.SHANGHAI_ZONE);
        for (JSONObject row : fetchRows("/api/public/stock_zh_a_spot_em", Map.of())) {
            String code = stockCode(required(row, "代码", "股票代码"));
            result.add(new MarketDataModels.StockQuote(
                    code,
                    text(required(row, "名称", "股票简称")),
                    marketOf(code),
                    decimal(required(row, "最新价")),
                    decimal(required(row, "涨跌幅")),
                    decimal(required(row, "成交额")),
                    decimal(required(row, "换手率")),
                    parseDateTime(value(row, "更新时间", "时间"), sourceTime)
            ));
        }
        return requireNotEmpty(result, "全市场行情");
    }

    /**
     * 查询固定十大指数及日内曲线。
     *
     * @return 指数快照
     */
    public List<MarketDataModels.IndexQuote> fetchIndices() {
        Map<String, JSONObject> spotByCode = new LinkedHashMap<>();
        for (JSONObject row : fetchRows("/api/public/index_zh_a_spot", Map.of())) {
            String code = stockCode(required(row, "代码", "指数代码"));
            if (MarketConstants.INDEX_NAMES.containsKey(code)) {
                spotByCode.put(code, row);
            }
        }
        List<MarketDataModels.IndexQuote> result = new ArrayList<>();
        for (Map.Entry<String, String> index : MarketConstants.INDEX_NAMES.entrySet()) {
            JSONObject spot = spotByCode.get(index.getKey());
            if (spot == null) {
                throw unavailable("指数实时列表缺少 " + index.getKey());
            }
            List<MarketDataModels.CurvePoint> curve = fetchIndexCurve(index.getKey());
            result.add(new MarketDataModels.IndexQuote(
                    index.getKey(),
                    index.getValue(),
                    decimal(required(spot, "最新价", "最新")),
                    decimal(required(spot, "涨跌幅")),
                    decimal(value(spot, "成交额")),
                    parseDateTime(value(spot, "时间", "更新时间"),
                            LocalDateTime.now(MarketConstants.SHANGHAI_ZONE)),
                    curve
            ));
        }
        return result;
    }

    /**
     * 查询行业或概念板块快照，成交额由 AKTools 稳定适配接口补齐。
     *
     * @param sectorType industry 或 concept
     * @return 板块快照
     */
    public List<MarketDataModels.SectorQuote> fetchSectorSnapshot(String sectorType) {
        if (!List.of("industry", "concept").contains(sectorType)) {
            throw new ServiceException(GlobalErrorCode.BAD_REQUEST.getCode(), "板块类型仅支持 industry 或 concept");
        }
        List<MarketDataModels.SectorQuote> result = new ArrayList<>();
        for (JSONObject row : fetchRows("/api/market/sector/snapshot", Map.of("sectorType", sectorType))) {
            result.add(new MarketDataModels.SectorQuote(
                    sectorType,
                    text(required(row, "sectorCode", "板块代码", "代码")),
                    text(required(row, "name", "板块名称", "名称")),
                    decimal(required(row, "changePercent", "涨跌幅")),
                    decimal(required(row, "turnover", "成交额")),
                    decimal(value(row, "marketCap", "总市值")),
                    decimal(value(row, "turnoverRate", "换手率")),
                    integer(value(row, "upCount", "上涨家数")),
                    integer(value(row, "downCount", "下跌家数")),
                    text(value(row, "leader", "领涨股票")),
                    decimal(value(row, "mainNetInflow", "主力净流入"))
            ));
        }
        return requireNotEmpty(result, sectorType + "板块快照");
    }

    /**
     * 查询板块今日、5日和10日资金强弱榜。
     *
     * @return 板块排行
     */
    public List<MarketDataModels.SectorRanking> fetchSectorRankings() {
        List<MarketDataModels.SectorRanking> result = new ArrayList<>();
        for (String period : RANKING_PERIODS) {
            appendRanking(result, period, "industry", "行业资金流");
            appendRanking(result, period, "concept", "概念资金流");
        }
        return requireNotEmpty(result, "板块资金排行");
    }

    /**
     * 查询板块异动。
     *
     * @return 最近异动记录
     */
    public List<MarketDataModels.MarketMutation> fetchMutations() {
        List<MarketDataModels.MarketMutation> result = new ArrayList<>();
        for (JSONObject row : fetchRows("/api/public/stock_board_change_em", Map.of())) {
            result.add(new MarketDataModels.MarketMutation(
                    text(value(row, "板块代码", "代码")),
                    text(required(row, "板块名称", "名称")),
                    text(required(row, "异动类型", "涨跌幅")),
                    text(value(row, "异动描述", "相关信息")),
                    parseDateTime(value(row, "异动时间", "时间"),
                            LocalDateTime.now(MarketConstants.SHANGHAI_ZONE))
            ));
        }
        return requireNotEmpty(result, "板块异动");
    }

    /**
     * 查询市场资金趋势，并使用固定指数作为对照线。
     *
     * @param indexCode 固定十大指数中的代码
     * @return 最近20日资金趋势
     */
    public List<MarketDataModels.FundTrendPoint> fetchFundTrend(String indexCode) {
        if (!MarketConstants.INDEX_NAMES.containsKey(indexCode)) {
            throw new ServiceException(GlobalErrorCode.BAD_REQUEST.getCode(), "不支持的指数代码");
        }
        Map<LocalDate, BigDecimal> indexValues = new LinkedHashMap<>();
        for (JSONObject row : fetchRows("/api/public/index_zh_a_hist",
                Map.of("symbol", indexCode, "period", "daily"))) {
            LocalDate date = parseDate(required(row, "日期", "date"));
            indexValues.put(date, decimal(required(row, "收盘", "close")));
        }
        List<MarketDataModels.FundTrendPoint> result = new ArrayList<>();
        for (JSONObject row : fetchRows("/api/public/stock_market_fund_flow", Map.of())) {
            LocalDate date = parseDate(required(row, "日期", "date"));
            result.add(new MarketDataModels.FundTrendPoint(
                    date,
                    decimal(required(row, "主力净流入-净额", "主力净流入")),
                    indexValues.get(date),
                    indexCode
            ));
        }
        result.sort(Comparator.comparing(MarketDataModels.FundTrendPoint::tradeDate));
        if (result.size() > 20) {
            result = new ArrayList<>(result.subList(result.size() - 20, result.size()));
        }
        return requireNotEmpty(result, "市场资金趋势");
    }

    /**
     * 查询所有个股的即时资金快照。
     *
     * @return 个股资金快照
     */
    public List<MarketDataModels.StockFundSnapshot> fetchIndividualFundFlow() {
        List<MarketDataModels.StockFundSnapshot> result = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now(MarketConstants.SHANGHAI_ZONE);
        for (JSONObject row : fetchRows("/api/public/stock_fund_flow_individual", Map.of("symbol", "即时"))) {
            String code = stockCode(required(row, "股票代码", "代码"));
            MarketDataModels.StockFundSnapshot point = new MarketDataModels.StockFundSnapshot(
                    code,
                    now.toLocalDate(),
                    parseDateTime(value(row, "更新时间", "时间"), now),
                    decimal(required(row, "流入资金", "主力流入")),
                    positive(decimal(required(row, "流出资金", "主力流出"))),
                    decimal(required(row, "净额", "主力净流入")),
                    decimal(required(row, "最新价")),
                    decimal(required(row, "涨跌幅")),
                    decimal(required(row, "成交额")),
                    decimal(required(row, "换手率"))
            );
            result.add(point);
        }
        return requireNotEmpty(result, "个股资金");
    }

    /**
     * 查询指定板块成分股并按指定口径取前五名。
     *
     * @param sectorType industry 或 concept
     * @param sectorCode 板块代码或名称
     * @param metric     changePercent、mainNetInflow 或 turnover
     * @return 前五名成分股
     */
    public List<MarketDataModels.SectorTopStock> fetchSectorTopStocks(String sectorType,
                                                                     String sectorCode,
                                                                     String metric) {
        String path = "concept".equals(sectorType)
                ? "/api/public/stock_board_concept_cons_em"
                : "/api/public/stock_board_industry_cons_em";
        List<MarketDataModels.SectorTopStock> result = new ArrayList<>();
        for (JSONObject row : fetchRows(path, Map.of("symbol", sectorCode))) {
            result.add(new MarketDataModels.SectorTopStock(
                    stockCode(required(row, "代码", "股票代码")),
                    text(required(row, "名称", "股票名称")),
                    decimal(required(row, "涨跌幅")),
                    decimal(value(row, "主力净流入", "净额")),
                    decimal(required(row, "成交额"))
            ));
        }
        Comparator<MarketDataModels.SectorTopStock> comparator = switch (metric) {
            case "mainNetInflow" -> Comparator.comparing(
                    MarketDataModels.SectorTopStock::mainNetInflow,
                    Comparator.nullsLast(Comparator.reverseOrder()));
            case "turnover" -> Comparator.comparing(
                    MarketDataModels.SectorTopStock::turnoverAmount,
                    Comparator.nullsLast(Comparator.reverseOrder()));
            default -> Comparator.comparing(
                    MarketDataModels.SectorTopStock::changePercent,
                    Comparator.nullsLast(Comparator.reverseOrder()));
        };
        return result.stream().sorted(comparator).limit(5).toList();
    }

    /**
     * 查询交易日历。
     *
     * @return 交易日列表
     */
    public List<LocalDate> fetchTradeDates() {
        List<LocalDate> dates = new ArrayList<>();
        for (JSONObject row : fetchRows("/api/public/tool_trade_date_hist_sina", Map.of())) {
            dates.add(parseDate(required(row, "trade_date", "交易日", "日期")));
        }
        return requireNotEmpty(dates, "交易日历");
    }

    private List<MarketDataModels.CurvePoint> fetchIndexCurve(String indexCode) {
        List<MarketDataModels.CurvePoint> result = new ArrayList<>();
        for (JSONObject row : fetchRows("/api/public/index_zh_a_hist_min_em",
                Map.of("symbol", indexCode, "period", "1"))) {
            result.add(new MarketDataModels.CurvePoint(
                    parseDateTime(required(row, "时间", "datetime"),
                            LocalDateTime.now(MarketConstants.SHANGHAI_ZONE)),
                    decimal(required(row, "收盘", "close"))
            ));
        }
        return requireNotEmpty(result, indexCode + "指数日内曲线");
    }

    private void appendDictionary(Map<String, MarketDataModels.StockDictionaryItem> target,
                                  List<JSONObject> rows,
                                  String market,
                                  String listingStatus) {
        for (JSONObject row : rows) {
            String code = stockCode(required(row, "证券代码", "A股代码", "公司代码", "代码"));
            String name = text(required(row, "证券简称", "A股简称", "公司简称", "名称"));
            target.put(code, new MarketDataModels.StockDictionaryItem(
                    code, name, market, listingStatus,
                    LocalDateTime.now(MarketConstants.SHANGHAI_ZONE)));
        }
    }

    private void appendRanking(List<MarketDataModels.SectorRanking> target,
                               String period,
                               String sectorType,
                               String sourceType) {
        for (JSONObject row : fetchRows("/api/public/stock_sector_fund_flow_rank",
                Map.of("indicator", period, "sector_type", sourceType))) {
            target.add(new MarketDataModels.SectorRanking(
                    period,
                    sectorType,
                    text(value(row, "板块代码", "代码")),
                    text(required(row, "名称", "板块名称")),
                    decimal(value(row, "涨跌幅")),
                    decimal(required(row, "主力净流入-净额", "主力净流入"))
            ));
        }
    }

    /**
     * HTTP 错误、空响应和错误 JSON 都在适配器边界转为服务不可用，禁止污染旧缓存。
     */
    private List<JSONObject> fetchRows(String path, Map<String, ?> query) {
        String url = buildUrl(path, query);
        RuntimeException lastError = null;
        int attempts = Math.max(1, property.getRetryTimes() + 1);
        for (int attempt = 1; attempt <= attempts; attempt++) {
            try {
                MarketHttpResponse response = transport.get(url);
                if (response.statusCode() < 200 || response.statusCode() >= 300) {
                    throw unavailable("AKTools HTTP状态异常: " + response.statusCode());
                }
                return parseRows(response.body());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw unavailable("AKTools请求被中断");
            } catch (Exception e) {
                lastError = e instanceof RuntimeException runtimeException
                        ? runtimeException : unavailable(e.getMessage());
                LOGGER.warn("AKTools请求失败，attempt={}/{}, path={}, error={}",
                        attempt, attempts, path, e.getMessage());
            }
        }
        throw lastError == null ? unavailable("AKTools请求失败") : lastError;
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
            } else if (root instanceof JSONObject jsonObject) {
                Object data = jsonObject.get("data");
                if (!(data instanceof JSONArray jsonArray)) {
                    throw unavailable("AKTools响应缺少data数组");
                }
                array = jsonArray;
            } else {
                throw unavailable("AKTools响应JSON结构错误");
            }
            if (array.isEmpty()) {
                throw unavailable("AKTools返回空数据");
            }
            List<JSONObject> rows = new ArrayList<>(array.size());
            for (Object value : array) {
                JSONObject row = value instanceof JSONObject jsonObject
                        ? jsonObject : JSONUtil.parseObj(value);
                rows.add(row);
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

    private Object required(JSONObject row, String... aliases) {
        Object value = value(row, aliases);
        if (value == null || CharSequenceUtil.isBlank(String.valueOf(value))
                || "--".equals(String.valueOf(value).trim())) {
            throw unavailable("AKTools响应缺少字段: " + String.join("/", aliases));
        }
        return value;
    }

    private Object value(JSONObject row, String... aliases) {
        for (String alias : aliases) {
            Object value = row.get(alias);
            if (value != null) {
                return value;
            }
        }
        return null;
    }

    private String text(Object value) {
        if (value == null) {
            return null;
        }
        String text = String.valueOf(value).trim();
        return text.isEmpty() || "--".equals(text) ? null : text;
    }

    private String stockCode(Object value) {
        String code = text(value);
        if (code == null) {
            throw unavailable("股票代码为空");
        }
        code = code.replaceAll("\\.0$", "").replaceAll("^(SH|SZ|BJ)", "").trim();
        return code.length() < 6 && code.chars().allMatch(Character::isDigit)
                ? "0".repeat(6 - code.length()) + code : code;
    }

    private String marketOf(String stockCode) {
        if (stockCode.startsWith("4") || stockCode.startsWith("8") || stockCode.startsWith("9")) {
            return "BJ";
        }
        return stockCode.startsWith("6") ? "SH" : "SZ";
    }

    private BigDecimal decimal(Object value) {
        String text = text(value);
        if (text == null) {
            return null;
        }
        String normalized = text.replace(",", "").replace("%", "").replace("元", "");
        BigDecimal multiplier = BigDecimal.ONE;
        if (normalized.endsWith("亿")) {
            multiplier = new BigDecimal("100000000");
            normalized = normalized.substring(0, normalized.length() - 1);
        } else if (normalized.endsWith("万")) {
            multiplier = new BigDecimal("10000");
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        try {
            return new BigDecimal(normalized).multiply(multiplier);
        } catch (NumberFormatException e) {
            throw unavailable("AKTools数值无法解析: " + text);
        }
    }

    private BigDecimal positive(BigDecimal value) {
        return value == null ? null : value.abs();
    }

    private Integer integer(Object value) {
        BigDecimal number = decimal(value);
        return number == null ? null : number.intValue();
    }

    private LocalDate parseDate(Object value) {
        String text = Objects.requireNonNull(text(value), "date");
        try {
            return LocalDate.parse(text.substring(0, Math.min(10, text.length())));
        } catch (DateTimeParseException e) {
            throw unavailable("AKTools日期无法解析: " + text);
        }
    }

    private LocalDateTime parseDateTime(Object value, LocalDateTime fallback) {
        String text = text(value);
        if (text == null) {
            return fallback;
        }
        try {
            if (text.length() == 8 && text.charAt(2) == ':') {
                return LocalDateTime.of(fallback.toLocalDate(), LocalTime.parse(text));
            }
            return LocalDateTime.parse(text, DATE_TIME_FORMATTER);
        } catch (DateTimeParseException e) {
            throw unavailable("AKTools时间无法解析: " + text);
        }
    }

    private <T> List<T> requireNotEmpty(List<T> values, String module) {
        if (values == null || values.isEmpty()) {
            throw unavailable(module + "返回空数据");
        }
        return values;
    }

    private ServiceException unavailable(String message) {
        return new ServiceException(GlobalErrorCode.SERVICE_UNAVAILABLE.getCode(), message);
    }

    @FunctionalInterface
    interface MarketHttpTransport {
        MarketHttpResponse get(String url) throws Exception;
    }

    record MarketHttpResponse(int statusCode, String body) {
    }
}
