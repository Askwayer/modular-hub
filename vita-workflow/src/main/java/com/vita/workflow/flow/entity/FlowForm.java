package com.vita.workflow.flow.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.vita.core.entity.BaseEntity;
import lombok.Data;

import lombok.EqualsAndHashCode;

/**
 * @BelongsProject: vita
 * @BelongsPackage: com.vita.workflow.flow.entity
 * @Author znk
 * @CreateTime 2026-07-14 15:51:28
 * @Description: 流程表单表 实体类
 * @version 1.0
 */
@Data
@TableName("flow_form")
@EqualsAndHashCode(callSuper = true)
public class FlowForm extends BaseEntity {

    /**
     * 表单编码
     */
    @TableField("form_code")
    private String formCode;

    /**
     * 表单名称
     */
    @TableField("form_name")
    private String formName;

    /**
     * 是否发布（0未发布 1已发布 9失效）
     */
    @TableField("is_publish")
    private Boolean isPublish;

    /**
     * 表单类型（0内置表单 存form_content 1外挂表单 存form_path）
     */
    @TableField("form_type")
    private Boolean formType;

    /**
     * 表单路径
     */
    @TableField("form_path")
    private String formPath;

    /**
     * 表单内容
     */
    @TableField("form_content")
    private String formContent;

    /**
     * 表单扩展，用户自行使用
     */
    @TableField("ext")
    private String ext;

    /**
     * 删除标志
     */
    @TableField("del_flag")
    private String delFlag;
}
