# RBAC 开发文档

## 1. 开发目标

本开发文档用于指导 RBAC 一期实现，目标是在当前仓库结构下，以最少必要改动完成：

- 当前用户角色获取
- 当前用户权限获取
- 当前用户菜单获取
- `Sa-Token` 角色/权限回调接入
- 管理端接口权限注解落地

---

## 2. 模块改造范围

### 2.1 `vita-common`

职责：

- 放公共鉴权常量、登录态模型

建议改动：

- `AuthConstants`
    - 增加 权限和角色 Redis Key，例如：
        - `LOGIN_USER_PERMISSION`
        - `LOGIN_USER_ROLE`
        - 保存角色码、权限码
- `LoginUserInfoModel`
    - 保留基础用户信息

### 2.2 `vita-service`

职责：

- 负责 RBAC 聚合查询、认证编排和 Sa-Token 回调实现

建议改动：

- `IAuthCommonService`
    - 统一承接角色码、权限码、菜单和超管判定查询
- `AuthCommonServiceImpl`
    - 实现聚合查询逻辑
- `StpInterfaceImpl`
    - 放到 `com.vita.auth` 包下
    - 优先读 `LOGIN_USER_ROLE`、`LOGIN_USER_PERMISSION`
    - 未命中再通过 `IAuthCommonService` 回源
- 相关 mapper 增加联查方法：
    - `SysUserRoleMapper`
    - `SysRolePermissionMapper`
    - `SysRoleMenuMapper`
    - `SysMenuMapper`
- `AuthServiceImpl`
    - 增加当前用户权限初始化能力
    - 负责构建 `AuthInfoVo`

### 2.3 `vita-admin`

职责：

- 暴露接口，承接管理端鉴权注解

建议改动：

- `AuthController`
    - 扩展 `GET /auth/info`
- 业务控制器
    - 按权限码逐步增加 `@SaCheckPermission`

### 2.4 `sql`

职责：

- 确保初始化脚本与设计一致

建议检查：

- `sys_user.is_super_admin`
- `sys_permission.auth_tag`
- 关联表唯一索引和启用状态

---

## 3. 返回模型设计建议

### 3.1 Redis 缓存对象

用途：

- 给 `StpInterfaceImpl` 用
- 给 `/auth/info` 初始化缓存用
- 保存角色码、权限码

### 3.2 权限初始化返回对象

建议新增：

```java
public class AuthInfoVo {
    private LoginUserInfoModel userInfo;
    private List<String> roleCodes;
    private List<String> permissionCodes;
}
```

用途：

- 管理端登录后的权限初始化接口
- 只返回“登录用户 + 角色码 + 权限码”
- 路由树通过独立的 `/auth/routers` 返回，避免把 RBAC 初始化和前端路由结构耦合在一起

### 3.3 菜单树返回对象

建议拆成两类，而不是复用同一个返回对象：

```java
List<TreeSelectModel>    // 下拉树
List<RouterVo>           // 前端路由树
```

建议约定：

- 下拉树给菜单管理、角色授权、权限配置等场景使用
- 路由树只给前端路由初始化使用
- 下拉树不要复用 `RouterVo`
- 路由树不要复用下拉树模型

---

## 4. 推荐服务拆分

### 4.1 `IAuthCommonService`

建议最少提供以下方法：

- `boolean isSuperAdmin(Long userId)`
- `List<String> queryRoleCodes(Long userId)`
- `List<String> queryPermissionCodes(Long userId)`
- `List<Long> queryMenuIds(Long userId)` 或 `List<SysMenu> queryMenus(Long userId)`

### 4.2 `AuthServiceImpl`

建议保持职责收口：

- `login`：认证，不做重 RBAC 查询
- `getCurrentAuthInfo` 或改造 `getCurrentUserInfo`
  - 聚合角色、权限
  - 构建 `AuthInfoVo`
  - 缓存角色码、权限码到 Session，供 `StpInterfaceImpl` 使用
- `getRouters`
  - 只负责获取当前用户路由树
  - 路由树构建逻辑收口到 `SysMenuService`

### 4.3 `ISysMenuService`

菜单能力建议拆分成三层：

- 原始查询
  - `getMenuList(searchDto)`：菜单管理页原始菜单查询
  - `getMenusByUserId(userId)`：当前用户授权菜单原始列表
- 树结构
  - `getMenuTreeByUserId(userId)`：当前用户菜单树
  - `getMenuTreeSelect(searchDto)`：菜单下拉树
- 路由结构
  - `getRoutersByUserId(userId)`：前端路由树

不要把“查询菜单”“构建树”“构建路由”混在一个 `buildMenus` 方法里。

---

## 5. 查询实现建议

### 5.1 实现方式

推荐按“单表查询下沉 mapper，多表聚合收口 service”的方式实现。

原因：

- 角色、权限、菜单的单表过滤、排序属于持久化细节
- 用户角色、角色菜单、角色权限属于 RBAC 聚合编排，放 service 更清晰
- 菜单树、路由树属于展示结构转换，不应放在 mapper 或 auth service 中

### 5.2 角色查询约束

- 只查启用角色
- 只查未删除关联
- 去重

### 5.3 权限查询约束

- 只查启用权限
- 返回 `permission_code` 或 `auth_tag`
- 若用户级超级管理员标记为 1，直接返回 `["**:**:**"]`

### 5.4 菜单查询约束

- 授权菜单原始查询：只查启用菜单
- 菜单下拉树：基于原始菜单树构建，不要复用路由结构
- 前端路由树：由 `visible` 映射为 `hidden`，不要在原始查询阶段把隐藏菜单直接裁掉
- 若需要按钮权限，不要从菜单表补推，而要从权限表获取
- 后续菜单权限、按钮权限、授权树扩展，应复用“原始菜单查询 + 树构建”链路，而不是复用 `RouterVo`

---

## 6. `StpInterfaceImpl` 实现建议

### 6.1 `getRoleList`

推荐逻辑：

1. `loginId` 转 `Long`
2. 从 Session 取 `LOGIN_USER_ROLE`
3. 命中则直接返回
4. 未命中则调用 `IAuthCommonService.getRoleCodesByUserId`
5. 若用户是超级管理员，返回虚拟角色码 `SUPER_ADMIN`

### 6.2 `getPermissionList`

推荐逻辑：

1. 从 Session 取 `LOGIN_USER_PERMISSION`
2. 命中则直接返回
3. 未命中则调用 `IAuthCommonService.getPermissionCodesByUserId`
4. 超级管理员返回 `["**:**:**"]`

### 6.3 注意事项

- 不要在 `StpInterfaceImpl` 中直接拼 SQL
- 回调实现只做读取缓存和兜底回源，不做复杂业务编排
- 真正的聚合逻辑收口到 `IAuthCommonService`

---

## 7. `/auth/info` 与 `/auth/routers` 接口建议

### 7.1 推荐返回结构

```json
{
  "userInfo": {
    "id": 1,
    "username": "admin",
    "nickName": "系统管理员",
    "deptId": 100,
    "isSuperAdmin": 1,
    "isSystem": 0
  },
  "roleCodes": [
    "SUPER_ADMIN"
  ],
  "permissionCodes": [
    "**:**:**"
  ]
}
```

### 7.2 推荐处理流程

`GET /auth/info`

1. 从 Session 取基础登录用户
2. 根据 `userId` 聚合角色码、权限码
3. 组装 `AuthInfoVo`
4. 写入 RBAC 上下文缓存
5. 返回前端

`GET /auth/routers`

1. 根据 `userId` 查询授权菜单原始列表
2. 构建菜单树
3. 构建 `RouterVo` 路由树
4. 返回前端

### 7.3 为什么不建议在 `/auth/login` 里做完

- 登录链路更容易被高频调用
- RBAC 聚合查询跨表较多
- 菜单树结构只在前端初始化时需要
- 下拉树和路由树的消费方不同，不应和登录认证链路耦合

---

## 8. 权限码规范建议

推荐统一格式：

```text
模块:资源:动作
```

示例：

- `system:user:view`
- `system:user:add`
- `system:user:update`
- `system:user:delete`
- `system:role:grant`

原则：

- 一个动作一个权限码
- 不要把 URL 直接当权限码
- 菜单权限和接口权限保持同一命名空间

---

## 9. 测试建议

### 9.1 单元测试

至少覆盖：

- 超级管理员直接拥有全部权限
- 普通用户根据角色获取权限
- 停用角色后权限失效
- 停用权限后权限失效
- 空角色用户返回空集合

### 9.2 集成测试

至少覆盖：

- `/auth/info` 返回角色码、权限码
- `/auth/routers` 返回当前用户路由树
- 菜单下拉树接口返回 `TreeSelectModel`
- `@SaCheckPermission` 成功/失败分支
- `@SaCheckRole` 成功/失败分支

### 9.3 回归测试

重点关注：

- 登录接口响应时间是否被明显拉长
- 未登录接口是否被误判
- 超级管理员是否被普通角色覆盖

---

## 10. 一期不做但要留口的内容

以下内容不建议在一期一起做完：

- 数据范围 SQL 自动注入
- 字段权限
- 接口路径自动映射为权限码
- Redis 级分布式权限缓存

但要提前留好扩展位置：

- `data_scope_type`
- `sys_role_dept_scope`
- `LOGIN_USER_ROLE`
- `LOGIN_USER_PERMISSION`
- `IAuthCommonService`

---

## 11. 实施完成标志

满足以下条件即可开始进入业务接口批量接入阶段：

- RBAC 聚合服务已完成
- `/auth/info` 已返回权限初始化数据
- `/auth/routers` 已返回当前用户路由树
- 菜单下拉树已独立返回 `TreeSelectModel`
- `StpInterfaceImpl` 已可返回角色和权限
- 至少一组业务接口已成功接入 `@SaCheckPermission`
- 单元测试与集成测试通过
