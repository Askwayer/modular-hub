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
.dto.MarketDailySummaryCreateDto;
.dto.MarketDailySummarySearchDto;
.dto.MarketDailySummaryUpdateDto;
.entity.MarketDailySummary;
.mapper.MarketDailySummaryMapper;
.service.IMarketDailySummaryService;
.vo.MarketDailySummaryDetailVo;
.vo.MarketDailySummaryListVo;
.vo.MarketDailySummaryPageVo;

/**
 * @BelongsProject: vita
 * @BelongsPackage: com.vita.market..service.impl
 * @Author znk
 * @CreateTime 2026-07-25 09:42:04
 * @Description: A股市场日汇总 Service 实现
 * @version 1.0
 */
@Service
public class MarketDailySummaryServiceImpl extends ServiceImpl<MarketDailySummaryMapper, MarketDailySummary> implements IMarketDailySummaryService {

    @Resource
    private MarketDailySummaryMapper marketDailySummaryMapper;

    /**
     * 新增数据并返回主键ID。
     *
     * @param createDto 创建请求对象
     * @return 新增记录ID
     */
    @Override
    public Long create(MarketDailySummaryCreateDto createDto) {
        if (createDto == null) {
            throw new ServiceException(GlobalErrorCode.BAD_REQUEST);
        }
        MarketDailySummary data = new MarketDailySummary();
        BeanUtils.copyProperties(createDto, data);
        marketDailySummaryMapper.insert(data);
        return data.getId();
    }

    /**
     * 更新指定数据。
     *
     * @param updateDto 更新请求对象
     */
    @Override
    public void update(MarketDailySummaryUpdateDto updateDto) {
        if (updateDto == null || updateDto.getId() == null) {
            throw new ServiceException(GlobalErrorCode.BAD_REQUEST);
        }
        getRequiredEntity(updateDto.getId());
        MarketDailySummary data = new MarketDailySummary();
        BeanUtils.copyProperties(updateDto, data);
        marketDailySummaryMapper.updateById(data);
    }

    /**
     * 删除指定数据。
     *
     * @param id 主键ID
     */
    @Override
    public void delete(Long id) {
        getRequiredEntity(id);
        marketDailySummaryMapper.deleteById(id);
    }

    /**
     * 查询单条数据。
     *
     * @param id 主键ID
     * @return 详情对象
     */
    @Override
    public MarketDailySummaryDetailVo get(Long id) {
        MarketDailySummary data = getRequiredEntity(id);
        return BeanUtil.copyProperties(data, MarketDailySummaryDetailVo.class);
    }

    /**
     * 列表查询。
     *
     * @param searchDto 查询条件
     * @return 列表VO
     */
    @Override
    public List<MarketDailySummaryListVo> list(MarketDailySummarySearchDto searchDto) {
        List<MarketDailySummary> dataList = marketDailySummaryMapper.selectList(buildQueryWrapper(searchDto));
        return BeanUtil.copyToList(dataList, MarketDailySummaryListVo.class);
    }

    /**
     * 分页查询。
     *
     * @param searchDto 分页查询条件
     * @return 分页VO结果
     */
    @Override
    public PageResponse<MarketDailySummaryPageVo> page(MarketDailySummarySearchDto searchDto) {
        if (searchDto == null) {
            searchDto = new MarketDailySummarySearchDto();
        }
        PageResponse<MarketDailySummary> selectPage = marketDailySummaryMapper.selectPage(searchDto, buildQueryWrapper(searchDto));
        PageResponse<MarketDailySummaryPageVo> pageResponse = new PageResponse<>();
        pageResponse.setList(BeanUtil.copyToList(selectPage.getList(), MarketDailySummaryPageVo.class));
        pageResponse.setTotal(selectPage.getTotal());
        return pageResponse;
    }

    /**
     * 构造列表查询条件。
     *
     * @param searchDto 查询条件
     * @return 查询条件构造器
     */
    private LambdaQueryWrapperX<MarketDailySummary> buildQueryWrapper(MarketDailySummarySearchDto searchDto) {
        LambdaQueryWrapperX<MarketDailySummary> queryWrapper = new LambdaQueryWrapperX<>();
        if (searchDto != null && StrUtil.isNotBlank(searchDto.getKeyword())) {
            // TODO 根据业务字段补充关键字查询条件
        }
        queryWrapper.orderByDesc(MarketDailySummary::getId);
        return queryWrapper;
    }

    /**
     * 查询并校验数据存在性。
     *
     * @param id 主键ID
     * @return 实体对象
     */
    private MarketDailySummary getRequiredEntity(Long id) {
        if (id == null) {
            throw new ServiceException(GlobalErrorCode.BAD_REQUEST);
        }
        MarketDailySummary data = marketDailySummaryMapper.selectById(id);
        if (data == null) {
            throw new ServiceException(GlobalErrorCode.NOT_FOUND);
        }
        return data;
    }
}
