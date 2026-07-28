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
 * @Description: A股市场资金趋势接口
 * @Version: 1.0
 */
@RestController
@RequestMapping("/market/fund")
public class MarketFundController {

    private final IMarketDashboardService dashboardService;

    public MarketFundController(IMarketDashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    /**
     * 查询5、10或20日市场资金和指数对照线。
     *
     * @param days      天数
     * @param indexCode 固定指数代码
     * @return 资金趋势
     */
    @GetMapping("/trend")
    @SaCheckPermission("market:dashboard:view")
    public CommonResult<MarketModuleResult<List<MarketDataModels.FundTrendPoint>>> trend(
            @RequestParam(name = "days", defaultValue = "20") Integer days,
            @RequestParam(name = "indexCode", defaultValue = "000001") String indexCode) {
        return CommonResult.success(dashboardService.fundTrend(days, indexCode));
    }
}
