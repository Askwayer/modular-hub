package com.vita.market.support;

import com.vita.market.enums.MarketTradeStatus;
import com.vita.market.model.MarketTradeContext;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Comparator;
import java.util.List;

/**
 * @BelongsProject: be-vita
 * @BelongsPackage: com.vita.market.support
 * @Author: znk
 * @CreateTime: 2026-07-25
 * @Description: 基于上海时区和交易日历的 A股交易时钟
 * @Version: 1.0
 */
@Component
public class MarketTradeClock {

    private static final LocalTime MORNING_OPEN = LocalTime.of(9, 30);
    private static final LocalTime MORNING_CLOSE = LocalTime.of(11, 30);
    private static final LocalTime AFTERNOON_OPEN = LocalTime.of(13, 0);
    private static final LocalTime AFTERNOON_CLOSE = LocalTime.of(15, 0);

    /**
     * 交易状态只依据传入日历判断，禁止在日历缺失时猜测法定节假日。
     *
     * @param now        上海时区当前时间
     * @param tradeDates 已缓存交易日历
     * @return 当前交易上下文
     */
    public MarketTradeContext resolve(LocalDateTime now, List<LocalDate> tradeDates) {
        LocalDate today = now.toLocalDate();
        if (tradeDates == null || tradeDates.isEmpty()) {
            return new MarketTradeContext(MarketTradeStatus.UNKNOWN, today, null);
        }
        LocalDate recentDate = tradeDates.stream()
                .filter(date -> !date.isAfter(today))
                .max(Comparator.naturalOrder())
                .orElse(null);
        if (!tradeDates.contains(today)) {
            return new MarketTradeContext(MarketTradeStatus.NON_TRADING_DAY, today, recentDate);
        }
        LocalTime time = now.toLocalTime();
        if (time.isBefore(MORNING_OPEN)) {
            return new MarketTradeContext(MarketTradeStatus.PRE_OPEN, today, today);
        }
        if (time.isBefore(MORNING_CLOSE)) {
            return new MarketTradeContext(MarketTradeStatus.TRADING, today, today);
        }
        if (time.isBefore(AFTERNOON_OPEN)) {
            return new MarketTradeContext(MarketTradeStatus.LUNCH_BREAK, today, today);
        }
        if (time.isBefore(AFTERNOON_CLOSE)) {
            return new MarketTradeContext(MarketTradeStatus.TRADING, today, today);
        }
        return new MarketTradeContext(MarketTradeStatus.CLOSED, today, today);
    }
}
