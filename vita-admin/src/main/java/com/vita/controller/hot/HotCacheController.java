package com.vita.controller.hot;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.vita.annotation.RepeatSubmit;
import com.vita.core.CommonResult;
import com.vita.core.controller.BaseController;
import com.vita.hot.service.IHotSourceService;
import com.vita.hot.vo.HotEngineHealthVo;
import com.vita.hot.vo.HotSourceSnapshotVo;
import com.vita.hot.vo.HotSourceStatusVo;
import jakarta.validation.constraints.NotBlank;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * @BelongsProject: be-vita
 * @BelongsPackage: com.vita.controller.hot
 * @Author: Codex
 * @CreateTime: 2026-05-11
 * @Description: 热榜缓存后台控制器
 * @Version: 1.0
 */
@Validated
@RestController
@RequestMapping("/system/hot/cache")
public class HotCacheController extends BaseController {

    private final IHotSourceService hotSourceService;

    /**
     * 构造函数注入热榜服务依赖
     *
     * @param hotSourceService 热榜源服务接口实现
     */
    public HotCacheController(IHotSourceService hotSourceService) {
        this.hotSourceService = hotSourceService;
    }


    /**
     * 检查热榜引擎服务的健康状态
     * 需要 system:hot-cache:view 权限
     *
     * @return 热榜引擎健康状态信息
     */
    @GetMapping("/health")
    @SaCheckPermission("system:hot-cache:view")
    public CommonResult<HotEngineHealthVo> health() {
        return CommonResult.success(hotSourceService.health());
    }

    /**
     * 刷新热榜源注册表信息
     * 从热榜引擎获取所有可用的热榜源列表并更新到Redis缓存中
     * 需要 system:hot-cache:refresh 权限
     *
     * @return 成功刷新的热榜源数量
     */
    @RepeatSubmit
    @GetMapping("/registry/sources")
    @SaCheckPermission("system:hot-cache:refresh")
    public CommonResult<Long> refreshRegistry() {
        return CommonResult.success(hotSourceService.refreshSourceRegistry());
    }


    /**
     * 手动刷新指定热榜源的缓存数据
     * 需要 system:hot-cache:refresh 权限
     *
     * @param sourceCode 热榜源代码标识，例如 "weibo", "zhihu" 等
     * @return 刷新后的热榜快照数据
     */
    @RepeatSubmit
    @PostMapping("/refresh")
    @SaCheckPermission("system:hot-cache:refresh")
    public CommonResult<HotSourceSnapshotVo> refresh(@RequestParam("sourceCode") @NotBlank(message = "sourceCode不能为空") String sourceCode) {
        return CommonResult.success(hotSourceService.refreshSource(sourceCode));
    }

    /**
     * 批量刷新所有已启用的热榜源缓存
     * 需要 system:hot-cache:refresh 权限
     *
     * @return 所有热榜源的刷新状态列表
     */
    @RepeatSubmit
    @PostMapping("/refreshAll")
    @SaCheckPermission("system:hot-cache:refresh")
    public CommonResult<List<HotSourceStatusVo>> refreshAll() {
        return CommonResult.success(hotSourceService.refreshAllEnabledSources());
    }

    /**
     * 查询所有热榜源的缓存状态信息
     * 包括缓存是否就绪、最后更新时间、数据条数等状态信息
     * 需要 system:hot-cache:view 权限
     *
     * @return 所有热榜源的缓存状态列表
     */
    @GetMapping("/status")
    @SaCheckPermission("system:hot-cache:view")
    public CommonResult<List<HotSourceStatusVo>> status() {
        return CommonResult.success(hotSourceService.listCacheStatus());
    }
}
