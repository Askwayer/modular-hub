package com.vita.system.sysPermission.service;

import com.vita.core.page.PageResponse;
import com.vita.system.sysPermission.dto.SysPermissionCreateDto;
import com.vita.system.sysPermission.dto.SysPermissionSearchDto;
import com.vita.system.sysPermission.dto.SysPermissionUpdateDto;
import com.vita.system.sysPermission.entity.SysPermission;
import com.baomidou.mybatisplus.extension.service.IService;
import com.vita.system.sysPermission.vo.SysPermissionDetailVo;
import com.vita.system.sysPermission.vo.SysPermissionListVo;
import com.vita.system.sysPermission.vo.SysPermissionPageVo;

import java.util.List;

/**
 * @BelongsProject: vita
 * @BelongsPackage: com.vita.system.sysPermission.service
 * @Author znk
 * @CreateTime 2026-03-31 16:27:59
 * @Description: 权限表 Service 接口
 * @version 1.0
 */
public interface ISysPermissionService extends IService<SysPermission> {

    /**
     * 新增数据。
     *
     * @param createDto 创建请求对象
     * @return 新增记录ID
     */
    Long create(SysPermissionCreateDto createDto);

    /**
     * 更新数据。
     *
     * @param updateDto 更新请求对象
     */
    void update(SysPermissionUpdateDto updateDto);

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
    SysPermissionDetailVo get(Long id);

    /**
     * 列表查询。
     *
     * @param searchDto 查询条件
     * @return 列表VO
     */
    List<SysPermissionListVo> list(SysPermissionSearchDto searchDto);

    /**
     * 分页查询。
     *
     * @param searchDto 分页查询条件
     * @return 分页VO结果
     */
    PageResponse<SysPermissionPageVo> page(SysPermissionSearchDto searchDto);
}
