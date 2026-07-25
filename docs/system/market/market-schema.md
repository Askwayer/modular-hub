# A股行情数据大屏表结构设计

## 1. 实施范围

V1 已落地行情领域持久化、AKTools 固定接口适配、Redis 快照降级、定时采集、Controller、菜单和权限。
三张表不对外暴露通用 CRUD，所有写操作均由行情领域 Service 按业务规则执行。

- 初始化脚本：`sql/init/market.sql`
- 幂等升级脚本：`sql/upgrade/market_upgrade.sql`
- 初始化脚本会删除并重建三张行情表，只用于新环境或明确接受数据清空的场景。
- 升级脚本对行情表仅执行 `CREATE TABLE IF NOT EXISTS`，不会删除或覆盖已有行情数据；菜单、权限和
  角色授权按业务编码使用 `NOT EXISTS` 幂等补齐。

行情数据在 V1 中属于系统级数据。三张表仍保留仓库标准 `tenant_id` 和审计后缀，但业务唯一约束不包含
租户维度。

## 2. 股票字典

`market_stock` 保存股票基础信息和资金监控配置。

| 字段 | 说明 |
| --- | --- |
| `stock_code` | 股票编号，全局唯一 |
| `stock_name` | 股票名称，字典同步时按编号更新 |
| `market` | `SH`、`SZ`、`BJ` |
| `listing_status` | `LISTED`、`SUSPENDED`、`DELISTED` |
| `monitor_enabled` | 是否启用个股资金监控 |
| `enabled_time` | 最近一次启用时间，重新启用时更新 |
| `display_order` | 启用股票展示顺序 |
| `source_update_time` | 数据源提供的最近更新时间 |

股票字典不通过物理删除处理退市数据。同步逻辑按编号更新名称和状态，并在明确退市时关闭监控；
暂停上市保留启用配置，但采集服务不会写入无效资金点。

## 3. 个股资金采样点

`market_stock_fund_point` 保存启用股票的40秒资金采样。

- `sample_bucket` 是按上海时区对齐后的40秒桶起始时间。
- `(stock_code, sample_bucket)` 唯一，作为重复调度和服务重启时的幂等边界。
- 流入额、流出额、净额和成交额统一使用人民币元；流出额保持正数。
- 仅完整有效的采样点允许落库，因此行情数值字段不设置零默认值。
- `(trade_date, stock_code, sample_bucket)` 索引用于日内曲线恢复和30天数据清理。

## 4. 市场日汇总

`market_daily_summary` 每个交易日最多一条记录，保存：

- 沪市、深市和北交所成交额；
- 上涨、下跌、平盘、涨停和跌停家数；
- 主力净流入；
- 最后有效行情时间和收盘定稿状态。

统计值允许为 `NULL`，表示数据源未提供或当前刷新未取得有效值，不得将未知数据转换为零。
`is_finalized=1` 表示当日15:00后的收盘定稿记录。

## 5. 公共字段

三张表统一采用：

```text
id -> tenant_id -> 业务字段 -> create_time -> update_time -> is_deleted
-> create_by_id -> create_by -> update_by_id -> update_by -> version -> remark
```

主键由 be-vita/MyBatis-Plus 统一 ID 策略生成，DDL 不使用自增。当前表结构不建立外键，跨表合法性由
行情 Service 负责校验。行情查询保持单表访问，不在 `vita-service` 使用跨表查询。

## 6. 运行时边界

- `vita-scheduler` 固定延迟40秒触发刷新，交易时钟仅在09:30–11:30、13:00–15:00采集。
- 午休只维护交易状态；15:00后通过 Redis 标记执行一次收盘定稿。
- 各模块使用受限线程池并行刷新，失败只更新模块状态，不覆盖最近成功快照。
- 股票启停和排序使用 Redisson 锁及数据库事务，最多启用10只正常上市股票。
- Redis 快照默认保留35天，MySQL 个股资金点默认保留30天。
- 初始化脚本是破坏性脚本；已有环境只能使用幂等升级脚本。

完整接口、缓存和配置说明参见 `docs/system/market/market-v1.md`。
