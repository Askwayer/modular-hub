package com.vita.app.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.vita.app.dto.AppOauthAccountCreateDto;
import com.vita.app.dto.AppOauthAccountSearchDto;
import com.vita.app.dto.AppOauthAccountUpdateDto;
import com.vita.app.entity.AppOauthAccount;
import com.vita.app.mapper.AppOauthAccountMapper;
import com.vita.app.service.IAppOauthAccountService;
import com.vita.app.vo.AppOauthAccountDetailVo;
import com.vita.app.vo.AppOauthAccountListVo;
import com.vita.app.vo.AppOauthAccountPageVo;
import com.vita.core.exception.GlobalErrorCode;
import com.vita.core.exception.ServiceException;
import com.vita.core.page.PageResponse;
import com.vita.mybatis.wrapper.LambdaQueryWrapperX;
import jakarta.annotation.Resource;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * @BelongsProject: vita
 * @BelongsPackage: com.vita.app.service.impl
 * @Author znk
 * @CreateTime 2026-08-26 16:58:43
 * @Description: App OAuth账号绑定表 Service 实现
 * @version 1.0
 */
@Service
public class AppOauthAccountServiceImpl extends ServiceImpl<AppOauthAccountMapper, AppOauthAccount> implements IAppOauthAccountService {

    @Resource
    private AppOauthAccountMapper appOauthAccountMapper;

    /**
     * 新增数据并返回主键ID。
     *
     * @param createDto 创建请求对象
     * @return 新增记录ID
     */
    @Override
    public Long create(AppOauthAccountCreateDto createDto) {
        if (createDto == null) {
            throw new ServiceException(GlobalErrorCode.BAD_REQUEST);
        }
        AppOauthAccount data = new AppOauthAccount();
        BeanUtils.copyProperties(createDto, data);
        appOauthAccountMapper.insert(data);
        return data.getId();
    }

    /**
     * 更新指定数据。
     *
     * @param updateDto 更新请求对象
     */
    @Override
    public void update(AppOauthAccountUpdateDto updateDto) {
        if (updateDto == null || updateDto.getId() == null) {
            throw new ServiceException(GlobalErrorCode.BAD_REQUEST);
        }
        getRequiredEntity(updateDto.getId());
        AppOauthAccount data = new AppOauthAccount();
        BeanUtils.copyProperties(updateDto, data);
        appOauthAccountMapper.updateById(data);
    }

    /**
     * 删除指定数据。
     *
     * @param id 主键ID
     */
    @Override
    public void delete(Long id) {
        getRequiredEntity(id);
        appOauthAccountMapper.deleteById(id);
    }

    /**
     * 查询单条数据。
     *
     * @param id 主键ID
     * @return 详情对象
     */
    @Override
    public AppOauthAccountDetailVo get(Long id) {
        AppOauthAccount data = getRequiredEntity(id);
        return BeanUtil.copyProperties(data, AppOauthAccountDetailVo.class);
    }

    /**
     * 列表查询。
     *
     * @param searchDto 查询条件
     * @return 列表VO
     */
    @Override
    public List<AppOauthAccountListVo> list(AppOauthAccountSearchDto searchDto) {
        List<AppOauthAccount> dataList = appOauthAccountMapper.selectList(buildQueryWrapper(searchDto));
        return BeanUtil.copyToList(dataList, AppOauthAccountListVo.class);
    }

    /**
     * 分页查询。
     *
     * @param searchDto 分页查询条件
     * @return 分页VO结果
     */
    @Override
    public PageResponse<AppOauthAccountPageVo> page(AppOauthAccountSearchDto searchDto) {
        if (searchDto == null) {
            searchDto = new AppOauthAccountSearchDto();
        }
        PageResponse<AppOauthAccount> selectPage = appOauthAccountMapper.selectPage(searchDto, buildQueryWrapper(searchDto));
        PageResponse<AppOauthAccountPageVo> pageResponse = new PageResponse<>();
        pageResponse.setList(BeanUtil.copyToList(selectPage.getList(), AppOauthAccountPageVo.class));
        pageResponse.setTotal(selectPage.getTotal());
        return pageResponse;
    }

    /**
     * 构造列表查询条件。
     *
     * @param searchDto 查询条件
     * @return 查询条件构造器
     */
    private LambdaQueryWrapperX<AppOauthAccount> buildQueryWrapper(AppOauthAccountSearchDto searchDto) {
        LambdaQueryWrapperX<AppOauthAccount> queryWrapper = new LambdaQueryWrapperX<>();
        if (searchDto != null && StrUtil.isNotBlank(searchDto.getKeyword())) {
            // TODO 根据业务字段补充关键字查询条件
        }
        queryWrapper.orderByDesc(AppOauthAccount::getId);
        return queryWrapper;
    }

    /**
     * 查询并校验数据存在性。
     *
     * @param id 主键ID
     * @return 实体对象
     */
    private AppOauthAccount getRequiredEntity(Long id) {
        if (id == null) {
            throw new ServiceException(GlobalErrorCode.BAD_REQUEST);
        }
        AppOauthAccount data = appOauthAccountMapper.selectById(id);
        if (data == null) {
            throw new ServiceException(GlobalErrorCode.NOT_FOUND);
        }
        return data;
    }
}
