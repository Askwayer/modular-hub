# be-vita AI Skills

本目录存放工具中立的任务工作流。所有 Skill 使用标准 `SKILL.md` 目录结构，但当前不依赖 Codex、Claude 或其他代理的原生自动发现机制。

## 权威与优先级

- `AICoding/rules/be-vita-ai-codegen-rules.md` 是唯一完整、最高优先级的规则源。
- Skill 只负责执行顺序、上下文导航和检查步骤，不重新定义数据库、分层、命名或事务规则。
- Skill 与主规则冲突、Skill 缺失或未加载时，必须以主规则为准，且不得放宽任何硬约束。

## Skill 目录

| Skill | 适用任务 | 组合关系 |
| --- | --- | --- |
| `be-vita-database-design` | 建表、改表、初始化或升级 SQL、Entity 映射、DDL 审查 | 需要标准 CRUD 时继续读取 CRUD Skill |
| `be-vita-java-conventions` | Java 新增、修改、重构和代码审查 | 涉及表结构时同时读取数据库 Skill |
| `be-vita-crud-generation` | 新增或变更业务表后生成标准 CRUD | 按任务同时读取数据库与 Java Skill |

## 使用方式

开始任务前先读取主规则，再按上表读取相关 `SKILL.md`。复合任务必须组合使用对应 Skill；与数据库、Java 或标准 CRUD 无关的任务无需加载这些 Skill。
