package com.vita.controller.market;

import com.vita.core.CommonResult;
import com.vita.core.controller.BaseController;
import com.vita.core.page.PageResponse;
import com.vita.market.
import jakarta.validation.Valid;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

.dto.MarketDailySummaryCreateDto;
.dto.MarketDailySummarySearchDto;
.dto.MarketDailySummaryUpdateDto;
.service.IMarketDailySummaryService;
.vo.MarketDailySummaryDetailVo;
.vo.MarketDailySummaryListVo;
.vo.MarketDailySummaryPageVo;

/**
 * @BelongsProject: vita
 * @BelongsPackage: com.vita.controller.market
 * @Author znk
 * @CreateTime 2026-07-25 09:42:04
 * @Description: A股市场日汇总 控制器
 * @version 1.0
 */
@Validated
@RestController
@RequestMapping("/market/marketDailySummary")
public class MarketDailySummaryController extends BaseController {

    /**
     * A股市场日汇总 服务。
     */
    private final IMarketDailySummaryService marketDailySummaryService;

    public MarketDailySummaryController(IMarketDailySummaryService marketDailySummaryService) {
        this.marketDailySummaryService = marketDailySummaryService;
    }

    /**
     * 新增数据。
     *
     * @param createDto 创建请求对象
     * @return 新增记录ID
     */
    @PostMapping("/add")
    public CommonResult<Long> create(@RequestBody @Valid MarketDailySummaryCreateDto createDto) {
        return CommonResult.success(marketDailySummaryService.create(createDto));
    }

    /**
     * 更新数据。
     *
     * @param updateDto 更新请求对象
     * @return 更新结果
     */
    @PostMapping("/update")
    public CommonResult<Boolean> update(@RequestBody @Valid MarketDailySummaryUpdateDto updateDto) {
        marketDailySummaryService.update(updateDto);
        return CommonResult.success(true);
    }

    /**
     * 删除数据。
     *
     * @param id 主键ID
     * @return 删除结果
     */
    @GetMapping("/delete")
    public CommonResult<Boolean> delete(@RequestParam("id") Long id) {
        marketDailySummaryService.delete(id);
        return CommonResult.success(true);
    }

    /**
     * 查询详情。
     *
     * @param id 主键ID
     * @return 详情对象
     */
    @GetMapping("/getDetail")
    public CommonResult<MarketDailySummaryDetailVo> getDetail(@RequestParam("id") Long id) {
        return CommonResult.success(marketDailySummaryService.get(id));
    }

    /**
     * 列表查询。
     *
     * @param searchDto 查询条件
     * @return 列表数据
     */
    @GetMapping("/list")
    public CommonResult<List<MarketDailySummaryListVo>> list(@Valid MarketDailySummarySearchDto searchDto) {
        return CommonResult.success(marketDailySummaryService.list(searchDto));
    }

    /**
     * 分页查询。
     *
     * @param searchDto 分页查询条件
     * @return 分页数据
     */
    @GetMapping("/page")
    public CommonResult<PageResponse<MarketDailySummaryPageVo>> page(@Valid MarketDailySummarySearchDto searchDto) {
        return CommonResult.success(marketDailySummaryService.page(searchDto));
    }
}
