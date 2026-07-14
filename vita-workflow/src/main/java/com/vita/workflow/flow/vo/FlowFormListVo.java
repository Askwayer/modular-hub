package com.vita.workflow.flow.vo;

import lombok.Data;

/**
 * @BelongsProject: vita
 * @BelongsPackage: com.vita.workflow.flow.vo
 * @Author znk
 * @CreateTime 2026-07-14 15:51:28
 * @Description: 流程表单表 列表 VO
 * @version 1.0
 */
@Data
public class FlowFormListVo {
    /**
     * 表单编码
     */
    private String formCode;

    /**
     * 表单名称
     */
    private String formName;

    /**
     * 是否发布（0未发布 1已发布 9失效）
     */
    private Boolean isPublish;

    /**
     * 表单类型（0内置表单 存form_content 1外挂表单 存form_path）
     */
    private Boolean formType;

    /**
     * 表单路径
     */
    private String formPath;

    /**
     * 表单内容
     */
    private String formContent;

    /**
     * 表单扩展，用户自行使用
     */
    private String ext;

    /**
     * 删除标志
     */
    private String delFlag;

}
