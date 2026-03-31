package com.vita.system.sysRole.service;

import com.vita.core.page.PageResponse;
import com.vita.system.sysRole.dto.SysRoleCreateDto;
import com.vita.system.sysRole.dto.SysRoleSearchDto;
import com.vita.system.sysRole.dto.SysRoleUpdateDto;
import com.vita.system.sysRole.entity.SysRole;
import com.baomidou.mybatisplus.extension.service.IService;
import com.vita.system.sysRole.vo.SysRoleDetailVo;
import com.vita.system.sysRole.vo.SysRoleListVo;
import com.vita.system.sysRole.vo.SysRolePageVo;

import java.util.List;

/**
 * @BelongsProject: vita
 * @BelongsPackage: com.vita.system.sysRole.service
 * @Author znk
 * @CreateTime 2026-03-31 14:45:57
 * @Description: 角色表 Service 接口
 * @version 1.0
 */
public interface ISysRoleService extends IService<SysRole> {

    /**
     * 新增数据。
     *
     * @param createDto 创建请求对象
     * @return 新增记录ID
     */
    Long create(SysRoleCreateDto createDto);

    /**
     * 更新数据。
     *
     * @param updateDto 更新请求对象
     */
    void update(SysRoleUpdateDto updateDto);

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
    SysRoleDetailVo get(Long id);

    /**
     * 列表查询。
     *
     * @param searchDto 查询条件
     * @return 列表VO
     */
    List<SysRoleListVo> list(SysRoleSearchDto searchDto);

    /**
     * 分页查询。
     *
     * @param searchDto 分页查询条件
     * @return 分页VO结果
     */
    PageResponse<SysRolePageVo> page(SysRoleSearchDto searchDto);
}
