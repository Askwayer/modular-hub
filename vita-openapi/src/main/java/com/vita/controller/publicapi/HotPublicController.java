package com.vita.controller.publicapi;

import com.vita.core.CommonResult;
import com.vita.core.controller.BaseController;
import com.vita.hot.service.IHotSourceService;
import com.vita.hot.vo.HotSourceSnapshotVo;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @BelongsProject: be-vita
 * @BelongsPackage: com.vita.controller.publicapi
 * @Author: Codex
 * @CreateTime: 2026-05-11
 * @Description: 热榜公开接口控制器
 * @Version: 1.0
 */
@Validated
@RestController
@RequestMapping("/public/hot")
public class HotPublicController extends BaseController {

    private final IHotSourceService hotSourceService;

    public HotPublicController(IHotSourceService hotSourceService) {
        this.hotSourceService = hotSourceService;
    }

    /**
     * 查询单个热榜源缓存详情。
     *
     * @param sourceCode 源编码
     * @return 缓存快照
     */
    @GetMapping("/{sourceCode:^(?!sources$).+}")
    public CommonResult<HotSourceSnapshotVo> getSource(@PathVariable("sourceCode") String sourceCode) {
        return CommonResult.success(hotSourceService.getCachedSource(sourceCode));
    }
}
