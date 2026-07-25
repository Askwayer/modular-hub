package com.vita.controller.market;

import com.vita.core.CommonResult;
import com.vita.core.controller.BaseController;
import com.vita.core.page.PageResponse;
import com.vita.market.
import jakarta.validation.Valid;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

.dto.MarketStockFundPointCreateDto;
.dto.MarketStockFundPointSearchDto;
.dto.MarketStockFundPointUpdateDto;
.service.IMarketStockFundPointService;
.vo.MarketStockFundPointDetailVo;
.vo.MarketStockFundPointListVo;
.vo.MarketStockFundPointPageVo;

/**
 * @BelongsProject: vita
 * @BelongsPackage: com.vita.controller.market
 * @Author znk
 * @CreateTime 2026-07-25 09:42:04
 * @Description: A股个股资金40秒采样点 控制器
 * @version 1.0
 */
@Validated
@RestController
@RequestMapping("/market/marketStockFundPoint")
public class MarketStockFundPointController extends BaseController {

    /**
     * A股个股资金40秒采样点 服务。
     */
    private final IMarketStockFundPointService marketStockFundPointService;

    public MarketStockFundPointController(IMarketStockFundPointService marketStockFundPointService) {
        this.marketStockFundPointService = marketStockFundPointService;
    }

    /**
     * 新增数据。
     *
     * @param createDto 创建请求对象
     * @return 新增记录ID
     */
    @PostMapping("/add")
    public CommonResult<Long> create(@RequestBody @Valid MarketStockFundPointCreateDto createDto) {
        return CommonResult.success(marketStockFundPointService.create(createDto));
    }

    /**
     * 更新数据。
     *
     * @param updateDto 更新请求对象
     * @return 更新结果
     */
    @PostMapping("/update")
    public CommonResult<Boolean> update(@RequestBody @Valid MarketStockFundPointUpdateDto updateDto) {
        marketStockFundPointService.update(updateDto);
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
        marketStockFundPointService.delete(id);
        return CommonResult.success(true);
    }

    /**
     * 查询详情。
     *
     * @param id 主键ID
     * @return 详情对象
     */
    @GetMapping("/getDetail")
    public CommonResult<MarketStockFundPointDetailVo> getDetail(@RequestParam("id") Long id) {
        return CommonResult.success(marketStockFundPointService.get(id));
    }

    /**
     * 列表查询。
     *
     * @param searchDto 查询条件
     * @return 列表数据
     */
    @GetMapping("/list")
    public CommonResult<List<MarketStockFundPointListVo>> list(@Valid MarketStockFundPointSearchDto searchDto) {
        return CommonResult.success(marketStockFundPointService.list(searchDto));
    }

    /**
     * 分页查询。
     *
     * @param searchDto 分页查询条件
     * @return 分页数据
     */
    @GetMapping("/page")
    public CommonResult<PageResponse<MarketStockFundPointPageVo>> page(@Valid MarketStockFundPointSearchDto searchDto) {
        return CommonResult.success(marketStockFundPointService.page(searchDto));
    }
}
