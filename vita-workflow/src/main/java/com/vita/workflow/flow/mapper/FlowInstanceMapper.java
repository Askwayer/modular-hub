package com.vita.workflow.flow.mapper;

import com.vita.mybatis.mapper.BaseMapperX;
import com.vita.workflow.flow.entity.FlowInstance;
import org.apache.ibatis.annotations.Mapper;

/**
 * @BelongsProject: vita
 * @BelongsPackage: com.vita.workflow.flow.mapper
 * @Author znk
 * @CreateTime 2026-07-14 15:51:28
 * @Description: 流程实例表 Mapper 接口
 * @version 1.0
 */
@Mapper
public interface FlowInstanceMapper extends BaseMapperX<FlowInstance> {

}
