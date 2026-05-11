package com.vita.controller.hot;

import com.vita.core.CommonResult;
import com.vita.core.controller.BaseController;
import com.vita.hot.service.IHotSourceService;
import com.vita.hot.vo.HotSourceSnapshotVo;
import com.vita.hot.vo.HotSourceStatusVo;
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
@RequestMapping("/hot/cache")
public class HotCacheController extends BaseController {

    private final IHotSourceService hotSourceService;

    public HotCacheController(IHotSourceService hotSourceService) {
        this.hotSourceService = hotSourceService;
    }

    /**
     * 手动刷新单个热榜源缓存。
     *
     * @param sourceCode 源编码
     * @return 刷新后的快照
     */
    @PostMapping("/refresh")
    public CommonResult<HotSourceSnapshotVo> refresh(@RequestParam("sourceCode") String sourceCode) {
        return CommonResult.success(hotSourceService.refreshSource(sourceCode));
    }

    /**
     * 手动刷新全部启用热榜缓存。
     *
     * @return 刷新后的状态列表
     */
    @PostMapping("/refreshAll")
    public CommonResult<List<HotSourceStatusVo>> refreshAll() {
        return CommonResult.success(hotSourceService.refreshAllEnabledSources());
    }

    /**
     * 查询热榜缓存状态。
     *
     * @return 状态列表
     */
    @GetMapping("/status")
    public CommonResult<List<HotSourceStatusVo>> status() {
        return CommonResult.success(hotSourceService.listCacheStatus());
    }
}
