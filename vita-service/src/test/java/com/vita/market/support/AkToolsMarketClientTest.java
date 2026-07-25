package com.vita.market.support;

import com.vita.core.exception.ServiceException;
import com.vita.market.property.MarketProperty;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * AKTools 中文字段、金额单位和错误响应夹具测试。
 */
class AkToolsMarketClientTest {

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
    void shouldRejectInvalidJsonAndMissingRequiredField() {
        AkToolsMarketClient invalidJsonClient = client(200, "{bad-json");
        assertThatThrownBy(invalidJsonClient::fetchIndividualFundFlow)
                .isInstanceOf(ServiceException.class)
                .hasMessageContaining("JSON");

        AkToolsMarketClient missingFieldClient = client(200, """
                {"data":[{"股票代码":"000001","流入资金":"1万"}]}
                """);
        assertThatThrownBy(missingFieldClient::fetchIndividualFundFlow)
                .isInstanceOf(ServiceException.class)
                .hasMessageContaining("缺少字段");
    }

    private AkToolsMarketClient client(int status, String body) {
        MarketProperty property = new MarketProperty();
        property.setRetryTimes(0);
        property.setConnectTimeoutMs(500);
        property.setReadTimeoutMs(1000);
        return new AkToolsMarketClient(
                property,
                url -> new AkToolsMarketClient.MarketHttpResponse(status, body));
    }
}
