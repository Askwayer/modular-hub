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
    aktools-base-url: http://127.0.0.1:8080
    connect-timeout-ms: 3000
    read-timeout-ms: 8000
    retry-times: 1
    snapshot-ttl-days: 35
    fund-point-retention-days: 30
    manual-refresh-interval-seconds: 30
    refresh-lock-lease-seconds: 35
```

AKTools 只需提供现有 `/api/public/{function}`，be-vita 不依赖任何定制行情接口。

固定函数白名单：

- 指数：`stock_zh_index_spot_sina`、`index_zh_a_hist_min_em`、`stock_zh_index_daily_tx`
- 市场：`stock_zh_a_spot_em`、`stock_market_fund_flow`、`stock_fund_flow_individual`
- 板块：`stock_board_industry_name_em`、`stock_board_industry_summary_ths`、
  `stock_board_concept_name_em`、`stock_sector_fund_flow_rank`、行业/概念成分接口、
  `stock_board_change_em`
- 基础数据：`tool_trade_date_hist_sina` 和现有沪深北股票名单、退市名单接口

错误状态码、错误 JSON 和空响应视为对应模块刷新失败。单项字段不存在、`--`、空字符串和非法数值
标准化为 `null`，不能补零或使用请求时间伪造行情时间。

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

Redis 使用独立 Jackson 配置保存行情快照和交易日历，标准支持 `LocalDate`、`LocalDateTime`、
Java record 和日期集合。`lastAttemptAt/lastSuccessAt` 是 be-vita 刷新时间，
`sourceDataTime` 只能来自数据源。

## 4. 接口与权限

所有接口位于 `/admin/api/market/**`。

行情父菜单 `/market` 的动态路由组件固定为 `MarketLayout`，由前端渲染独立暗色
行情工作台；不得使用管理端 `Layout`，避免侧栏、面包屑和浅色主题进入行情视图。
已有环境执行 `sql/upgrade/market_upgrade.sql` 时会幂等修正该菜单元数据。

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

板块查询枚举统一使用小写，`be-vita` 不再做大小写转换：

- `sectorType`：`industry`、`concept`
- `areaMetric`：`turnover`、`marketcap`
- `direction`：`all`、`up`、`down`
- `metric`：`changepercent`、`mainnetinflow`、`turnover`
- `period`：`today`、`5d`、`10d`

`stock_sector_fund_flow_rank` 的 `indicator` 和 `sector_type` 也使用上述小写值调用
AKTools。大写或中英文混用参数视为契约错误并返回 400。

写接口：

- `POST /market/dashboard/refresh`
- `POST /market/stock/enable`
- `POST /market/stock/disable`
- `POST /market/stock/reorder`
- `POST /market/stock/dictionary/sync`

## 5. 数据规则

- 固定指数按以下顺序返回9项：上证指数、深证成指、创业板指、科创50、科创综指、
  中证A500、沪深300、上证50、深证100。北证50和 `899050` 不得进入指数接口。
- 移除北证50不影响北交所股票；北交所继续参与沪深京总览、字典和个股资金监控。
- 指数实时值以新浪列表为主，东方财富分钟线补充曲线和源时间，腾讯日线只用于昨收、历史对照和
  降级。使用腾讯卡片数据时必须标记 `STALE`。
- 分钟线按分钟去重，同一分钟保留最后有效点；固定指数缺失时仍保留空卡位。
- 行业热力图支持成交额和总市值，概念热力图仅支持总市值。板块只按代码或标准化后的完整名称
  精确关联，禁止模糊匹配。
- 市场资金趋势返回主力、超大单、大单、中单和小单五类净流入，并按所选固定指数叠加日线。
- 金额统一为人民币元，比例为不带 `%` 的数值，股票和指数代码为6位字符串。
- 股票字典按编号更新名称和上市状态，不删除历史股票。
- 退市股票自动停止监控；暂停上市保留监控配置但不写资金点。
- 启用、停用和排序在 Redisson 锁与事务中完成。
- 排序请求必须完整包含全部启用股票，不能缺失、重复或混入未启用股票。
- 资金流出以正数存储和返回，净额单独返回。
- 全市场即时资金保存在 Redis 中供板块关联；MySQL 只保存当前启用且正常上市股票的资金点。
- 采样点按上海时区40秒桶和股票编号唯一，不补零、不回填失败时段。
- 运行时查询保持单表访问；资金监控分页先查股票，再按股票读取曲线。

## 6. 部署顺序

1. 新环境执行 `sql/init/market.sql`，再执行 `sql/init/insert.sql`。
2. 已有环境仅执行 `sql/upgrade/market_upgrade.sql`。
3. 配置 `AKTOOLS_BASE_URL`，确认上述公开函数可以通过 `/api/public/{function}` 访问。
4. 启动 Redis、MySQL、`vita-admin` 和 `vita-scheduler`。
5. 管理员执行一次股票字典同步，再启用不超过10只正常上市股票。

`sql/init/market.sql` 会清空三张行情表，禁止在已有行情数据的生产环境执行。
