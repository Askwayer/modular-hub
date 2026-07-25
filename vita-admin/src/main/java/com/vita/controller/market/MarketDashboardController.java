package com.vita.controller.market;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.vita.core.CommonResult;
import com.vita.log.annotation.Log;
import com.vita.market.model.MarketDataModels;
import com.vita.market.service.IMarketDashboardService;
import com.vita.market.service.IMarketRefreshService;
import com.vita.market.vo.MarketDashboardStatusVo;
import com.vita.market.vo.MarketModuleResult;
import com.vita.repeat.annotation.RepeatSubmit;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * @BelongsProject: be-vita
 * @BelongsPackage: com.vita.controller.market
 * @Author: znk
 * @CreateTime: 2026-07-25
 * @Description: A股行情大屏状态和指数接口
 * @Version: 1.0
 */
@RestController
@RequestMapping("/market/dashboard")
public class MarketDashboardController {

    private final IMarketDashboardService dashboardService;
    private final IMarketRefreshService refreshService;

    public MarketDashboardController(IMarketDashboardService dashboardService,
                                     IMarketRefreshService refreshService) {
        this.dashboardService = dashboardService;
        this.refreshService = refreshService;
    }

    /**
     * 查询交易和模块刷新状态。
     *
     * @return 大屏状态
     */
    @GetMapping("/status")
    @SaCheckPermission("market:dashboard:view")
    public CommonResult<MarketDashboardStatusVo> status() {
        return CommonResult.success(dashboardService.status());
    }

    /**
     * 查询固定十大指数及当日曲线。
     *
     * @return 指数行情
     */
    @GetMapping("/indices")
    @SaCheckPermission("market:dashboard:view")
    public CommonResult<MarketModuleResult<List<MarketDataModels.IndexQuote>>> indices() {
        return CommonResult.success(dashboardService.indices());
    }

    /**
     * 查询全市场日汇总。
     *
     * @return 市场汇总
     */
    @GetMapping("/summary")
    @SaCheckPermission("market:dashboard:view")
    public CommonResult<MarketModuleResult<MarketDataModels.DailySummaryData>> summary() {
        return CommonResult.success(dashboardService.summary());
    }

    /**
     * 触发受限频保护的异步全局刷新。
     *
     * @return 是否接受刷新任务
     */
    @Log(module = "行情大屏", businessType = "REFRESH", saveResponseData = false)
    @RepeatSubmit
    @PostMapping("/refresh")
    @SaCheckPermission("market:dashboard:view")
    public CommonResult<Boolean> refresh() {
        return CommonResult.success(refreshService.triggerManualRefresh());
    }
}
