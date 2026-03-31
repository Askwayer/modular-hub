/*
 Navicat Premium Dump SQL

 Source Server         : kangCloud
 Source Server Type    : MySQL
 Source Server Version : 80037 (8.0.37)
 Source Host           : 106.54.205.33:3306
 Source Schema         : be_vita

 Target Server Type    : MySQL
 Target Server Version : 80037 (8.0.37)
 File Encoding         : 65001

 Date: 20/03/2026 16:09:12
*/

SET NAMES utf8mb4;
SET
FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for sys_oper_log
-- ----------------------------
DROP TABLE IF EXISTS `sys_oper_log`;
CREATE TABLE `sys_oper_log`
(
    `id`              bigint   NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `module`          varchar(100) DEFAULT NULL COMMENT '模块',
    `business_type`   varchar(100) DEFAULT NULL COMMENT '业务类型',
    `request_method`  varchar(20)  DEFAULT NULL COMMENT '请求方式',
    `oper_url`        varchar(500) DEFAULT NULL COMMENT '请求URL',
    `oper_ip`         varchar(64)  DEFAULT NULL COMMENT '操作IP',
    `oper_location`   varchar(255) DEFAULT NULL COMMENT '操作地点',
    `oper_name`       varchar(100) DEFAULT NULL COMMENT '操作人名称',
    `oper_user_id`    bigint       DEFAULT NULL COMMENT '操作人ID',
    `class_name`      varchar(255) DEFAULT NULL COMMENT '类名',
    `method_name`     varchar(255) DEFAULT NULL COMMENT '方法名',
    `request_param`   longtext COMMENT '请求参数',
    `response_result` longtext COMMENT '返回结果',
    `status`          tinyint      DEFAULT '1' COMMENT '状态 1成功 0失败',
    `error_msg`       longtext COMMENT '错误信息',
    `cost_time`       bigint       DEFAULT '0' COMMENT '耗时(ms)',
    `oper_time`       datetime     DEFAULT CURRENT_TIMESTAMP COMMENT '操作时间',
    `create_by_id`    bigint       DEFAULT NULL COMMENT '创建人ID',
    `create_by`       varchar(64)  DEFAULT NULL COMMENT '创建人',
    `create_time`     datetime NOT NULL COMMENT '创建时间',
    `update_by_id`    bigint       DEFAULT NULL COMMENT '更新人ID',
    `update_by`       varchar(64)  DEFAULT NULL COMMENT '更新人',
    `update_time`     datetime     DEFAULT NULL COMMENT '更新时间',
    `tenant_id`       bigint       DEFAULT '0' COMMENT '租户ID',
    `version`         int          DEFAULT '0' COMMENT '版本号',
    `is_deleted`      tinyint      DEFAULT '0' COMMENT '是否删除',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=2032385430369927170 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='系统操作日志表';

-- ----------------------------
-- Table structure for sys_user
-- ----------------------------
DROP TABLE IF EXISTS `sys_user`;
CREATE TABLE `sys_user`
(
    `id`              bigint                                                        NOT NULL AUTO_INCREMENT COMMENT 'ID',
    `dept_id`         bigint                                                                 DEFAULT NULL COMMENT '部门ID',
    `nick_name`       varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci           DEFAULT NULL COMMENT '昵称',
    `user_name`       varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci  NOT NULL COMMENT '用户名称',
    `password`        varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '密码',
    `mobile`          varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci           DEFAULT NULL COMMENT '手机号',
    `email`           varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci          DEFAULT NULL COMMENT '邮箱',
    `avatar_url`      varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci          DEFAULT NULL COMMENT '头像',
    `gender`          char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci               DEFAULT '2' COMMENT '性别(0-女,1-男,2-其他)',
    `status`          tinyint                                                       NOT NULL DEFAULT '1' COMMENT '状态 1启用 0禁用',
    `is_super`        char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci               DEFAULT '0' COMMENT '是否为超级管理员(0-否,1-是)',
    `login_time`      datetime                                                               DEFAULT NULL COMMENT '上次登录时间',
    `login_address`   varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci          DEFAULT NULL COMMENT '登录地址',
    `pwd_update_date` datetime                                                               DEFAULT NULL COMMENT '密码最后更新时间',
    `create_time`     datetime                                                      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '新增时间',
    `update_time`     datetime                                                               DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `is_deleted`      tinyint(1) DEFAULT '0' COMMENT '是否删除(0-否,1-是)',
    `tenant_id`       bigint                                                                 DEFAULT NULL COMMENT '租户ID',
    `create_by_id`    bigint                                                                 DEFAULT NULL COMMENT '新增用户ID',
    `create_by`       varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci           DEFAULT NULL COMMENT '新增用户名称',
    `update_by_id`    bigint                                                                 DEFAULT NULL COMMENT '更新用户ID',
    `update_by`       varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci           DEFAULT NULL COMMENT '更新用户名称',
    `version`         bigint                                                                 DEFAULT '0' COMMENT '乐观锁',
    `remark`          varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci          DEFAULT NULL COMMENT '备注',
    PRIMARY KEY (`id`),
    UNIQUE KEY `user_name` (`user_name`),
    KEY               `idx_user_user_name` (`user_name`),
    KEY               `idx_user_dept_id` (`dept_id`)
) ENGINE=InnoDB AUTO_INCREMENT=2032385430034382851 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='用户信息表';

-- ----------------------------
-- Table structure for sys_login_log
-- ----------------------------
DROP TABLE IF EXISTS `sys_login_log`;
CREATE TABLE `sys_login_log`
(
    `id`             bigint NOT NULL AUTO_INCREMENT COMMENT '访问ID',
    `user_name`      varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci  DEFAULT '' COMMENT '用户账号',
    `ipaddr`         varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT '' COMMENT '登录IP地址',
    `login_location` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT '' COMMENT '登录地点',
    `browser`        varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci  DEFAULT '' COMMENT '浏览器类型',
    `os`             varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci  DEFAULT '' COMMENT '操作系统',
    `status`         char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci      DEFAULT '0' COMMENT '登录状态（0成功 1失败）',
    `msg`            varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT '' COMMENT '提示消息',
    `login_time`     datetime                                                      DEFAULT NULL COMMENT '访问时间',
    `create_time`    datetime                                                      DEFAULT CURRENT_TIMESTAMP COMMENT '新增时间',
    `update_time`    datetime                                                      DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `is_deleted`     tinyint(1) DEFAULT '0' COMMENT '是否删除',
    `tenant_id`      bigint                                                        DEFAULT NULL COMMENT '租户ID',
    `create_by_id`   bigint                                                        DEFAULT NULL COMMENT '新增用户ID',
    `create_by`      varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci  DEFAULT NULL COMMENT '新增用户名称',
    `update_by_id`   bigint                                                        DEFAULT NULL COMMENT '更新用户ID',
    `update_by`      varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci  DEFAULT NULL COMMENT '更新用户名称',
    `version`        bigint                                                        DEFAULT '0' COMMENT '乐观锁',
    PRIMARY KEY (`id`),
    KEY              `idx_sys_logininfor_s` (`status`),
    KEY              `idx_sys_logininfor_lt` (`login_time`)
) ENGINE=InnoDB AUTO_INCREMENT=2026640584676085761 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='系统访问记录';

-- ----------------------------
-- Table structure for sys_ip_block
-- ----------------------------
DROP TABLE IF EXISTS `sys_ip_block`;
CREATE TABLE `sys_ip_block`
(
    `id`           bigint                                 NOT NULL AUTO_INCREMENT COMMENT 'ID',
    `ip`           varchar(64) COLLATE utf8mb4_general_ci NOT NULL COMMENT 'IP地址',
    `status`       tinyint(1) NOT NULL DEFAULT '1' COMMENT '状态 1启用 0禁用',
    `start_time`   datetime                               NOT NULL COMMENT '开始时间',
    `end_time`     datetime                                DEFAULT NULL COMMENT '结束时间，永久封禁为空',
    `tenant_id`    bigint                                  DEFAULT NULL COMMENT '租户ID',
    `remark`       varchar(500) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '备注',
    `create_by_id` bigint                                  DEFAULT NULL COMMENT '创建人ID',
    `create_by`    varchar(64) COLLATE utf8mb4_general_ci  DEFAULT NULL COMMENT '创建人',
    `create_time`  datetime                                DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_by_id` bigint                                  DEFAULT NULL COMMENT '更新人ID',
    `update_by`    varchar(64) COLLATE utf8mb4_general_ci  DEFAULT NULL COMMENT '更新人',
    `update_time`  datetime                                DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `version`      int                                     DEFAULT '0' COMMENT '版本号',
    `is_deleted`   tinyint(1) DEFAULT '0' COMMENT '逻辑删除',
    PRIMARY KEY (`id`),
    KEY            `idx_ip_status` (`ip`,`status`),
    KEY            `idx_end_time` (`end_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='IP封禁表';
