package com.vita.controller.market;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.vita.core.CommonResult;
import com.vita.core.page.PageResponse;
import com.vita.log.annotation.Log;
import com.vita.market.dto.MarketStockCodeRequest;
import com.vita.market.dto.MarketStockReorderRequest;
import com.vita.market.dto.MarketStockSearchDto;
import com.vita.market.model.MarketDataModels;
import com.vita.market.service.IMarketRefreshService;
import com.vita.market.service.IMarketStockService;
import com.vita.repeat.annotation.RepeatSubmit;
import jakarta.validation.Valid;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * @BelongsProject: be-vita
 * @BelongsPackage: com.vita.controller.market
 * @Author: znk
 * @CreateTime: 2026-07-25
 * @Description: A股股票字典和资金监控管理接口
 * @Version: 1.0
 */
@Validated
@RestController
@RequestMapping("/market/stock")
public class MarketStockController {

    private final IMarketStockService stockService;
    private final IMarketRefreshService refreshService;

    public MarketStockController(IMarketStockService stockService,
                                 IMarketRefreshService refreshService) {
        this.stockService = stockService;
        this.refreshService = refreshService;
    }

    /**
     * 分页查询股票字典。
     *
     * @param searchDto 查询条件
     * @return 股票字典分页
     */
    @GetMapping("/dictionary/page")
    @SaCheckPermission("market:dashboard:view")
    public CommonResult<PageResponse<MarketDataModels.StockDictionaryView>> dictionaryPage(
            @Valid MarketStockSearchDto searchDto) {
        return CommonResult.success(stockService.pageDictionary(searchDto));
    }

    /**
     * 每页查询4只当前启用股票及资金曲线。
     *
     * @param keyword 名称或编号
     * @param pageNum 页码
     * @return 监控股票分页
     */
    @GetMapping("/monitor/page")
    @SaCheckPermission("market:dashboard:view")
    public CommonResult<PageResponse<MarketDataModels.StockMonitorItem>> monitorPage(
            @RequestParam(name = "keyword", required = false) String keyword,
            @RequestParam(name = "pageNum", defaultValue = "1") Integer pageNum) {
        return CommonResult.success(stockService.pageMonitor(keyword, pageNum));
    }

    /**
     * 启用一只正常上市股票。
     *
     * @param request 股票编号
     * @return 操作结果
     */
    @Log(module = "行情股票监控", businessType = "ENABLE", saveResponseData = false)
    @RepeatSubmit
    @PostMapping("/enable")
    @SaCheckPermission("market:stock:manage")
    public CommonResult<Boolean> enable(@RequestBody @Valid MarketStockCodeRequest request) {
        stockService.enable(request.getStockCode());
        return CommonResult.success(true);
    }

    /**
     * 停用股票但保留历史资金点。
     *
     * @param request 股票编号
     * @return 操作结果
     */
    @Log(module = "行情股票监控", businessType = "DISABLE", saveResponseData = false)
    @RepeatSubmit
    @PostMapping("/disable")
    @SaCheckPermission("market:stock:manage")
    public CommonResult<Boolean> disable(@RequestBody @Valid MarketStockCodeRequest request) {
        stockService.disable(request.getStockCode());
        return CommonResult.success(true);
    }

    /**
     * 原子更新全部启用股票排序。
     *
     * @param request 完整股票编号顺序
     * @return 操作结果
     */
    @Log(module = "行情股票监控", businessType = "REORDER", saveResponseData = false)
    @RepeatSubmit
    @PostMapping("/reorder")
    @SaCheckPermission("market:stock:manage")
    public CommonResult<Boolean> reorder(@RequestBody @Valid MarketStockReorderRequest request) {
        stockService.reorder(request.getStockCodes());
        return CommonResult.success(true);
    }

    /**
     * 管理员手动同步沪深京股票字典。
     *
     * @return 同步记录数
     */
    @Log(module = "行情股票字典", businessType = "SYNC", saveResponseData = false)
    @RepeatSubmit
    @PostMapping("/dictionary/sync")
    @SaCheckPermission("market:stock:sync")
    public CommonResult<Integer> syncDictionary() {
        return CommonResult.success(refreshService.syncStockDictionary());
    }
}
