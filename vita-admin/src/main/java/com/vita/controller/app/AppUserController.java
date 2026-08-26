package com.vita.controller.app;

import com.vita.app.dto.AppUserCreateDto;
import com.vita.app.dto.AppUserSearchDto;
import com.vita.app.dto.AppUserUpdateDto;
import com.vita.app.service.IAppUserService;
import com.vita.app.vo.AppUserDetailVo;
import com.vita.app.vo.AppUserListVo;
import com.vita.app.vo.AppUserPageVo;
import com.vita.core.CommonResult;
import com.vita.core.controller.BaseController;
import com.vita.core.page.PageResponse;
import jakarta.validation.Valid;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * @BelongsProject: vita
 * @BelongsPackage: com.vita.controller.app
 * @Author znk
 * @CreateTime 2026-08-26 16:58:43
 * @Description: App用户表 控制器
 * @version 1.0
 */
@Validated
@RestController
@RequestMapping("/app/appUser")
public class AppUserController extends BaseController {

    /**
     * App用户表 服务。
     */
    private final IAppUserService appUserService;

    public AppUserController(IAppUserService appUserService) {
        this.appUserService = appUserService;
    }

    /**
     * 新增数据。
     *
     * @param createDto 创建请求对象
     * @return 新增记录ID
     */
    @PostMapping("/add")
    public CommonResult<Long> create(@RequestBody @Valid AppUserCreateDto createDto) {
        return CommonResult.success(appUserService.create(createDto));
    }

    /**
     * 更新数据。
     *
     * @param updateDto 更新请求对象
     * @return 更新结果
     */
    @PostMapping("/update")
    public CommonResult<Boolean> update(@RequestBody @Valid AppUserUpdateDto updateDto) {
        appUserService.update(updateDto);
        return CommonResult.success(true);
    }

    /**
     * 删除数据。
     *
     * @param id 主键ID
     * @return 删除结果
     */
    @GetMapping("/delete")
    public CommonResult<Boolean> delete(@RequestParam("id") Long id) {
        appUserService.delete(id);
        return CommonResult.success(true);
    }

    /**
     * 查询详情。
     *
     * @param id 主键ID
     * @return 详情对象
     */
    @GetMapping("/getDetail")
    public CommonResult<AppUserDetailVo> getDetail(@RequestParam("id") Long id) {
        return CommonResult.success(appUserService.get(id));
    }

    /**
     * 列表查询。
     *
     * @param searchDto 查询条件
     * @return 列表数据
     */
    @GetMapping("/list")
    public CommonResult<List<AppUserListVo>> list(@Valid AppUserSearchDto searchDto) {
        return CommonResult.success(appUserService.list(searchDto));
    }

    /**
     * 分页查询。
     *
     * @param searchDto 分页查询条件
     * @return 分页数据
     */
    @GetMapping("/page")
    public CommonResult<PageResponse<AppUserPageVo>> page(@Valid AppUserSearchDto searchDto) {
        return CommonResult.success(appUserService.page(searchDto));
    }
}
