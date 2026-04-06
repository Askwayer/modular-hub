package com.vita.controller.system;

import com.vita.core.CommonResult;
import com.vita.core.page.PageResponse;
import com.vita.system.sysRole.dto.SysRoleAuthUserDto;
import com.vita.system.sysRole.dto.SysRoleAuthUsersDto;
import com.vita.system.sysRole.dto.SysRoleCreateDto;
import com.vita.system.sysRole.dto.SysRoleSearchDto;
import com.vita.system.sysRole.dto.SysRoleUpdateDto;
import com.vita.system.sysRole.entity.SysRole;
import com.vita.system.sysRole.service.ISysRoleService;
import com.vita.system.sysRole.vo.SysRoleDetailVo;
import com.vita.system.sysRole.vo.SysRoleListVo;
import com.vita.system.sysRole.vo.SysRoleOptionVo;
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

    /**
     * 根据用户ID查询角色。
     *
     * @param userId 用户ID
     * @return 角色列表
     */
    @GetMapping("/getByUserId")
    public CommonResult<List<SysRoleListVo>> getByUserId(@RequestParam("userId") Long userId) {
        return CommonResult.success(sysRoleService.getListByUserId(userId));
    }

    /**
     * 查询所有角色。
     *
     * @return 角色列表
     */
    @GetMapping("/getAll")
    public CommonResult<List<SysRoleListVo>> getAll() {
        return CommonResult.success(sysRoleService.getAll());
    }

    /**
     * 根据用户ID获取角色选择框列表。
     *
     * @param userId 用户ID
     * @return 角色选择框列表
     */
    @GetMapping("/getOptionListByUserId")
    public CommonResult<List<SysRoleOptionVo>> getOptionListByUserId(@RequestParam("userId") Long userId) {
        return CommonResult.success(sysRoleService.getOptionListByUserId(userId));
    }

    /**
     * 校验角色名称是否唯一。
     *
     * @param id 角色ID，可为空
     * @param roleName 角色名称
     * @return true 唯一 / false 不唯一
     */
    @GetMapping("/checkRoleNameUnique")
    public CommonResult<Boolean> checkRoleNameUnique(@RequestParam(value = "id", required = false) Long id,
                                                     @RequestParam("roleName") String roleName) {
        SysRole role = new SysRole();
        role.setId(id);
        role.setRoleName(roleName);
        return CommonResult.success(sysRoleService.checkRoleNameUnique(role));
    }

    /**
     * 校验角色编码是否唯一。
     *
     * @param id 角色ID，可为空
     * @param roleCode 角色编码
     * @return true 唯一 / false 不唯一
     */
    @GetMapping("/checkRoleCodeUnique")
    public CommonResult<Boolean> checkRoleCodeUnique(@RequestParam(value = "id", required = false) Long id,
                                                     @RequestParam("roleCode") String roleCode) {
        SysRole role = new SysRole();
        role.setId(id);
        role.setRoleCode(roleCode);
        return CommonResult.success(sysRoleService.checkRoleCodeUnique(role));
    }

    /**
     * 批量删除角色。
     *
     * @param ids 角色ID列表
     * @return 删除结果
     */
    @DeleteMapping("/deleteBatch")
    public CommonResult<Boolean> deleteBatch(@RequestParam("ids") List<Long> ids) {
        sysRoleService.deleteBatch(ids);
        return CommonResult.success(true);
    }

    /**
     * 取消授权单个用户角色。
     *
     * @param authUserDto 请求对象
     * @return 处理结果
     */
    @PostMapping("/cancelAuthUser")
    public CommonResult<Boolean> cancelAuthUser(@RequestBody @Valid SysRoleAuthUserDto authUserDto) {
        sysRoleService.cancelAuthUser(authUserDto.getRoleId(), authUserDto.getUserId());
        return CommonResult.success(true);
    }

    /**
     * 批量取消授权用户角色。
     *
     * @param authUsersDto 请求对象
     * @return 处理结果
     */
    @PostMapping("/cancelAuthUsers")
    public CommonResult<Boolean> cancelAuthUsers(@RequestBody @Valid SysRoleAuthUsersDto authUsersDto) {
        sysRoleService.cancelAuthUsers(authUsersDto.getRoleId(), authUsersDto.getUserIds());
        return CommonResult.success(true);
    }

    /**
     * 批量选择授权用户角色。
     *
     * @param authUsersDto 请求对象
     * @return 处理结果
     */
    @PostMapping("/authUsers")
    public CommonResult<Boolean> authUsers(@RequestBody @Valid SysRoleAuthUsersDto authUsersDto) {
        sysRoleService.authUsers(authUsersDto.getRoleId(), authUsersDto.getUserIds());
        return CommonResult.success(true);
    }
}
