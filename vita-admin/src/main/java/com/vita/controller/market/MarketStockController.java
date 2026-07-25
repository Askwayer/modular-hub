package com.vita.controller.market;

import com.vita.core.CommonResult;
import com.vita.core.controller.BaseController;
import com.vita.core.page.PageResponse;
import com.vita.market.
import jakarta.validation.Valid;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

.dto.MarketStockCreateDto;
.dto.MarketStockSearchDto;
.dto.MarketStockUpdateDto;
.service.IMarketStockService;
.vo.MarketStockDetailVo;
.vo.MarketStockListVo;
.vo.MarketStockPageVo;

/**
 * @BelongsProject: vita
 * @BelongsPackage: com.vita.controller.market
 * @Author znk
 * @CreateTime 2026-07-25 09:42:04
 * @Description: A股股票字典 控制器
 * @version 1.0
 */
@Validated
@RestController
@RequestMapping("/market/marketStock")
public class MarketStockController extends BaseController {

    /**
     * A股股票字典 服务。
     */
    private final IMarketStockService marketStockService;

    public MarketStockController(IMarketStockService marketStockService) {
        this.marketStockService = marketStockService;
    }

    /**
     * 新增数据。
     *
     * @param createDto 创建请求对象
     * @return 新增记录ID
     */
    @PostMapping("/add")
    public CommonResult<Long> create(@RequestBody @Valid MarketStockCreateDto createDto) {
        return CommonResult.success(marketStockService.create(createDto));
    }

    /**
     * 更新数据。
     *
     * @param updateDto 更新请求对象
     * @return 更新结果
     */
    @PostMapping("/update")
    public CommonResult<Boolean> update(@RequestBody @Valid MarketStockUpdateDto updateDto) {
        marketStockService.update(updateDto);
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
        marketStockService.delete(id);
        return CommonResult.success(true);
    }

    /**
     * 查询详情。
     *
     * @param id 主键ID
     * @return 详情对象
     */
    @GetMapping("/getDetail")
    public CommonResult<MarketStockDetailVo> getDetail(@RequestParam("id") Long id) {
        return CommonResult.success(marketStockService.get(id));
    }

    /**
     * 列表查询。
     *
     * @param searchDto 查询条件
     * @return 列表数据
     */
    @GetMapping("/list")
    public CommonResult<List<MarketStockListVo>> list(@Valid MarketStockSearchDto searchDto) {
        return CommonResult.success(marketStockService.list(searchDto));
    }

    /**
     * 分页查询。
     *
     * @param searchDto 分页查询条件
     * @return 分页数据
     */
    @GetMapping("/page")
    public CommonResult<PageResponse<MarketStockPageVo>> page(@Valid MarketStockSearchDto searchDto) {
        return CommonResult.success(marketStockService.page(searchDto));
    }
}
