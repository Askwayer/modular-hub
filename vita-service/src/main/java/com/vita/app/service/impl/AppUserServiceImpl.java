package com.vita.app.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.vita.app.dto.AppUserCreateDto;
import com.vita.app.dto.AppUserSearchDto;
import com.vita.app.dto.AppUserUpdateDto;
import com.vita.app.entity.AppUser;
import com.vita.app.mapper.AppUserMapper;
import com.vita.app.service.IAppUserService;
import com.vita.app.vo.AppUserDetailVo;
import com.vita.app.vo.AppUserListVo;
import com.vita.app.vo.AppUserPageVo;
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
 * @Description: App用户表 Service 实现
 * @version 1.0
 */
@Service
public class AppUserServiceImpl extends ServiceImpl<AppUserMapper, AppUser> implements IAppUserService {

    @Resource
    private AppUserMapper appUserMapper;

    /**
     * 新增数据并返回主键ID。
     *
     * @param createDto 创建请求对象
     * @return 新增记录ID
     */
    @Override
    public Long create(AppUserCreateDto createDto) {
        if (createDto == null) {
            throw new ServiceException(GlobalErrorCode.BAD_REQUEST);
        }
        AppUser data = new AppUser();
        BeanUtils.copyProperties(createDto, data);
        appUserMapper.insert(data);
        return data.getId();
    }

    /**
     * 更新指定数据。
     *
     * @param updateDto 更新请求对象
     */
    @Override
    public void update(AppUserUpdateDto updateDto) {
        if (updateDto == null || updateDto.getId() == null) {
            throw new ServiceException(GlobalErrorCode.BAD_REQUEST);
        }
        getRequiredEntity(updateDto.getId());
        AppUser data = new AppUser();
        BeanUtils.copyProperties(updateDto, data);
        appUserMapper.updateById(data);
    }

    /**
     * 删除指定数据。
     *
     * @param id 主键ID
     */
    @Override
    public void delete(Long id) {
        getRequiredEntity(id);
        appUserMapper.deleteById(id);
    }

    /**
     * 查询单条数据。
     *
     * @param id 主键ID
     * @return 详情对象
     */
    @Override
    public AppUserDetailVo get(Long id) {
        AppUser data = getRequiredEntity(id);
        return BeanUtil.copyProperties(data, AppUserDetailVo.class);
    }

    /**
     * 列表查询。
     *
     * @param searchDto 查询条件
     * @return 列表VO
     */
    @Override
    public List<AppUserListVo> list(AppUserSearchDto searchDto) {
        List<AppUser> dataList = appUserMapper.selectList(buildQueryWrapper(searchDto));
        return BeanUtil.copyToList(dataList, AppUserListVo.class);
    }

    /**
     * 分页查询。
     *
     * @param searchDto 分页查询条件
     * @return 分页VO结果
     */
    @Override
    public PageResponse<AppUserPageVo> page(AppUserSearchDto searchDto) {
        if (searchDto == null) {
            searchDto = new AppUserSearchDto();
        }
        PageResponse<AppUser> selectPage = appUserMapper.selectPage(searchDto, buildQueryWrapper(searchDto));
        PageResponse<AppUserPageVo> pageResponse = new PageResponse<>();
        pageResponse.setList(BeanUtil.copyToList(selectPage.getList(), AppUserPageVo.class));
        pageResponse.setTotal(selectPage.getTotal());
        return pageResponse;
    }

    /**
     * 构造列表查询条件。
     *
     * @param searchDto 查询条件
     * @return 查询条件构造器
     */
    private LambdaQueryWrapperX<AppUser> buildQueryWrapper(AppUserSearchDto searchDto) {
        LambdaQueryWrapperX<AppUser> queryWrapper = new LambdaQueryWrapperX<>();
        if (searchDto != null && StrUtil.isNotBlank(searchDto.getKeyword())) {
            // TODO 根据业务字段补充关键字查询条件
        }
        queryWrapper.orderByDesc(AppUser::getId);
        return queryWrapper;
    }

    /**
     * 查询并校验数据存在性。
     *
     * @param id 主键ID
     * @return 实体对象
     */
    private AppUser getRequiredEntity(Long id) {
        if (id == null) {
            throw new ServiceException(GlobalErrorCode.BAD_REQUEST);
        }
        AppUser data = appUserMapper.selectById(id);
        if (data == null) {
            throw new ServiceException(GlobalErrorCode.NOT_FOUND);
        }
        return data;
    }
}
