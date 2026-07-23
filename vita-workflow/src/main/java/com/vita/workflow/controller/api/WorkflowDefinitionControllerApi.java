package com.vita.workflow.controller.api;

import com.vita.core.CommonResult;
import com.vita.core.page.PageResponse;
import com.vita.workflow.runtime.dto.WorkflowDefinitionSearchDto;
import com.vita.workflow.runtime.vo.WorkflowDefinitionVo;
import jakarta.validation.Valid;
import org.dromara.warm.flow.core.dto.DefJson;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

/**
 * @BelongsProject: be-vita
 * @BelongsPackage: com.vita.workflow.controller.api
 * @Author: znk
 * @CreateTime: 2026-07-23
 * @Description: 工作流定义 HTTP 接口契约
 * @Version: 1.0
 */
@RestController
public interface WorkflowDefinitionControllerApi {

    /**
     * 分页查询可发起的已发布流程定义。
     *
     * @param searchDto 查询条件
     * @return 流程定义分页结果
     */
    @GetMapping("/workflow/definitions/published")
    CommonResult<PageResponse<WorkflowDefinitionVo>> published(@Valid WorkflowDefinitionSearchDto searchDto);

    /**
     * 管理端分页查询流程定义。
     *
     * @param searchDto 查询条件
     * @return 流程定义分页结果
     */
    @GetMapping("/system/workflow/definitions/page")
    CommonResult<PageResponse<WorkflowDefinitionVo>> definitionPage(
            @Valid WorkflowDefinitionSearchDto searchDto);

    /**
     * 管理端查询流程定义详情。
     *
     * @param definitionId 流程定义 ID
     * @return 流程定义详情
     */
    @GetMapping("/system/workflow/definitions/{definitionId}")
    CommonResult<WorkflowDefinitionVo> definitionDetail(
            @PathVariable("definitionId") Long definitionId);

    /**
     * 发布流程定义。
     *
     * @param definitionId 流程定义 ID
     * @return 发布结果
     */
    @PutMapping("/system/workflow/definitions/{definitionId}/publish")
    CommonResult<Boolean> publishDefinition(@PathVariable("definitionId") Long definitionId);

    /**
     * 取消发布流程定义。
     *
     * @param definitionId 流程定义 ID
     * @return 取消发布结果
     */
    @PutMapping("/system/workflow/definitions/{definitionId}/unpublish")
    CommonResult<Boolean> unpublishDefinition(@PathVariable("definitionId") Long definitionId);

    /**
     * 复制流程定义。
     *
     * @param definitionId 流程定义 ID
     * @return 复制结果
     */
    @PostMapping("/system/workflow/definitions/{definitionId}/copy")
    CommonResult<Boolean> copyDefinition(@PathVariable("definitionId") Long definitionId);

    /**
     * 安全删除尚未产生实例的流程定义。
     *
     * @param definitionId 流程定义 ID
     * @return 删除结果
     */
    @DeleteMapping("/system/workflow/definitions/{definitionId}")
    CommonResult<Boolean> deleteDefinition(@PathVariable("definitionId") Long definitionId);

    /**
     * 导入 Warm-Flow 流程定义 JSON。
     *
     * @param defJson 流程定义对象
     * @return 导入结果
     */
    @PostMapping(value = "/system/workflow/importJson", consumes = MediaType.APPLICATION_JSON_VALUE)
    CommonResult<Boolean> importJson(@RequestBody(required = false) DefJson defJson);
}
