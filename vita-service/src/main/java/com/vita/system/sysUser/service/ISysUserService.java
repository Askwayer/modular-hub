package com.vita.system.sysUser.service;

import com.vita.core.page.PageResponse;
import com.vita.system.sysUser.dto.SysUserCreateDto;
import com.vita.system.sysUser.dto.SysUserSearchDto;
import com.vita.system.sysUser.dto.SysUserUpdateDto;
import com.vita.system.sysUser.entity.SysUser;
import com.baomidou.mybatisplus.extension.service.IService;
import com.vita.system.sysUser.vo.SysUserDetailVo;
import com.vita.system.sysUser.vo.SysUserListVo;
import com.vita.system.sysUser.vo.SysUserPageVo;

import java.util.List;

/**
 * @BelongsProject: vita
 * @BelongsPackage: com.vita.system.sysUser.service
 * @Author znk
 * @CreateTime 2026-03-06 21:44:32
 * @Description: 用户信息表 Service 接口
 * @version 1.0
 */
public interface ISysUserService extends IService<SysUser> {

    /**
     * 新增数据。
     *
     * @param createDto 创建请求对象
     * @return 新增记录ID
     */
    Long create(SysUserCreateDto createDto);

    /**
     * 更新数据。
     *
     * @param updateDto 更新请求对象
     */
    void update(SysUserUpdateDto updateDto);

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
    SysUserDetailVo get(Long id);

    /**
     * 列表查询。
     *
     * @param searchDto 查询条件
     * @return 列表VO
     */
    List<SysUserListVo> list(SysUserSearchDto searchDto);

    /**
     * 分页查询。
     *
     * @param searchDto 分页查询条件
     * @return 分页VO结果
     */
    PageResponse<SysUserPageVo> page(SysUserSearchDto searchDto);
}
