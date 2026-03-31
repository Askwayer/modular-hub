# RBAC权限判定简单逻辑

- 数据库设计建议采用 RBAC（基于角色的访问控制）模型，核心表结构如下：

```text
sys_user
  └── sys_user_role
         └── sys_role
                ├── sys_role_menu
                │      └── sys_menu
                ├── sys_role_permission
                │      └── sys_permission
                └── sys_role_dept_scope
                       └── sys_dept
```

---

## 1. 登录认证

### 1.1 登录后缓存内容

- 调用用户信息接口缓存信息
    - 用户基础信息
    - 角色编码集合
    - 权限编码集合
    - 数据范围简要信息

```json
{
  "userId": 1,
  "userName": "admin",
  "roleCodes": [
    "SUPER_ADMIN",
    "SYSTEM_ADMIN"
  ],
  "permissionCodes": [
    "system:user:view",
    "system:user:add",
    "system:user:update"
  ]
}
```

---

## 2. 菜单控制

- 用户可见菜单 = 用户角色关联到的 `sys_menu`
    - 左侧菜单树
    - 路由动态加载
    - 页面显隐

---

## 3. 接口/按钮权限控制

- 用户权限 = 用户角色关联到的 sys_permission.permission_code
    - @SaCheckPermission("system:user:add")
    - 前端按钮显隐
    - 导出/审核/删除等动作控制

### 3.1 接口鉴权

```java
@SaCheckPermission("system:user:add")
```

### 3.2 超级管理员权限

- 通过`is_super_admin`字段标识超级管理员，拥有系统内所有权限，无需单独分配角色权限。

### 3.3 角色编码

```java
@SaCheckRole("SUPER_ADMIN")
```

---

## 4. 数据权限控制(后续开发拓展)
- 从角色取 data_scope_type，拼接 SQL 条件。
 - `ALL`：不加限制
 - `DEPT_AND_CHILD`：`dept_id in (...)`
 - `DEPT_SELF`：`dept_id` = 当前用户dept_id 
 - `SELF`：`create_by_id` = 当前用户id 
 - `CUSTOM`：`dept_id` in 角色自定义部门

> `service` 层查当前用户角色 -> 算出数据范围 -> `mapper` 按条件查询