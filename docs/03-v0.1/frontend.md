# V0.1 前端与 API 集成

[设计导航](README.md)

## 基本原则

保留web-ele与已有requestClient，在应用层改认证、路由和权限适配；不大改packages底层，不再封装一套Axios。Vben是门户脚手架，不是所有未来业务的强制依赖。

## 响应与错误

保持 `{code, success, msg, content}`，成功HTTP200/code200。分页输入pageNum/pageSize，输出content.list/total。ID使用字符串。

| 场景 | HTTP | 业务码 |
| --- | --- | --- |
| 参数/JSON错误 | 400 | 400 |
| 登录失败、过期、未登录 | 401 | 401 |
| 无权 | 403 | 403 |
| 不存在 | 404 | 404 |
| 唯一/状态冲突 | 409 | 明确冲突码 |
| 过频 | 429 | 429 |
| 重复提交 | 409 | 900保留或明确迁移 |
| 未分类业务拒绝 | 400 | 原业务码 |
| 系统/依赖异常 | 500/503 | 对应码 |

不能将900/1001等直接设为HTTP状态。X-Request-Id关联日志，限制头长度和字符；不返回堆栈/连接信息。

## 页面和API

浏览器使用/api代理至后端/admin/api；以下省略前缀。

| 页面 | 路由 | 接口 |
| --- | --- | --- |
| 登录 | /auth/login | POST /auth/login、GET /captcha/captcha |
| 首页 | /workspace | GET /auth/info |
| 用户/角色分配 | /system/users | sysUser CRUD、角色选择、setRoles |
| 部门 | /system/depts | sysDept CRUD |
| 角色/授权 | /system/roles | sysRole CRUD、get/setAuthorization |
| 权限目录 | /system/permissions | sysPermission CRUD |
| 菜单 | /system/menus | sysMenu CRUD |
| 只读演示 | /demo/context | GET /demo/context，hub:demo:view |
| 错误页 | 复用fallback | 403、404、系统错误 |

保留现有auth/info、auth/routers、POST auth/logout。拟新增用户getRoles/setRoles、角色getAuthorization/setAuthorization；完整保存menuIds和permissionIds，一事务处理。空数组清空，缺字段400。旧增量授权若保留也要同规则。

```json
{"roleId":"123","menuIds":["201","202"],"permissionIds":["301"]}
```

编辑使用对话框。未接通的注册、短信、扫码和找回密码入口隐藏/关闭；未改密码时不发送password字段。

## 请求与状态

- 登录401只提示，不进入再次退出循环。
- 已登录请求401清理身份、权限、菜单和路由，一次跳登录；并发401不重复处理。
- 403保留登录态。网络错误不当作无权或已退出服务端。
- 退出API失败仍清本地状态，但提示服务端撤销未确认；Token受有效期限制。
- 可用整页跳登录确保动态路由清空，避免侵入Router内部。
- auth/info不重复回Token；登录结果是Token来源。
- hasHubPermission/HubCan在应用层处理精确码与服务端超级管理员标记；不修改共享useAccess的通配语义。

## 动态路由

- 固定backend模式、关闭refreshToken；版本化旧偏好，不能被演示设置切回。
- /workspace为登录固有首页，由后端统一拼入，其余按有效角色菜单过滤。
- BasicLayout用于目录，本地页面键如/system/users/index；后端不发任意JS地址。
- 页面键与构建产物、迁移种子构成契约；未知组件回退404并诊断，不白屏。
- 已知但未授权页面403，未知路径404；刷新先恢复身份/菜单再导航。
- 不做远程组件加载；外链作为可选增强且受控。
- 菜单不是API授权，按钮不是安全边界。

现有[组件回退](../../frontend/packages/utils/src/helpers/generate-routes-backend.ts#L75)继续复用。

## 文件与顺序

改造集中在[web-ele](../../frontend/apps/web-ele)：

1. api/request.ts、api/core/auth.ts、user.ts：HTTP、身份和ID类型。
2. store/auth.ts：会话清理和登录状态。
3. preferences.ts、api/core/menu.ts、router/access.ts、guard.ts、routes/core.ts：路由闭环。
4. 新system API、页面、权限帮助函数和测试。
5. scripts/deploy模板改为web-ele产物与API代理。

长期保留业务类型和交互；mapAuthInfoToUserInfo、路由转换属于Vben适配。表单、弹窗和Element Plus继续复用，暂不抽独立设计系统。

