package com.vita.market.

import com.baomidou.mybatisplus.extension.service.IService;
import com.vita.core.page.PageResponse;

import java.util.List;

.service;
.dto.MarketStockCreateDto;
.dto.MarketStockSearchDto;
.dto.MarketStockUpdateDto;
.entity.MarketStock;
.vo.MarketStockDetailVo;
.vo.MarketStockListVo;
.vo.MarketStockPageVo;

/**
 * @BelongsProject: vita
 * @BelongsPackage: com.vita.market..service
 * @Author znk
 * @CreateTime 2026-07-25 09:42:04
 * @Description: A股股票字典 Service 接口
 * @version 1.0
 */
public interface IMarketStockService extends IService<MarketStock> {

    /**
     * 新增数据。
     *
     * @param createDto 创建请求对象
     * @return 新增记录ID
     */
    Long create(MarketStockCreateDto createDto);

    /**
     * 更新数据。
     *
     * @param updateDto 更新请求对象
     */
    void update(MarketStockUpdateDto updateDto);

    /**
     * 删除数据。
     *
     * @param id 主键ID
     */
    void delete(Long id);

    /**
     * 查询单条数据。
     *
     * @param id 主键ID
     * @return 详情对象
     */
    MarketStockDetailVo get(Long id);

    /**
     * 列表查询。
     *
     * @param searchDto 查询条件
     * @return 列表VO
     */
    List<MarketStockListVo> list(MarketStockSearchDto searchDto);

    /**
     * 分页查询。
     *
     * @param searchDto 分页查询条件
     * @return 分页VO结果
     */
    PageResponse<MarketStockPageVo> page(MarketStockSearchDto searchDto);
}
