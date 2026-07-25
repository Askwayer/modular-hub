package com.vita.market.service.impl;

import cn.hutool.core.text.CharSequenceUtil;
import com.vita.core.exception.GlobalErrorCode;
import com.vita.core.exception.ServiceException;
import com.vita.core.page.PageRequest;
import com.vita.core.page.PageResponse;
import com.vita.market.constant.MarketConstants;
import com.vita.market.dto.MarketStockSearchDto;
import com.vita.market.entity.MarketDailySummary;
import com.vita.market.entity.MarketStock;
import com.vita.market.entity.MarketStockFundPoint;
import com.vita.market.mapper.MarketDailySummaryMapper;
import com.vita.market.mapper.MarketStockFundPointMapper;
import com.vita.market.mapper.MarketStockMapper;
import com.vita.market.model.MarketDataModels;
import com.vita.market.property.MarketProperty;
import com.vita.market.service.IMarketStockService;
import com.vita.market.support.AkToolsMarketClient;
import com.vita.mybatis.wrapper.LambdaQueryWrapperX;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * @BelongsProject: be-vita
 * @BelongsPackage: com.vita.market.service.impl
 * @Author: znk
 * @CreateTime: 2026-07-25
 * @Description: 股票字典、监控配置和采样持久化服务实现
 * @Version: 1.0
 */
@Service
public class MarketStockServiceImpl implements IMarketStockService {

    private final MarketStockMapper stockMapper;
    private final MarketStockFundPointMapper fundPointMapper;
    private final MarketDailySummaryMapper dailySummaryMapper;
    private final AkToolsMarketClient marketClient;
    private final RedissonClient redissonClient;
    private final MarketProperty property;

    public MarketStockServiceImpl(MarketStockMapper stockMapper,
                                  MarketStockFundPointMapper fundPointMapper,
                                  MarketDailySummaryMapper dailySummaryMapper,
                                  AkToolsMarketClient marketClient,
                                  RedissonClient redissonClient,
                                  MarketProperty property) {
        this.stockMapper = stockMapper;
        this.fundPointMapper = fundPointMapper;
        this.dailySummaryMapper = dailySummaryMapper;
        this.marketClient = marketClient;
        this.redissonClient = redissonClient;
        this.property = property;
    }

    /**
     * 股票字典分页查询只访问 market_stock 单表。
     *
     * @param searchDto 查询条件
     * @return 股票字典分页
     */
    @Override
    public PageResponse<MarketDataModels.StockDictionaryView> pageDictionary(MarketStockSearchDto searchDto) {
        MarketStockSearchDto query = searchDto == null ? new MarketStockSearchDto() : searchDto;
        LambdaQueryWrapperX<MarketStock> wrapper = new LambdaQueryWrapperX<>();
        wrapper.likeIfPresent(MarketStock::getStockCode, query.getStockCode())
                .likeIfPresent(MarketStock::getStockName, query.getStockName())
                .eqIfPresent(MarketStock::getMarket, normalize(query.getMarket()))
                .eqIfPresent(MarketStock::getListingStatus, normalize(query.getListingStatus()))
                .eqIfPresent(MarketStock::getMonitorEnabled, query.getMonitorEnabled())
                .orderByAsc(MarketStock::getStockCode);
        PageResponse<MarketStock> page = stockMapper.selectPage(query, wrapper);
        return new PageResponse<>(page.getList().stream().map(this::toDictionaryView).toList(), page.getTotal());
    }

    /**
     * 每页固定返回4只启用股票，资金曲线按股票逐表查询，避免跨表 JOIN。
     *
     * @param keyword 名称或编号
     * @param pageNum 页码
     * @return 监控股票分页
     */
    @Override
    public PageResponse<MarketDataModels.StockMonitorItem> pageMonitor(String keyword, Integer pageNum) {
        PageRequest pageRequest = new PageRequest();
        pageRequest.setPageNum(pageNum == null ? 1 : pageNum);
        pageRequest.setPageSize(MarketConstants.MONITOR_PAGE_SIZE);
        LambdaQueryWrapperX<MarketStock> wrapper = new LambdaQueryWrapperX<>();
        wrapper.eq(MarketStock::getMonitorEnabled, true);
        if (CharSequenceUtil.isNotBlank(keyword)) {
            wrapper.and(query -> query.like(MarketStock::getStockCode, keyword)
                    .or()
                    .like(MarketStock::getStockName, keyword));
        }
        wrapper.orderByAsc(MarketStock::getDisplayOrder)
                .orderByAsc(MarketStock::getEnabledTime);
        PageResponse<MarketStock> page = stockMapper.selectPage(pageRequest, wrapper);
        LocalDate today = LocalDate.now(MarketConstants.SHANGHAI_ZONE);
        List<MarketDataModels.StockMonitorItem> items = page.getList().stream().map(stock -> {
            List<MarketDataModels.StockFundSnapshot> curve = listFundPoints(stock.getStockCode(), today);
            MarketDataModels.StockFundSnapshot latest = curve.isEmpty() ? null : curve.get(curve.size() - 1);
            return new MarketDataModels.StockMonitorItem(toDictionaryView(stock), latest, curve);
        }).toList();
        return new PageResponse<>(items, page.getTotal());
    }

    /**
     * 新启用仅允许正常上市股票；锁内重新统计数量，防止并发突破10只限制。
     *
     * @param stockCode 股票编号
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void enable(String stockCode) {
        withStockManageLock(() -> {
            MarketStock stock = requiredStock(stockCode);
            if (Boolean.TRUE.equals(stock.getMonitorEnabled())) {
                return null;
            }
            if (!MarketConstants.LISTED.equals(stock.getListingStatus())) {
                throw business("仅正常上市股票可以启用监控");
            }
            Long count = stockMapper.selectCount(new LambdaQueryWrapperX<MarketStock>()
                    .eq(MarketStock::getMonitorEnabled, true));
            if (count >= MarketConstants.MAX_MONITORED_STOCKS) {
                throw business("最多允许启用10只股票");
            }
            Integer maxOrder = stockMapper.selectList(new LambdaQueryWrapperX<MarketStock>()
                            .eq(MarketStock::getMonitorEnabled, true)
                            .orderByDesc(MarketStock::getDisplayOrder)
                            .last("LIMIT 1"))
                    .stream()
                    .map(MarketStock::getDisplayOrder)
                    .findFirst()
                    .orElse(0);
            MarketStock update = new MarketStock();
            update.setId(stock.getId());
            update.setMonitorEnabled(true);
            update.setEnabledTime(LocalDateTime.now(MarketConstants.SHANGHAI_ZONE));
            update.setDisplayOrder(maxOrder + 1);
            stockMapper.updateById(update);
            return null;
        });
    }

    /**
     * 停用只更新监控配置，不删除任何历史采样。
     *
     * @param stockCode 股票编号
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void disable(String stockCode) {
        withStockManageLock(() -> {
            MarketStock stock = requiredStock(stockCode);
            MarketStock update = new MarketStock();
            update.setId(stock.getId());
            update.setMonitorEnabled(false);
            update.setDisplayOrder(0);
            stockMapper.updateById(update);
            return null;
        });
    }

    /**
     * 排序请求必须与当前启用股票集合完全相同，避免局部排序造成并发丢失。
     *
     * @param stockCodes 完整股票编号顺序
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void reorder(List<String> stockCodes) {
        withStockManageLock(() -> {
            List<String> normalizedCodes = stockCodes == null
                    ? List.of()
                    : stockCodes.stream().map(this::normalizeCode).toList();
            Set<String> requested = new HashSet<>(normalizedCodes);
            if (requested.size() != normalizedCodes.size()) {
                throw business("排序股票编号不能重复");
            }
            List<MarketStock> enabled = listEnabledStocks();
            Set<String> current = enabled.stream().map(MarketStock::getStockCode).collect(java.util.stream.Collectors.toSet());
            if (!requested.equals(current) || normalizedCodes.size() != current.size()) {
                throw business("排序请求必须完整包含当前全部启用股票");
            }
            Map<String, MarketStock> byCode = new LinkedHashMap<>();
            enabled.forEach(stock -> byCode.put(stock.getStockCode(), stock));
            for (int index = 0; index < normalizedCodes.size(); index++) {
                MarketStock update = new MarketStock();
                update.setId(byCode.get(normalizedCodes.get(index)).getId());
                update.setDisplayOrder(index + 1);
                stockMapper.updateById(update);
            }
            return null;
        });
    }

    /**
     * 字典同步按股票编号更新名称和上市状态，不删除历史股票。
     * 明确退市时立即停用，暂停上市则保留启用配置。
     *
     * @return 同步记录数
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int syncDictionary() {
        RLock lock = redissonClient.getLock(MarketConstants.DICTIONARY_SYNC_LOCK_KEY);
        boolean locked = false;
        try {
            locked = lock.tryLock(0, TimeUnit.SECONDS);
            if (!locked) {
                throw new ServiceException(GlobalErrorCode.LOCKED);
            }
            List<MarketDataModels.StockDictionaryItem> items = marketClient.fetchStockDictionary();
            for (MarketDataModels.StockDictionaryItem item : items) {
                upsertStock(item);
            }
            return items.size();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ServiceException(GlobalErrorCode.LOCKED);
        } finally {
            if (locked && lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    @Override
    public List<MarketStock> listEnabledStocks() {
        return stockMapper.selectList(new LambdaQueryWrapperX<MarketStock>()
                .eq(MarketStock::getMonitorEnabled, true)
                .orderByAsc(MarketStock::getDisplayOrder)
                .orderByAsc(MarketStock::getEnabledTime));
    }

    /**
     * 只保存启用且正常上市股票的完整资金点，按股票编号和40秒桶幂等。
     *
     * @param points 数据源资金点
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveFundPoints(List<MarketDataModels.StockFundSnapshot> points) {
        if (points == null || points.isEmpty()) {
            return;
        }
        Set<String> enabledCodes = listEnabledStocks().stream()
                .filter(stock -> MarketConstants.LISTED.equals(stock.getListingStatus()))
                .map(MarketStock::getStockCode)
                .collect(java.util.stream.Collectors.toSet());
        for (MarketDataModels.StockFundSnapshot point : points) {
            if (!enabledCodes.contains(point.stockCode()) || !isComplete(point)) {
                continue;
            }
            LocalDateTime bucket = sampleBucket(point.sampleTime());
            Long count = fundPointMapper.selectCount(new LambdaQueryWrapperX<MarketStockFundPoint>()
                    .eq(MarketStockFundPoint::getStockCode, point.stockCode())
                    .eq(MarketStockFundPoint::getSampleBucket, bucket));
            if (count > 0) {
                continue;
            }
            MarketStockFundPoint entity = toFundPoint(point, bucket);
            try {
                fundPointMapper.insert(entity);
            } catch (DuplicateKeyException ignored) {
                // 多实例在同一40秒桶落点时由唯一键完成最终幂等。
            }
        }
        LocalDate expiredBefore = LocalDate.now(MarketConstants.SHANGHAI_ZONE)
                .minusDays(property.getFundPointRetentionDays());
        fundPointMapper.delete(new LambdaQueryWrapperX<MarketStockFundPoint>()
                .lt(MarketStockFundPoint::getTradeDate, expiredBefore));
    }

    /**
     * 市场日汇总按交易日幂等更新，收盘定稿不会产生第二条记录。
     *
     * @param summary 日汇总
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveDailySummary(MarketDataModels.DailySummaryData summary) {
        MarketDailySummary current = dailySummaryMapper.selectOne(
                MarketDailySummary::getTradeDate, summary.tradeDate());
        MarketDailySummary entity = new MarketDailySummary();
        entity.setTradeDate(summary.tradeDate());
        entity.setShTurnoverAmount(summary.shTurnoverAmount());
        entity.setSzTurnoverAmount(summary.szTurnoverAmount());
        entity.setBjTurnoverAmount(summary.bjTurnoverAmount());
        entity.setUpCount(summary.upCount());
        entity.setDownCount(summary.downCount());
        entity.setFlatCount(summary.flatCount());
        entity.setLimitUpCount(summary.limitUpCount());
        entity.setLimitDownCount(summary.limitDownCount());
        entity.setMainNetInflow(summary.mainNetInflow());
        entity.setLastValidTime(summary.lastValidTime());
        entity.setIsFinalized(summary.finalized());
        if (current == null) {
            dailySummaryMapper.insert(entity);
        } else {
            entity.setId(current.getId());
            dailySummaryMapper.updateById(entity);
        }
    }

    @Override
    public List<MarketDataModels.StockFundSnapshot> listFundPoints(String stockCode, LocalDate tradeDate) {
        return fundPointMapper.selectList(new LambdaQueryWrapperX<MarketStockFundPoint>()
                        .eq(MarketStockFundPoint::getStockCode, normalizeCode(stockCode))
                        .eq(MarketStockFundPoint::getTradeDate, tradeDate)
                        .orderByAsc(MarketStockFundPoint::getSampleBucket))
                .stream()
                .map(this::toFundSnapshot)
                .toList();
    }

    private void upsertStock(MarketDataModels.StockDictionaryItem item) {
        MarketStock current = stockMapper.selectOne(MarketStock::getStockCode, item.stockCode());
        MarketStock entity = new MarketStock();
        entity.setStockCode(item.stockCode());
        entity.setStockName(item.stockName());
        entity.setMarket(item.market());
        entity.setListingStatus(item.listingStatus());
        entity.setSourceUpdateTime(item.sourceUpdateTime());
        if (current == null) {
            entity.setMonitorEnabled(false);
            entity.setDisplayOrder(0);
            stockMapper.insert(entity);
            return;
        }
        entity.setId(current.getId());
        if (MarketConstants.DELISTED.equals(item.listingStatus())) {
            entity.setMonitorEnabled(false);
            entity.setDisplayOrder(0);
        }
        stockMapper.updateById(entity);
    }

    private MarketStock requiredStock(String stockCode) {
        String code = normalizeCode(stockCode);
        MarketStock stock = stockMapper.selectOne(MarketStock::getStockCode, code);
        if (stock == null) {
            throw new ServiceException(GlobalErrorCode.NOT_FOUND.getCode(), "股票编号不存在");
        }
        return stock;
    }

    private <T> T withStockManageLock(Supplier<T> operation) {
        RLock lock = redissonClient.getLock(MarketConstants.STOCK_MANAGE_LOCK_KEY);
        boolean locked = false;
        try {
            locked = lock.tryLock(0, property.getRefreshLockLeaseSeconds(), TimeUnit.SECONDS);
            if (!locked) {
                throw new ServiceException(GlobalErrorCode.LOCKED);
            }
            return operation.get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ServiceException(GlobalErrorCode.LOCKED);
        } finally {
            if (locked && lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    private MarketDataModels.StockDictionaryView toDictionaryView(MarketStock stock) {
        return new MarketDataModels.StockDictionaryView(
                stock.getStockCode(),
                stock.getStockName(),
                stock.getMarket(),
                stock.getListingStatus(),
                Boolean.TRUE.equals(stock.getMonitorEnabled()),
                stock.getEnabledTime(),
                stock.getDisplayOrder(),
                stock.getSourceUpdateTime()
        );
    }

    private MarketStockFundPoint toFundPoint(MarketDataModels.StockFundSnapshot point, LocalDateTime bucket) {
        MarketStockFundPoint entity = new MarketStockFundPoint();
        entity.setStockCode(point.stockCode());
        entity.setTradeDate(point.tradeDate());
        entity.setSampleBucket(bucket);
        entity.setSampleTime(point.sampleTime());
        entity.setInflowAmount(point.inflowAmount());
        entity.setOutflowAmount(point.outflowAmount().abs());
        entity.setNetAmount(point.netAmount());
        entity.setLatestPrice(point.latestPrice());
        entity.setChangePercent(point.changePercent());
        entity.setTurnoverAmount(point.turnoverAmount());
        entity.setTurnoverRate(point.turnoverRate());
        return entity;
    }

    private MarketDataModels.StockFundSnapshot toFundSnapshot(MarketStockFundPoint point) {
        return new MarketDataModels.StockFundSnapshot(
                point.getStockCode(),
                point.getTradeDate(),
                point.getSampleTime(),
                point.getInflowAmount(),
                point.getOutflowAmount(),
                point.getNetAmount(),
                point.getLatestPrice(),
                point.getChangePercent(),
                point.getTurnoverAmount(),
                point.getTurnoverRate()
        );
    }

    private boolean isComplete(MarketDataModels.StockFundSnapshot point) {
        return point.stockCode() != null
                && point.tradeDate() != null
                && point.sampleTime() != null
                && point.inflowAmount() != null
                && point.outflowAmount() != null
                && point.netAmount() != null
                && point.latestPrice() != null
                && point.changePercent() != null
                && point.turnoverAmount() != null
                && point.turnoverRate() != null;
    }

    private LocalDateTime sampleBucket(LocalDateTime sampleTime) {
        long epoch = sampleTime.toEpochSecond(ZoneOffset.ofHours(8));
        long bucket = epoch - Math.floorMod(epoch, MarketConstants.SAMPLE_BUCKET_SECONDS);
        return LocalDateTime.ofEpochSecond(bucket, 0, ZoneOffset.ofHours(8));
    }

    private String normalizeCode(String stockCode) {
        if (CharSequenceUtil.isBlank(stockCode)) {
            throw new ServiceException(GlobalErrorCode.BAD_REQUEST.getCode(), "股票编号不能为空");
        }
        String value = stockCode.trim().toUpperCase();
        return value.replaceFirst("^(SH|SZ|BJ)", "");
    }

    private String normalize(String value) {
        return CharSequenceUtil.isBlank(value) ? null : value.trim().toUpperCase();
    }

    private ServiceException business(String message) {
        return new ServiceException(GlobalErrorCode.BUSINESS_EXCEPTION.getCode(), message);
    }
}
