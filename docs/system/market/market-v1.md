# A股行情数据大屏 V1

## 1. 模块边界

- `vita-service/com.vita.market`：AKTools 适配、交易日历、Redis 快照、刷新协调、股票管理和采样持久化。
- `vita-scheduler`：40秒采集、08:00交易日历和08:15股票字典任务。
- `vita-admin`：登录后的大屏读取和管理员配置接口。
- `vita-openapi`：不暴露行情匿名接口。
- `hot`：与行情领域独立，本次没有复用或修改。

外部数据只能通过 `AkToolsMarketClient` 的固定方法访问。请求参数不能指定函数名，也不能令 be-vita
绕过 AKTools 直连第三方行情站点。

## 2. 配置

```yaml
vita:
  market:
    aktools-base-url: ${AKTOOLS_BASE_URL:http://127.0.0.1:18001}
    connect-timeout-ms: 3000
    read-timeout-ms: 8000
    retry-times: 2
    snapshot-ttl-days: 35
    fund-point-retention-days: 30
    manual-refresh-interval-seconds: 30
    refresh-lock-lease-seconds: 35
```

AKTools 需提供通用 `/api/public/{function}` 接口，并补充稳定接口：

```text
GET /api/market/sector/snapshot?sectorType=industry|concept
```

该接口必须返回 `sectorCode/name/changePercent/turnover/marketCap/turnoverRate/upCount/downCount/leader/mainNetInflow`。
字段缺失、错误状态码、错误 JSON、空响应或数值解析失败均视为模块刷新失败。

## 3. 交易与降级

交易状态由 `Asia/Shanghai` 和 AKTools 交易日历共同计算：

- `PRE_OPEN`
- `TRADING`
- `LUNCH_BREAK`
- `CLOSED`
- `NON_TRADING_DAY`
- `UNKNOWN`

交易日历不可用时优先使用 Redis 历史日历；没有任何日历时返回 `UNKNOWN`，不根据周末以外的规则猜测
节假日。模块成功数据和刷新状态分开保存，失败时旧数据继续返回并标记 `STALE`；没有历史数据时返回
`ERROR` 或 `NO_DATA`。未知行情数值保持 `null`。

## 4. 接口与权限

所有接口位于 `/admin/api/market/**`。

| 范围 | 权限码 |
| --- | --- |
| 全部读取接口、异步手动刷新 | `market:dashboard:view` |
| 股票启用、停用、排序 | `market:stock:manage` |
| 股票字典同步 | `market:stock:sync` |

`COMMON_USER`、`SUB_ADMIN`、`SUPER_ADMIN` 均获得查看权限；写权限仅授予
`SUB_ADMIN` 和 `SUPER_ADMIN`。业务代码只检查权限码，不硬编码角色。

读取接口：

- `GET /market/dashboard/status`
- `GET /market/dashboard/indices`
- `GET /market/dashboard/summary`
- `GET /market/sector/heatmap`
- `GET /market/sector/topStocks`
- `GET /market/sector/rankings`
- `GET /market/sector/mutations`
- `GET /market/fund/trend`
- `GET /market/stock/dictionary/page`
- `GET /market/stock/monitor/page`

写接口：

- `POST /market/dashboard/refresh`
- `POST /market/stock/enable`
- `POST /market/stock/disable`
- `POST /market/stock/reorder`
- `POST /market/stock/dictionary/sync`

## 5. 数据规则

- 股票字典按编号更新名称和上市状态，不删除历史股票。
- 退市股票自动停止监控；暂停上市保留监控配置但不写资金点。
- 启用、停用和排序在 Redisson 锁与事务中完成。
- 排序请求必须完整包含全部启用股票，不能缺失、重复或混入未启用股票。
- 资金流出以正数存储和返回，净额单独返回。
- 采样点按上海时区40秒桶和股票编号唯一，不补零、不回填失败时段。
- 运行时查询保持单表访问；资金监控分页先查股票，再按股票读取曲线。

## 6. 部署顺序

1. 新环境执行 `sql/init/market.sql`，再执行 `sql/init/insert.sql`。
2. 已有环境仅执行 `sql/upgrade/market_upgrade.sql`。
3. 配置 `AKTOOLS_BASE_URL`，确认稳定板块接口已经部署。
4. 启动 Redis、MySQL、`vita-admin` 和 `vita-scheduler`。
5. 管理员执行一次股票字典同步，再启用不超过10只正常上市股票。

`sql/init/market.sql` 会清空三张行情表，禁止在已有行情数据的生产环境执行。
