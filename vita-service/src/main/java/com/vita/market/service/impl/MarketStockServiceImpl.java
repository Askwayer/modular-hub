package com.vita.market.

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.vita.core.exception.GlobalErrorCode;
import com.vita.core.exception.ServiceException;
import com.vita.core.page.PageResponse;
import com.vita.mybatis.wrapper.LambdaQueryWrapperX;
import jakarta.annotation.Resource;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.util.List;

.service.impl;
.dto.MarketStockCreateDto;
.dto.MarketStockSearchDto;
.dto.MarketStockUpdateDto;
.entity.MarketStock;
.mapper.MarketStockMapper;
.service.IMarketStockService;
.vo.MarketStockDetailVo;
.vo.MarketStockListVo;
.vo.MarketStockPageVo;

/**
 * @BelongsProject: vita
 * @BelongsPackage: com.vita.market..service.impl
 * @Author znk
 * @CreateTime 2026-07-25 09:42:04
 * @Description: A股股票字典 Service 实现
 * @version 1.0
 */
@Service
public class MarketStockServiceImpl extends ServiceImpl<MarketStockMapper, MarketStock> implements IMarketStockService {

    @Resource
    private MarketStockMapper marketStockMapper;

    /**
     * 新增数据并返回主键ID。
     *
     * @param createDto 创建请求对象
     * @return 新增记录ID
     */
    @Override
    public Long create(MarketStockCreateDto createDto) {
        if (createDto == null) {
            throw new ServiceException(GlobalErrorCode.BAD_REQUEST);
        }
        MarketStock data = new MarketStock();
        BeanUtils.copyProperties(createDto, data);
        marketStockMapper.insert(data);
        return data.getId();
    }

    /**
     * 更新指定数据。
     *
     * @param updateDto 更新请求对象
     */
    @Override
    public void update(MarketStockUpdateDto updateDto) {
        if (updateDto == null || updateDto.getId() == null) {
            throw new ServiceException(GlobalErrorCode.BAD_REQUEST);
        }
        getRequiredEntity(updateDto.getId());
        MarketStock data = new MarketStock();
        BeanUtils.copyProperties(updateDto, data);
        marketStockMapper.updateById(data);
    }

    /**
     * 删除指定数据。
     *
     * @param id 主键ID
     */
    @Override
    public void delete(Long id) {
        getRequiredEntity(id);
        marketStockMapper.deleteById(id);
    }

    /**
     * 查询单条数据。
     *
     * @param id 主键ID
     * @return 详情对象
     */
    @Override
    public MarketStockDetailVo get(Long id) {
        MarketStock data = getRequiredEntity(id);
        return BeanUtil.copyProperties(data, MarketStockDetailVo.class);
    }

    /**
     * 列表查询。
     *
     * @param searchDto 查询条件
     * @return 列表VO
     */
    @Override
    public List<MarketStockListVo> list(MarketStockSearchDto searchDto) {
        List<MarketStock> dataList = marketStockMapper.selectList(buildQueryWrapper(searchDto));
        return BeanUtil.copyToList(dataList, MarketStockListVo.class);
    }

    /**
     * 分页查询。
     *
     * @param searchDto 分页查询条件
     * @return 分页VO结果
     */
    @Override
    public PageResponse<MarketStockPageVo> page(MarketStockSearchDto searchDto) {
        if (searchDto == null) {
            searchDto = new MarketStockSearchDto();
        }
        PageResponse<MarketStock> selectPage = marketStockMapper.selectPage(searchDto, buildQueryWrapper(searchDto));
        PageResponse<MarketStockPageVo> pageResponse = new PageResponse<>();
        pageResponse.setList(BeanUtil.copyToList(selectPage.getList(), MarketStockPageVo.class));
        pageResponse.setTotal(selectPage.getTotal());
        return pageResponse;
    }

    /**
     * 构造列表查询条件。
     *
     * @param searchDto 查询条件
     * @return 查询条件构造器
     */
    private LambdaQueryWrapperX<MarketStock> buildQueryWrapper(MarketStockSearchDto searchDto) {
        LambdaQueryWrapperX<MarketStock> queryWrapper = new LambdaQueryWrapperX<>();
        if (searchDto != null && StrUtil.isNotBlank(searchDto.getKeyword())) {
            // TODO 根据业务字段补充关键字查询条件
        }
        queryWrapper.orderByDesc(MarketStock::getId);
        return queryWrapper;
    }

    /**
     * 查询并校验数据存在性。
     *
     * @param id 主键ID
     * @return 实体对象
     */
    private MarketStock getRequiredEntity(Long id) {
        if (id == null) {
            throw new ServiceException(GlobalErrorCode.BAD_REQUEST);
        }
        MarketStock data = marketStockMapper.selectById(id);
        if (data == null) {
            throw new ServiceException(GlobalErrorCode.NOT_FOUND);
        }
        return data;
    }
}
