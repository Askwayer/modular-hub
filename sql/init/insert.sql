-- ----------------------------
-- Records of sys_user
-- 默认账号：
-- 1. admin / admin@123456
-- 2. common / common@123456
-- 密码已按 com.vita.utils.Sm4Utils.encryptToBase64 预加密
-- ----------------------------
INSERT INTO `sys_user` (`id`, `dept_id`, `nick_name`, `user_name`, `password`, `mobile`, `email`, `avatar_url`,
                        `gender`,
                        `status`, `is_super_admin`, `is_system`, `login_time`, `login_address`, `pwd_update_date`,
                        `create_time`, `update_time`, `is_deleted`, `tenant_id`, `create_by_id`, `create_by`,
                        `update_by_id`, `update_by`, `version`, `remark`)
VALUES (1, NULL, '系统管理员', 'admin', 'VFxoaF65+ZEmXPwhNr/ixA==', NULL, 'admin@vita.local', NULL, '2',
        1, 1, 1, NULL, NULL, '2026-04-02 00:00:00',
        '2026-04-02 00:00:00', '2026-04-02 00:00:00', 0, 0, 1, 'system',
        1, 'system', 0, '初始化超级管理员账号'),
       (2, NULL, '普通用户', 'common', 'ZI26U0eoH9c2jh6CmEEdqw==', NULL, 'common@vita.local', NULL, '2',
        1, 0, 0, NULL, NULL, '2026-04-02 00:00:00',
        '2026-04-02 00:00:00', '2026-04-02 00:00:00', 0, 0, 1, 'system',
        1, 'system', 0, '初始化普通用户账号');

-- ----------------------------
-- Records of sys_role
-- 说明：超级管理员真正判定仍以 sys_user.is_super_admin 为准
-- ----------------------------
INSERT INTO `sys_role` (`id`, `role_name`, `role_code`, `role_sort`, `status`, `data_scope_type`, `is_system`,
                        `is_deleted`, `tenant_id`, `create_time`, `create_by_id`, `create_by`, `update_time`,
                        `update_by_id`, `update_by`, `version`, `remark`)
VALUES (1, '超级管理员角色', 'SUPER_ADMIN_ROLE', 1, 1, 'ALL', 1,
        0, 0, '2026-04-02 00:00:00', 1, 'system', '2026-04-02 00:00:00',
        1, 'system', 0, '用于初始化菜单与权限，超级管理员语义仍以用户标记为准'),
       (2, '普通用户角色', 'COMMON_USER', 2, 1, 'SELF', 1,
        0, 0, '2026-04-02 00:00:00', 1, 'system', '2026-04-02 00:00:00',
        1, 'system', 0, '普通用户默认角色');

-- ----------------------------
-- Records of sys_menu
-- ----------------------------
INSERT INTO `sys_menu` (`id`, `parent_id`, `menu_name`, `menu_type`, `route_name`, `route_link`, `component_path`,
                        `icon`,
                        `sort_no`, `visible`, `is_cache`, `always_show`, `is_external`, `status`, `is_system`,
                        `is_deleted`, `tenant_id`, `create_time`, `create_by_id`, `create_by`, `update_time`,
                        `update_by_id`, `update_by`, `version`, `remark`)
VALUES (1000, 0, '系统管理', 'CONTENTS', 'System', '/system', 'Layout', 'setting',
        10, 1, 0, 1, 0, 1, 1,
        0, 0, '2026-04-02 00:00:00', 1, 'system', '2026-04-02 00:00:00',
        1, 'system', 0, '系统管理根目录'),
       (1001, 1000, '用户管理', 'MENU', 'SysUser', '/system/sysUser', 'system/sysUser/index', 'user',
        11, 1, 0, 0, 0, 1, 1,
        0, 0, '2026-04-02 00:00:00', 1, 'system', '2026-04-02 00:00:00',
        1, 'system', 0, '用户管理目录'),
       (1002, 1000, '角色管理', 'MENU', 'SysRole', '/system/sysRole', 'system/sysRole/index', 'peoples',
        12, 1, 0, 0, 0, 1, 1,
        0, 0, '2026-04-02 00:00:00', 1, 'system', '2026-04-02 00:00:00',
        1, 'system', 0, '角色管理目录'),
       (1003, 1000, '菜单管理', 'MENU', 'SysMenu', '/system/sysMenu', 'system/sysMenu/index', 'menu',
        13, 1, 0, 0, 0, 1, 1,
        0, 0, '2026-04-02 00:00:00', 1, 'system', '2026-04-02 00:00:00',
        1, 'system', 0, '菜单管理目录');

-- ----------------------------
-- Records of sys_permission
-- 统一采用 模块:资源:动作 规范
-- ----------------------------
INSERT INTO `sys_permission` (`id`, `permission_name`, `permission_code`, `permission_type`, `menu_id`, `api_method`,
                              `api_path`, `auth_tag`, `sort_no`, `status`, `is_system`, `is_deleted`, `tenant_id`,
                              `create_time`, `create_by_id`, `create_by`, `update_time`, `update_by_id`, `update_by`,
                              `version`, `remark`)
VALUES (2001, '用户管理查看', 'system:user:view', 'MENU_ACTION', 1001, NULL,
        NULL, 'system:user:view', 11, 1, 1, 0, 0,
        '2026-04-02 00:00:00', 1, 'system', '2026-04-02 00:00:00', 1, 'system',
        0, '涵盖用户详情、列表、分页查询'),
       (2002, '用户管理新增', 'system:user:add', 'MENU_ACTION', 1001, 'POST',
        '/system/sysUser/add', 'system:user:add', 12, 1, 1, 0, 0,
        '2026-04-02 00:00:00', 1, 'system', '2026-04-02 00:00:00', 1, 'system',
        0, '用户新增权限'),
       (2003, '用户管理修改', 'system:user:update', 'MENU_ACTION', 1001, 'POST',
        '/system/sysUser/update', 'system:user:update', 13, 1, 1, 0, 0,
        '2026-04-02 00:00:00', 1, 'system', '2026-04-02 00:00:00', 1, 'system',
        0, '用户更新权限'),
       (2004, '用户管理删除', 'system:user:delete', 'MENU_ACTION', 1001, 'DELETE',
        '/system/sysUser/delete', 'system:user:delete', 14, 1, 1, 0, 0,
        '2026-04-02 00:00:00', 1, 'system', '2026-04-02 00:00:00', 1, 'system',
        0, '用户删除权限'),
       (2005, '角色管理查看', 'system:role:view', 'MENU_ACTION', 1002, NULL,
        NULL, 'system:role:view', 21, 1, 1, 0, 0,
        '2026-04-02 00:00:00', 1, 'system', '2026-04-02 00:00:00', 1, 'system',
        0, '涵盖角色详情、列表、分页查询'),
       (2006, '角色管理新增', 'system:role:add', 'MENU_ACTION', 1002, 'POST',
        '/system/sysRole/add', 'system:role:add', 22, 1, 1, 0, 0,
        '2026-04-02 00:00:00', 1, 'system', '2026-04-02 00:00:00', 1, 'system',
        0, '角色新增权限'),
       (2007, '角色管理修改', 'system:role:update', 'MENU_ACTION', 1002, 'POST',
        '/system/sysRole/update', 'system:role:update', 23, 1, 1, 0, 0,
        '2026-04-02 00:00:00', 1, 'system', '2026-04-02 00:00:00', 1, 'system',
        0, '角色更新权限'),
       (2008, '角色管理删除', 'system:role:delete', 'MENU_ACTION', 1002, 'DELETE',
        '/system/sysRole/delete', 'system:role:delete', 24, 1, 1, 0, 0,
        '2026-04-02 00:00:00', 1, 'system', '2026-04-02 00:00:00', 1, 'system',
        0, '角色删除权限'),
       (2009, '菜单管理查看', 'system:menu:view', 'MENU_ACTION', 1003, NULL,
        NULL, 'system:menu:view', 31, 1, 1, 0, 0,
        '2026-04-02 00:00:00', 1, 'system', '2026-04-02 00:00:00', 1, 'system',
        0, '涵盖菜单详情、列表、分页查询'),
       (2010, '菜单管理新增', 'system:menu:add', 'MENU_ACTION', 1003, 'POST',
        '/system/sysMenu/add', 'system:menu:add', 32, 1, 1, 0, 0,
        '2026-04-02 00:00:00', 1, 'system', '2026-04-02 00:00:00', 1, 'system',
        0, '菜单新增权限'),
       (2011, '菜单管理修改', 'system:menu:update', 'MENU_ACTION', 1003, 'POST',
        '/system/sysMenu/update', 'system:menu:update', 33, 1, 1, 0, 0,
        '2026-04-02 00:00:00', 1, 'system', '2026-04-02 00:00:00', 1, 'system',
        0, '菜单更新权限'),
       (2012, '菜单管理删除', 'system:menu:delete', 'MENU_ACTION', 1003, 'DELETE',
        '/system/sysMenu/delete', 'system:menu:delete', 34, 1, 1, 0, 0,
        '2026-04-02 00:00:00', 1, 'system', '2026-04-02 00:00:00', 1, 'system',
        0, '菜单删除权限');

-- ----------------------------
-- Records of sys_user_role
-- ----------------------------
INSERT INTO `sys_user_role` (`id`, `user_id`, `role_id`, `is_deleted`, `tenant_id`, `create_time`, `create_by_id`,
                             `create_by`, `update_time`, `update_by_id`, `update_by`, `version`)
VALUES (3001, 1, 1, 0, 0, '2026-04-02 00:00:00', 1,
        'system', '2026-04-02 00:00:00', 1, 'system', 0),
       (3002, 2, 2, 0, 0, '2026-04-02 00:00:00', 1,
        'system', '2026-04-02 00:00:00', 1, 'system', 0);

-- ----------------------------
-- Records of sys_role_menu
-- 普通用户需显式关联根目录，否则菜单树缺失父节点
-- ----------------------------
INSERT INTO `sys_role_menu` (`id`, `role_id`, `menu_id`, `is_deleted`, `tenant_id`, `create_time`, `create_by_id`,
                             `create_by`, `update_time`, `update_by_id`, `update_by`, `version`)
VALUES (4001, 1, 1000, 0, 0, '2026-04-02 00:00:00', 1,
        'system', '2026-04-02 00:00:00', 1, 'system', 0),
       (4002, 1, 1001, 0, 0, '2026-04-02 00:00:00', 1,
        'system', '2026-04-02 00:00:00', 1, 'system', 0),
       (4003, 1, 1002, 0, 0, '2026-04-02 00:00:00', 1,
        'system', '2026-04-02 00:00:00', 1, 'system', 0),
       (4004, 1, 1003, 0, 0, '2026-04-02 00:00:00', 1,
        'system', '2026-04-02 00:00:00', 1, 'system', 0),
       (4005, 2, 1000, 0, 0, '2026-04-02 00:00:00', 1,
        'system', '2026-04-02 00:00:00', 1, 'system', 0),
       (4006, 2, 1001, 0, 0, '2026-04-02 00:00:00', 1,
        'system', '2026-04-02 00:00:00', 1, 'system', 0),
       (4007, 2, 1002, 0, 0, '2026-04-02 00:00:00', 1,
        'system', '2026-04-02 00:00:00', 1, 'system', 0),
       (4008, 2, 1003, 0, 0, '2026-04-02 00:00:00', 1,
        'system', '2026-04-02 00:00:00', 1, 'system', 0);

-- ----------------------------
-- Records of sys_role_permission
-- 超级管理员角色关联全量权限，普通用户角色仅保留查看权限
-- ----------------------------
INSERT INTO `sys_role_permission` (`id`, `role_id`, `permission_id`, `is_deleted`, `tenant_id`, `create_time`,
                                   `create_by_id`, `create_by`, `update_time`, `update_by_id`, `update_by`, `version`)
VALUES (5001, 1, 2001, 0, 0, '2026-04-02 00:00:00',
        1, 'system', '2026-04-02 00:00:00', 1, 'system', 0),
       (5002, 1, 2002, 0, 0, '2026-04-02 00:00:00',
        1, 'system', '2026-04-02 00:00:00', 1, 'system', 0),
       (5003, 1, 2003, 0, 0, '2026-04-02 00:00:00',
        1, 'system', '2026-04-02 00:00:00', 1, 'system', 0),
       (5004, 1, 2004, 0, 0, '2026-04-02 00:00:00',
        1, 'system', '2026-04-02 00:00:00', 1, 'system', 0),
       (5005, 1, 2005, 0, 0, '2026-04-02 00:00:00',
        1, 'system', '2026-04-02 00:00:00', 1, 'system', 0),
       (5006, 1, 2006, 0, 0, '2026-04-02 00:00:00',
        1, 'system', '2026-04-02 00:00:00', 1, 'system', 0),
       (5007, 1, 2007, 0, 0, '2026-04-02 00:00:00',
        1, 'system', '2026-04-02 00:00:00', 1, 'system', 0),
       (5008, 1, 2008, 0, 0, '2026-04-02 00:00:00',
        1, 'system', '2026-04-02 00:00:00', 1, 'system', 0),
       (5009, 1, 2009, 0, 0, '2026-04-02 00:00:00',
        1, 'system', '2026-04-02 00:00:00', 1, 'system', 0),
       (5010, 1, 2010, 0, 0, '2026-04-02 00:00:00',
        1, 'system', '2026-04-02 00:00:00', 1, 'system', 0),
       (5011, 1, 2011, 0, 0, '2026-04-02 00:00:00',
        1, 'system', '2026-04-02 00:00:00', 1, 'system', 0),
       (5012, 1, 2012, 0, 0, '2026-04-02 00:00:00',
        1, 'system', '2026-04-02 00:00:00', 1, 'system', 0),
       (5013, 2, 2001, 0, 0, '2026-04-02 00:00:00',
        1, 'system', '2026-04-02 00:00:00', 1, 'system', 0),
       (5014, 2, 2005, 0, 0, '2026-04-02 00:00:00',
        1, 'system', '2026-04-02 00:00:00', 1, 'system', 0),
       (5015, 2, 2009, 0, 0, '2026-04-02 00:00:00',
        1, 'system', '2026-04-02 00:00:00', 1, 'system', 0);

SET
FOREIGN_KEY_CHECKS = 1;
