package com.vita.controller.system;

import com.vita.core.CommonResult;
import com.vita.core.page.PageResponse;
import com.vita.system.associate.sysRoleDeptScope.dto.SysRoleDeptScopeCreateDto;
import com.vita.system.associate.sysRoleDeptScope.dto.SysRoleDeptScopeSearchDto;
import com.vita.system.associate.sysRoleDeptScope.dto.SysRoleDeptScopeUpdateDto;
import com.vita.system.associate.sysRoleDeptScope.service.ISysRoleDeptScopeService;
import com.vita.system.associate.sysRoleDeptScope.vo.SysRoleDeptScopeDetailVo;
import com.vita.system.associate.sysRoleDeptScope.vo.SysRoleDeptScopeListVo;
import com.vita.system.associate.sysRoleDeptScope.vo.SysRoleDeptScopePageVo;
import jakarta.validation.Valid;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import com.vita.core.controller.BaseController;

/**
 * @BelongsProject: vita
 * @BelongsPackage: com.vita.controller.system
 * @Author znk
 * @CreateTime 2026-03-31 16:41:38
 * @Description: 角色自定义数据范围表 控制器
 * @version 1.0
 */
@Validated
@RestController
@RequestMapping("/system/sysRoleDeptScope")
public class SysRoleDeptScopeController extends BaseController {

    /**
     * 角色自定义数据范围表 服务。
     */
    private final ISysRoleDeptScopeService sysRoleDeptScopeService;

    public SysRoleDeptScopeController(ISysRoleDeptScopeService sysRoleDeptScopeService) {
        this.sysRoleDeptScopeService = sysRoleDeptScopeService;
    }

    /**
     * 新增数据。
     *
     * @param createDto 创建请求对象
     * @return 新增记录ID
     */
    @PostMapping("/add")
    public CommonResult<Long> create(@RequestBody @Valid SysRoleDeptScopeCreateDto createDto) {
        return CommonResult.success(sysRoleDeptScopeService.create(createDto));
    }

    /**
     * 更新数据。
     *
     * @param updateDto 更新请求对象
     * @return 更新结果
     */
    @PostMapping("/update")
    public CommonResult<Boolean> update(@RequestBody @Valid SysRoleDeptScopeUpdateDto updateDto) {
        sysRoleDeptScopeService.update(updateDto);
        return CommonResult.success(true);
    }

    /**
     * 删除数据。
     *
     * @param id 主键ID
     * @return 删除结果
     */
    @DeleteMapping("/delete")
    public CommonResult<Boolean> delete(@RequestParam("id") Long id) {
        sysRoleDeptScopeService.delete(id);
        return CommonResult.success(true);
    }

    /**
     * 查询详情。
     *
     * @param id 主键ID
     * @return 详情对象
     */
    @GetMapping("/getDetail")
    public CommonResult<SysRoleDeptScopeDetailVo> getDetail(@RequestParam("id") Long id) {
        return CommonResult.success(sysRoleDeptScopeService.get(id));
    }

    /**
     * 列表查询。
     *
     * @param searchDto 查询条件
     * @return 列表数据
     */
    @GetMapping("/list")
    public CommonResult<List<SysRoleDeptScopeListVo>> list(@Valid SysRoleDeptScopeSearchDto searchDto) {
        return CommonResult.success(sysRoleDeptScopeService.list(searchDto));
    }

    /**
     * 分页查询。
     *
     * @param searchDto 分页查询条件
     * @return 分页数据
     */
    @GetMapping("/page")
    public CommonResult<PageResponse<SysRoleDeptScopePageVo>> page(@Valid SysRoleDeptScopeSearchDto searchDto) {
        return CommonResult.success(sysRoleDeptScopeService.page(searchDto));
    }
}
