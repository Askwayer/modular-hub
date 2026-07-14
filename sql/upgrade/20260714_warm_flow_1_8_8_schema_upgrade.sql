-- Warm-Flow 1.8.8 表单能力兼容升级。
-- 仅补充缺失表，不删除或重建已有流程数据。
CREATE TABLE IF NOT EXISTS `flow_form`
(
    `id`           bigint(20) UNSIGNED NOT NULL COMMENT '主键id',
    `form_code`    varchar(40)         NOT NULL COMMENT '表单编码',
    `form_name`    varchar(100)        NOT NULL COMMENT '表单名称',
    `version`      varchar(20)         NOT NULL COMMENT '表单版本',
    `is_publish`   tinyint(1)          NOT NULL DEFAULT '0' COMMENT '是否发布（0未发布 1已发布 9失效）',
    `form_type`    tinyint(1)                   DEFAULT '0' COMMENT '表单类型（0内置表单 存form_content 1外挂表单 存form_path）',
    `form_path`    varchar(100)                  DEFAULT NULL COMMENT '表单路径',
    `form_content` longtext                      DEFAULT NULL COMMENT '表单内容',
    `ext`          varchar(400)                  DEFAULT NULL COMMENT '表单扩展，用户自行使用',
    `create_time`  datetime                      DEFAULT NULL COMMENT '创建时间',
    `create_by`    varchar(64)                   DEFAULT '' COMMENT '创建人',
    `update_time`  datetime                      DEFAULT NULL COMMENT '更新时间',
    `update_by`    varchar(64)                   DEFAULT '' COMMENT '更新人',
    `del_flag`     char(1)                       DEFAULT '0' COMMENT '删除标志',
    `tenant_id`    varchar(40)                   DEFAULT NULL COMMENT '租户id',
    PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB COMMENT ='流程表单表';
