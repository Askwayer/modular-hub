package com.vita.controller.system;

import com.vita.core.CommonResult;
import com.vita.core.page.PageResponse;
import com.vita.system.sysRole.dto.SysRoleCreateDto;
import com.vita.system.sysRole.dto.SysRoleSearchDto;
import com.vita.system.sysRole.dto.SysRoleUpdateDto;
import com.vita.system.sysRole.service.ISysRoleService;
import com.vita.system.sysRole.vo.SysRoleDetailVo;
import com.vita.system.sysRole.vo.SysRoleListVo;
import com.vita.system.sysRole.vo.SysRolePageVo;
import jakarta.validation.Valid;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import com.vita.core.controller.BaseController;

/**
 * @BelongsProject: vita
 * @BelongsPackage: com.vita.controller.system
 * @Author znk
 * @CreateTime 2026-03-31 14:45:57
 * @Description: 角色表 控制器
 * @version 1.0
 */
@Validated
@RestController
@RequestMapping("/system/sysRole")
public class SysRoleController extends BaseController {

    /**
     * 角色表 服务。
     */
    private final ISysRoleService sysRoleService;

    public SysRoleController(ISysRoleService sysRoleService) {
        this.sysRoleService = sysRoleService;
    }

    /**
     * 新增数据。
     *
     * @param createDto 创建请求对象
     * @return 新增记录ID
     */
    @PostMapping("/add")
    public CommonResult<Long> create(@RequestBody @Valid SysRoleCreateDto createDto) {
        return CommonResult.success(sysRoleService.create(createDto));
    }

    /**
     * 更新数据。
     *
     * @param updateDto 更新请求对象
     * @return 更新结果
     */
    @PostMapping("/update")
    public CommonResult<Boolean> update(@RequestBody @Valid SysRoleUpdateDto updateDto) {
        sysRoleService.update(updateDto);
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
        sysRoleService.delete(id);
        return CommonResult.success(true);
    }

    /**
     * 查询详情。
     *
     * @param id 主键ID
     * @return 详情对象
     */
    @GetMapping("/getDetail")
    public CommonResult<SysRoleDetailVo> getDetail(@RequestParam("id") Long id) {
        return CommonResult.success(sysRoleService.get(id));
    }

    /**
     * 列表查询。
     *
     * @param searchDto 查询条件
     * @return 列表数据
     */
    @GetMapping("/list")
    public CommonResult<List<SysRoleListVo>> list(@Valid SysRoleSearchDto searchDto) {
        return CommonResult.success(sysRoleService.list(searchDto));
    }

    /**
     * 分页查询。
     *
     * @param searchDto 分页查询条件
     * @return 分页数据
     */
    @GetMapping("/page")
    public CommonResult<PageResponse<SysRolePageVo>> page(@Valid SysRoleSearchDto searchDto) {
        return CommonResult.success(sysRoleService.page(searchDto));
    }
}
