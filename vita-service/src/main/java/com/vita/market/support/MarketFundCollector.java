package com.vita.market.support;

import com.vita.market.enums.MarketDataStatus;
import com.vita.market.model.MarketCollectionResult;
import com.vita.market.model.MarketDataModels;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 市场五类资金基础序列采集器。
 */
@Component
public class MarketFundCollector {

    private final AkToolsMarketClient marketClient;

    public MarketFundCollector(AkToolsMarketClient marketClient) {
        this.marketClient = marketClient;
    }

    public MarketCollectionResult<List<MarketDataModels.MarketFundPoint>> collect() {
        try {
            return MarketCollectionResult.fresh(marketClient.fetchMarketFundFlow(), null);
        } catch (RuntimeException error) {
            return new MarketCollectionResult<>(
                    null, MarketDataStatus.ERROR, null, error.getMessage());
        }
    }
}
