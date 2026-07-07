package com.vita.workflow.category.service;

import com.vita.enums.CommonStatusEnum;
import com.vita.mybatis.LambdaQueryWrapperX;
import com.vita.workflow.category.entity.WorkflowCategory;
import com.vita.workflow.category.mapper.WorkflowCategoryMapper;
import org.dromara.warm.flow.core.dto.Tree;
import org.dromara.warm.flow.ui.service.CategoryService;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Warm-Flow 设计器流程分类服务。
 *
 * @author Codex
 */
@Service
public class WorkflowCategoryServiceImpl implements CategoryService {

    private final WorkflowCategoryMapper workflowCategoryMapper;

    public WorkflowCategoryServiceImpl(WorkflowCategoryMapper workflowCategoryMapper) {
        this.workflowCategoryMapper = workflowCategoryMapper;
    }

    @Override
    public List<Tree> queryCategory() {
        LambdaQueryWrapperX<WorkflowCategory> queryWrapper = new LambdaQueryWrapperX<>();
        queryWrapper.eq(WorkflowCategory::getStatus, CommonStatusEnum.ENABLED.getCode())
                .orderByAsc(WorkflowCategory::getSortNo, WorkflowCategory::getId);
        return workflowCategoryMapper.selectList(queryWrapper)
                .stream()
                .map(category -> new Tree(category.getCategoryCode(),
                        category.getCategoryName(),
                        category.getParentCode(),
                        null))
                .toList();
    }
}
