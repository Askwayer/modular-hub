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
.dto.MarketStockFundPointCreateDto;
.dto.MarketStockFundPointSearchDto;
.dto.MarketStockFundPointUpdateDto;
.entity.MarketStockFundPoint;
.mapper.MarketStockFundPointMapper;
.service.IMarketStockFundPointService;
.vo.MarketStockFundPointDetailVo;
.vo.MarketStockFundPointListVo;
.vo.MarketStockFundPointPageVo;

/**
 * @BelongsProject: vita
 * @BelongsPackage: com.vita.market..service.impl
 * @Author znk
 * @CreateTime 2026-07-25 09:42:04
 * @Description: A股个股资金40秒采样点 Service 实现
 * @version 1.0
 */
@Service
public class MarketStockFundPointServiceImpl extends ServiceImpl<MarketStockFundPointMapper, MarketStockFundPoint> implements IMarketStockFundPointService {

    @Resource
    private MarketStockFundPointMapper marketStockFundPointMapper;

    /**
     * 新增数据并返回主键ID。
     *
     * @param createDto 创建请求对象
     * @return 新增记录ID
     */
    @Override
    public Long create(MarketStockFundPointCreateDto createDto) {
        if (createDto == null) {
            throw new ServiceException(GlobalErrorCode.BAD_REQUEST);
        }
        MarketStockFundPoint data = new MarketStockFundPoint();
        BeanUtils.copyProperties(createDto, data);
        marketStockFundPointMapper.insert(data);
        return data.getId();
    }

    /**
     * 更新指定数据。
     *
     * @param updateDto 更新请求对象
     */
    @Override
    public void update(MarketStockFundPointUpdateDto updateDto) {
        if (updateDto == null || updateDto.getId() == null) {
            throw new ServiceException(GlobalErrorCode.BAD_REQUEST);
        }
        getRequiredEntity(updateDto.getId());
        MarketStockFundPoint data = new MarketStockFundPoint();
        BeanUtils.copyProperties(updateDto, data);
        marketStockFundPointMapper.updateById(data);
    }

    /**
     * 删除指定数据。
     *
     * @param id 主键ID
     */
    @Override
    public void delete(Long id) {
        getRequiredEntity(id);
        marketStockFundPointMapper.deleteById(id);
    }

    /**
     * 查询单条数据。
     *
     * @param id 主键ID
     * @return 详情对象
     */
    @Override
    public MarketStockFundPointDetailVo get(Long id) {
        MarketStockFundPoint data = getRequiredEntity(id);
        return BeanUtil.copyProperties(data, MarketStockFundPointDetailVo.class);
    }

    /**
     * 列表查询。
     *
     * @param searchDto 查询条件
     * @return 列表VO
     */
    @Override
    public List<MarketStockFundPointListVo> list(MarketStockFundPointSearchDto searchDto) {
        List<MarketStockFundPoint> dataList = marketStockFundPointMapper.selectList(buildQueryWrapper(searchDto));
        return BeanUtil.copyToList(dataList, MarketStockFundPointListVo.class);
    }

    /**
     * 分页查询。
     *
     * @param searchDto 分页查询条件
     * @return 分页VO结果
     */
    @Override
    public PageResponse<MarketStockFundPointPageVo> page(MarketStockFundPointSearchDto searchDto) {
        if (searchDto == null) {
            searchDto = new MarketStockFundPointSearchDto();
        }
        PageResponse<MarketStockFundPoint> selectPage = marketStockFundPointMapper.selectPage(searchDto, buildQueryWrapper(searchDto));
        PageResponse<MarketStockFundPointPageVo> pageResponse = new PageResponse<>();
        pageResponse.setList(BeanUtil.copyToList(selectPage.getList(), MarketStockFundPointPageVo.class));
        pageResponse.setTotal(selectPage.getTotal());
        return pageResponse;
    }

    /**
     * 构造列表查询条件。
     *
     * @param searchDto 查询条件
     * @return 查询条件构造器
     */
    private LambdaQueryWrapperX<MarketStockFundPoint> buildQueryWrapper(MarketStockFundPointSearchDto searchDto) {
        LambdaQueryWrapperX<MarketStockFundPoint> queryWrapper = new LambdaQueryWrapperX<>();
        if (searchDto != null && StrUtil.isNotBlank(searchDto.getKeyword())) {
            // TODO 根据业务字段补充关键字查询条件
        }
        queryWrapper.orderByDesc(MarketStockFundPoint::getId);
        return queryWrapper;
    }

    /**
     * 查询并校验数据存在性。
     *
     * @param id 主键ID
     * @return 实体对象
     */
    private MarketStockFundPoint getRequiredEntity(Long id) {
        if (id == null) {
            throw new ServiceException(GlobalErrorCode.BAD_REQUEST);
        }
        MarketStockFundPoint data = marketStockFundPointMapper.selectById(id);
        if (data == null) {
            throw new ServiceException(GlobalErrorCode.NOT_FOUND);
        }
        return data;
    }
}
