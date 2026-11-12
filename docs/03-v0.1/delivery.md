# V0.1 环境、部署与验收

[设计导航](README.md)

## 环境与版本

- JDK17、Boot3.5.0、pnpm10.33.0沿用仓库。
- Node选择仓库允许的22.x且不低于22.18.0，补丁验证后固定。
- Maven仓库未锁，Boot插件下限3.6.3；企业可用版本验证后固定。
- MySQL8.x、Redis具体服务版本待确认；Flyway和Compose v2实施时锁定，不盲用最新版。
- 本轮归档没有检查实际安装版本，也没有执行任何构建或测试。

## 配置和网络

主配置只有非敏感公共默认值，不默认prod。local使用忽略文件/外部注入；dev/test隔离Schema和Redis命名空间；prod显式选择且缺秘密失败。运行账号不具DDL权，迁移账号独立。VITE_*按公开配置处理。

```text
本地：浏览器 → Vite /api → admin /admin/api → MySQL + Redis
部署：浏览器 → Nginx HTTPS
                    ├─ /     → web-ele
                    └─ /api/ → admin /admin/api/
```

API location优先，API404不回退SPA HTML。用户不直连数据库/Redis或后台端口。代理清洗转发头并定义可信来源。workflow/file默认关闭，测试端点不可用而不只是菜单隐藏。

local/dev/test资源不足可逻辑隔离；prod必须独立账号和数据。V0.1不启动其他三个后端入口。

## 工程交付

流程：工具链校验→单测/类型/无修复静态检查→MySQL/Redis集成→前端E2E→admin/web-ele打包→版本/校验和→人工发布→备份迁移→健康→登录冒烟。

现有lint和Lefthook含自动修复，不能当成只读检查直接执行。CI采用明确不修复的命令，先记录基线。不把全仓模板历史问题混进V0.1修复。

允许手工发布，但步骤可重复。live反映进程，ready检查必要依赖，故障503且公共响应不泄露细节。日志有请求标识、轮转和保留限制。备份包括数据库/版本记录/部署配置，秘密单独保护。至少恢复到隔离环境一次。镜像/Jar回滚不等于Schema回滚，单实例升级允许短暂中断。

## 验收矩阵

| ID | 场景 | 预期与层级 |
| --- | --- | --- |
| A01 | 正确密码验证码 | 200与Token，日志无秘密；API集成 |
| A02 | 错密码/不存在/禁用用户 | 统一失败，不枚举账号；API集成 |
| A03 | 无/非法/过期Token | 401且无副作用；API集成 |
| A04 | 同账号两Token退出一个 | 只当前失效；真实Redis |
| A05 | 改密/重置 | 全部旧Token401，新登录成功；数据库+Redis |
| A06 | 禁用再启用 | 旧Token不复活；集成 |
| A07 | 提交撤权或禁用角色 | 下一新请求403，无缓存TTL等待；集成 |
| A08 | 禁用角色仍有关联 | 新登录也无该角色权限/菜单；集成 |
| A09 | 普通用户直调IAM写 | 403、库不变；集成 |
| A10 | 授权撤权至少三轮 | 无重复和唯一索引错误；MySQL |
| A11 | 整体授权含非法ID | 全部回滚，无假成功审计；事务测试 |
| A12 | 来源/账号阈值 | 429且不影响其他账号；Redis |
| A13 | 空字段/状态/JSON非法 | 400、success=false、code非200；MockMvc |
| A14 | 1001/900/未知异常 | HTTP映射正确且脱敏；MockMvc |
| A15 | 大整数ID往返 | 字符串完全一致；契约测试 |
| A16 | 刷新深层路由 | 恢复身份和菜单后导航；E2E |
| A17 | 无权页/未知路由/组件缺失 | 403/404/回退，不白屏；E2E |
| A18 | 并发401 | 一次清理跳转，无递归；前端单测+E2E |
| A19 | A退出后B登录 | 无A菜单/按钮/路由残留；E2E |
| A20 | 菜单与API权限交叉组合 | 各自独立生效；E2E+API |
| A21 | 部门/菜单挂自己后代 | 拒绝，库无环；Service+MySQL |
| A22 | 空库/重复迁移/脚本篡改 | 成功/无重复/校验失败；迁移集成 |
| A23 | 旧库无显式baseline | 不自动接管破坏；迁移集成 |
| A24 | 缺必要配置冷启动 | 明确失败，不回显秘密；启动测试 |
| A25 | Redis/数据库不可用 | ready503，不沿用旧权限；集成 |
| A26 | IAM成功/回滚 | 脱敏成功审计/无假成功；MySQL |
| A27 | workflow/file关闭 | 对应功能不可用且无额外外部依赖；启动 |
| A28 | 打包部署恢复 | admin/web-ele正确、恢复后可登录；演练 |
| A29 | Java/Python Manifest | 可表达且无Java类/平台表/秘密；契约评审 |
| A30 | 依赖边界 | 独立应用不依赖service/Mapper，演示无业务表；静态检查 |

A29/A30只是预设计验证，不是独立服务SSO运行验证。

## 后续建议命令及影响

以下均未执行，后续实施时按任务授权：

| 命令 | 影响 |
| --- | --- |
| mvn -o -pl vita-common,vita-service -am test | 离线测试；写target/报告，缓存不足会失败 |
| mvn -pl vita-admin -am test | 可能下载依赖并写产物，执行测试 |
| mvn -pl vita-admin -am package -DskipTests | 下载/编译/打包，不证明测试通过 |
| pnpm --filter @vben/web-ele typecheck | 需现有依赖，可能写缓存 |
| pnpm test:unit | 执行测试，可能写缓存 |
| pnpm build:ele | 写dist、Turbo缓存和压缩产物 |

数据库/Redis集成测试需独立环境，不能把初始化脚本对真实库执行。参考：[Maven JUnit Platform](https://maven.apache.org/surefire/maven-surefire-plugin/examples/junit-platform.html)、[Boot3.5 Maven要求](https://docs.spring.io/spring-boot/3.5/maven-plugin/)。

