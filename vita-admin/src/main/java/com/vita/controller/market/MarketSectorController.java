package com.vita.controller.market;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.vita.core.CommonResult;
import com.vita.market.model.MarketDataModels;
import com.vita.market.service.IMarketDashboardService;
import com.vita.market.vo.MarketModuleResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * @BelongsProject: be-vita
 * @BelongsPackage: com.vita.controller.market
 * @Author: znk
 * @CreateTime: 2026-07-25
 * @Description: A股板块热力、排行和异动接口
 * @Version: 1.0
 */
@RestController
@RequestMapping("/market/sector")
public class MarketSectorController {

    private final IMarketDashboardService dashboardService;

    public MarketSectorController(IMarketDashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    /**
     * 查询板块热力图数据。
     *
     * @param sectorType 板块类型
     * @param areaMetric 面积口径
     * @param direction  涨跌方向
     * @param keyword    关键字
     * @return 板块快照
     */
    @GetMapping("/heatmap")
    @SaCheckPermission("market:dashboard:view")
    public CommonResult<MarketModuleResult<MarketDataModels.SectorHeatmapData>> heatmap(
            @RequestParam(name = "sectorType", defaultValue = "industry") String sectorType,
            @RequestParam(name = "areaMetric", required = false) String areaMetric,
            @RequestParam(name = "direction", defaultValue = "all") String direction,
            @RequestParam(name = "keyword", required = false) String keyword) {
        return CommonResult.success(dashboardService.heatmap(sectorType, areaMetric, direction, keyword));
    }

    /**
     * 查询所选板块 TOP5 成分股。
     *
     * @param sectorType 板块类型
     * @param sectorCode 板块代码或名称
     * @param metric     排序口径
     * @return 成分股 TOP5
     */
    @GetMapping("/topStocks")
    @SaCheckPermission("market:dashboard:view")
    public CommonResult<MarketModuleResult<List<MarketDataModels.SectorTopStock>>> topStocks(
            @RequestParam(name = "sectorType", defaultValue = "industry") String sectorType,
            @RequestParam(name = "sectorCode") String sectorCode,
            @RequestParam(name = "metric", defaultValue = "changePercent") String metric) {
        return CommonResult.success(dashboardService.topStocks(sectorType, sectorCode, metric));
    }

    /**
     * 查询板块资金强弱排行。
     *
     * @param period     今日、5日或10日
     * @param sectorType 板块类型
     * @return 板块排行
     */
    @GetMapping("/rankings")
    @SaCheckPermission("market:dashboard:view")
    public CommonResult<MarketModuleResult<List<MarketDataModels.SectorRanking>>> rankings(
            @RequestParam(name = "period", defaultValue = "今日") String period,
            @RequestParam(name = "sectorType", defaultValue = "industry") String sectorType) {
        return CommonResult.success(dashboardService.rankings(period, sectorType));
    }

    /**
     * 查询最近板块异动。
     *
     * @return 板块异动
     */
    @GetMapping("/mutations")
    @SaCheckPermission("market:dashboard:view")
    public CommonResult<MarketModuleResult<List<MarketDataModels.MarketMutation>>> mutations() {
        return CommonResult.success(dashboardService.mutations());
    }
}
