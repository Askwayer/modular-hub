package com.vita.app.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.vita.app.dto.AppUserCreateDto;
import com.vita.app.dto.AppUserSearchDto;
import com.vita.app.dto.AppUserUpdateDto;
import com.vita.app.entity.AppUser;
import com.vita.app.vo.AppUserDetailVo;
import com.vita.app.vo.AppUserListVo;
import com.vita.app.vo.AppUserPageVo;
import com.vita.core.page.PageResponse;

import java.util.List;

/**
 * @BelongsProject: vita
 * @BelongsPackage: com.vita.app.service
 * @Author znk
 * @CreateTime 2026-08-26 16:58:43
 * @Description: App用户表 Service 接口
 * @version 1.0
 */
public interface IAppUserService extends IService<AppUser> {

    /**
     * 新增数据。
     *
     * @param createDto 创建请求对象
     * @return 新增记录ID
     */
    Long create(AppUserCreateDto createDto);

    /**
     * 更新数据。
     *
     * @param updateDto 更新请求对象
     */
    void update(AppUserUpdateDto updateDto);

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
    AppUserDetailVo get(Long id);

    /**
     * 列表查询。
     *
     * @param searchDto 查询条件
     * @return 列表VO
     */
    List<AppUserListVo> list(AppUserSearchDto searchDto);

    /**
     * 分页查询。
     *
     * @param searchDto 分页查询条件
     * @return 分页VO结果
     */
    PageResponse<AppUserPageVo> page(AppUserSearchDto searchDto);
}
