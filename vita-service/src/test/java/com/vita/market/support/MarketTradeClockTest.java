package com.vita.market.support;

import com.vita.market.enums.MarketTradeStatus;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * A股交易时钟边界测试。
 */
class MarketTradeClockTest {

    private final MarketTradeClock tradeClock = new MarketTradeClock();
    private final LocalDate tradeDate = LocalDate.of(2026, 7, 24);
    private final List<LocalDate> calendar = List.of(
            LocalDate.of(2026, 7, 23),
            tradeDate,
            LocalDate.of(2026, 7, 27));

    @Test
    void shouldResolveTradingSessions() {
        assertThat(resolve(9, 29)).isEqualTo(MarketTradeStatus.PRE_OPEN);
        assertThat(resolve(9, 30)).isEqualTo(MarketTradeStatus.TRADING);
        assertThat(resolve(11, 30)).isEqualTo(MarketTradeStatus.LUNCH_BREAK);
        assertThat(resolve(13, 0)).isEqualTo(MarketTradeStatus.TRADING);
        assertThat(resolve(15, 0)).isEqualTo(MarketTradeStatus.CLOSED);
    }

    @Test
    void shouldUseRecentTradeDateOnWeekend() {
        var context = tradeClock.resolve(
                LocalDateTime.of(2026, 7, 25, 10, 0), calendar);

        assertThat(context.tradeStatus()).isEqualTo(MarketTradeStatus.NON_TRADING_DAY);
        assertThat(context.effectiveTradeDate()).isEqualTo(tradeDate);
    }

    @Test
    void shouldReturnUnknownWithoutCalendar() {
        var context = tradeClock.resolve(
                LocalDateTime.of(2026, 7, 24, 10, 0), List.of());

        assertThat(context.tradeStatus()).isEqualTo(MarketTradeStatus.UNKNOWN);
        assertThat(context.effectiveTradeDate()).isNull();
    }

    private MarketTradeStatus resolve(int hour, int minute) {
        return tradeClock.resolve(
                LocalDateTime.of(tradeDate, java.time.LocalTime.of(hour, minute)),
                calendar).tradeStatus();
    }
}
