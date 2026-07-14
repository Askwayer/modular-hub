package com.vita.workflow.flow.mapper;

import com.vita.mybatis.mapper.BaseMapperX;
import com.vita.workflow.flow.entity.FlowTask;
import org.apache.ibatis.annotations.Mapper;

/**
 * @BelongsProject: vita
 * @BelongsPackage: com.vita.workflow.flow.mapper
 * @Author znk
 * @CreateTime 2026-07-14 15:51:28
 * @Description: 待办任务表 Mapper 接口
 * @version 1.0
 */
@Mapper
public interface FlowTaskMapper extends BaseMapperX<FlowTask> {

}
