package com.vita.market.support;

import com.vita.core.exception.ServiceException;
import com.vita.market.model.MarketDataModels;
import com.vita.market.property.MarketProperty;
import org.junit.jupiter.api.Test;

import java.net.http.HttpClient;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * AKTools 中文字段、金额单位和错误响应夹具测试。
 */
class AkToolsMarketClientTest {

    @Test
    void shouldUseHttp11ForUvicorn() {
        MarketProperty property = new MarketProperty();

        HttpClient httpClient = AkToolsMarketClient.createHttpClient(property);

        assertThat(httpClient.version()).isEqualTo(HttpClient.Version.HTTP_1_1);
    }

    @Test
    void shouldNormalizeChineseMoneyUnitsAndPositiveOutflow() {
        String body = """
                {"data":[{
                  "股票代码":"000001",
                  "更新时间":"2026-07-24 10:01:20",
                  "流入资金":"1.2亿元",
                  "流出资金":"-300万元",
                  "净额":"9000万元",
                  "最新价":"12.34",
                  "涨跌幅":"1.25%",
                  "成交额":"2.5亿",
                  "换手率":"3.21%"
                }]}
                """;
        AkToolsMarketClient client = client(200, body);

        var point = client.fetchIndividualFundFlow().get(0);

        assertThat(point.stockCode()).isEqualTo("000001");
        assertThat(point.inflowAmount()).isEqualByComparingTo("120000000");
        assertThat(point.outflowAmount()).isEqualByComparingTo("3000000");
        assertThat(point.netAmount()).isEqualByComparingTo("90000000");
        assertThat(point.changePercent()).isEqualByComparingTo("1.25");
    }

    @Test
    void shouldRejectHttpError() {
        AkToolsMarketClient client = client(503, "{}");

        assertThatThrownBy(client::fetchIndividualFundFlow)
                .isInstanceOf(ServiceException.class)
                .hasMessageContaining("HTTP状态异常");
    }

    @Test
    void shouldRejectInvalidJsonAndKeepMissingNumbersAsNull() {
        AkToolsMarketClient invalidJsonClient = client(200, "{bad-json");
        assertThatThrownBy(invalidJsonClient::fetchIndividualFundFlow)
                .isInstanceOf(ServiceException.class)
                .hasMessageContaining("JSON");

        AkToolsMarketClient missingFieldClient = client(200, """
                {"data":[{"股票代码":"000001","流入资金":"1万"}]}
                """);
        var point = missingFieldClient.fetchIndividualFundFlow().get(0);
        assertThat(point.inflowAmount()).isEqualByComparingTo("10000");
        assertThat(point.outflowAmount()).isNull();
        assertThat(point.latestPrice()).isNull();
        assertThat(point.sourceDataTime()).isNull();
    }

    @Test
    void shouldUseSinaIndexFunctionAndReturnOnlyFixedNineCodes() {
        String body = """
                {"data":[
                  {"代码":"sh000001","最新价":"3000","涨跌额":"10","涨跌幅":"0.3","成交额":"1亿"},
                  {"代码":"sz399001","最新价":"9000"},
                  {"代码":"sz399006","最新价":"2000"},
                  {"代码":"sh000688","最新价":"1000"},
                  {"代码":"sh000680","最新价":"900"},
                  {"代码":"sh000510","最新价":"5000"},
                  {"代码":"sh000300","最新价":"4000"},
                  {"代码":"sh000016","最新价":"2500"},
                  {"代码":"sz399330","最新价":"6000"},
                  {"代码":"bj899050","最新价":"1200"}
                ]}
                """;
        AtomicReference<String> requestedUrl = new AtomicReference<>();
        MarketProperty property = property();
        AkToolsMarketClient client = new AkToolsMarketClient(property, url -> {
            requestedUrl.set(url);
            return new AkToolsMarketClient.MarketHttpResponse(200, body);
        });

        var spots = client.fetchIndexSpots();

        assertThat(requestedUrl.get()).endsWith("/api/public/stock_zh_index_spot_sina");
        assertThat(spots.keySet()).containsExactly(
                "000001", "399001", "399006", "000688", "000680",
                "000510", "000300", "000016", "399330");
        assertThat(spots).doesNotContainKey("899050");
    }

    @Test
    void shouldKeepBeijingExchangeStocksInMarketSnapshot() {
        AkToolsMarketClient client = client(200, """
                {"data":[{
                  "代码":"832149",
                  "名称":"利尔达",
                  "最新价":"17.29",
                  "涨跌幅":"30",
                  "成交额":"1000000"
                }]}
                """);

        var quote = client.fetchMarketSnapshot().get(0);

        assertThat(quote.stockCode()).isEqualTo("832149");
        assertThat(quote.market()).isEqualTo("BJ");
    }

    @Test
    void shouldReplaceDuplicateMinuteAndExposeFiveFundCategories() {
        MarketProperty property = property();
        AkToolsMarketClient client = new AkToolsMarketClient(property, url -> {
            if (url.contains("index_zh_a_hist_min_em")) {
                return new AkToolsMarketClient.MarketHttpResponse(200, """
                        {"data":[
                          {"时间":"2026-07-28 10:00:01","收盘":"3000"},
                          {"时间":"2026-07-28 10:00:59","收盘":"3001"}
                        ]}
                        """);
            }
            return new AkToolsMarketClient.MarketHttpResponse(200, """
                    {"data":[{
                      "日期":"2026-07-28",
                      "主力净流入-净额":"1亿",
                      "超大单净流入-净额":"2万",
                      "大单净流入-净额":"3万",
                      "中单净流入-净额":"-4万",
                      "小单净流入-净额":"-5万"
                    }]}
                    """);
        });

        assertThat(client.fetchIndexCurve("000001"))
                .singleElement()
                .satisfies(point -> assertThat(point.value()).isEqualByComparingTo("3001"));
        var fund = client.fetchMarketFundFlow().get(0);
        assertThat(fund.mainNetInflow()).isEqualByComparingTo("100000000");
        assertThat(fund.superLargeNetInflow()).isEqualByComparingTo("20000");
        assertThat(fund.smallNetInflow()).isEqualByComparingTo("-50000");
    }

    @Test
    void shouldApplyFunctionSpecificIndustryAndMutationUnits() {
        MarketProperty property = property();
        AkToolsMarketClient client = new AkToolsMarketClient(property, url -> {
            if (url.contains("stock_board_industry_name_em")) {
                return new AkToolsMarketClient.MarketHttpResponse(200, """
                        {"data":[{"板块代码":"BK001","板块名称":"传媒","总市值":"500000000"}]}
                        """);
            }
            if (url.contains("stock_board_industry_summary_ths")) {
                return new AkToolsMarketClient.MarketHttpResponse(200, """
                        {"data":[{"板块":"传媒","总成交额":"278.19","净流入":"1.5"}]}
                        """);
            }
            return new AkToolsMarketClient.MarketHttpResponse(200, """
                    {"data":[{
                      "板块名称":"传媒",
                      "主力净流入":"123.45",
                      "板块异动总次数":"9",
                      "板块异动最频繁个股及所属类型-股票名称":"甲公司",
                      "板块异动最频繁个股及所属类型-买卖方向":"大笔买入"
                    }]}
                    """);
        });

        var marketIndustries = client.fetchIndustryMarketSectors();
        var summaryIndustries = client.fetchIndustrySummarySectors();
        assertThat(marketIndustries.get(0).marketCap()).isEqualByComparingTo("500000000");
        assertThat(summaryIndustries.get(0).turnover()).isEqualByComparingTo("27819000000");
        assertThat(summaryIndustries.get(0).mainNetInflow()).isEqualByComparingTo("150000000");
        var mutation = client.fetchMutations().get(0);
        assertThat(mutation.mainNetInflow()).isEqualByComparingTo("1234500");
        assertThat(mutation.mutationCount()).isEqualTo(9);
        assertThat(mutation.frequentStock()).isEqualTo("甲公司");
    }

    @Test
    void shouldSendLowercaseSectorRankingParametersToAkTools() {
        List<String> requestedUrls = new ArrayList<>();
        MarketProperty property = property();
        AkToolsMarketClient client = new AkToolsMarketClient(property, url -> {
            requestedUrls.add(url);
            return new AkToolsMarketClient.MarketHttpResponse(200, """
                    {"data":[{
                      "板块代码":"BK001",
                      "名称":"传媒",
                      "涨跌幅":"1.2",
                      "主力净流入-净额":"1000000"
                    }]}
                    """);
        });

        var rankings = client.fetchSectorRankings();

        assertThat(rankings).hasSize(6);
        assertThat(rankings).extracting(MarketDataModels.SectorRanking::period)
                .containsOnly("today", "5d", "10d");
        assertThat(requestedUrls).allSatisfy(url -> {
            assertThat(url).contains("/api/public/stock_sector_fund_flow_rank");
            assertThat(url).containsPattern("indicator=(today|5d|10d)");
            assertThat(url).containsPattern("sector_type=(industry|concept)");
        });
        assertThat(requestedUrls)
                .noneMatch(url -> url.contains("INDUSTRY") || url.contains("%E8%A1%8C%E4%B8%9A"));
    }

    private AkToolsMarketClient client(int status, String body) {
        MarketProperty property = property();
        return new AkToolsMarketClient(
                property,
                url -> new AkToolsMarketClient.MarketHttpResponse(status, body));
    }

    private MarketProperty property() {
        MarketProperty property = new MarketProperty();
        property.setRetryTimes(0);
        property.setConnectTimeoutMs(500);
        property.setReadTimeoutMs(1000);
        return property;
    }
}
