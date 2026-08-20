---
name: be-vita-database-design
description: 处理 be-vita 的建表、改表、初始化或升级 SQL、Entity 字段映射和 DDL 审查；不用于不涉及数据库结构的普通代码修改。
---

# be-vita 数据库设计

## 规则入口

先读取 `AICoding/rules/be-vita-ai-codegen-rules.md` 的“3.5 持久化与 SQL”“3.7 文档联动”“6. 新建表默认字段 DDL 规范”和“8. 交付检查清单”。本 Skill 不替代或重复这些硬约束。

## 工作流

1. 检查 `git status --short --branch`，再精确定位目标 SQL、Entity、Mapper、相关设计文档和历史实现；只读取当前判断所需片段。
2. 将目标表归类为仓库自建表、纯关联表或第三方组件内置表。仅在主规则允许时使用例外，并在 SQL 或设计文档中说明原因。
3. 对照主规则检查字段布局、公共字段、类型、默认值、注释、字符集和逻辑删除语义；业务专属字段不得混入公共审计后缀。
4. 根据现有仓库约定同步初始化 SQL、升级 SQL、Entity 映射及中央文档。不得只修改其中一个入口而留下冲突定义。
5. 使用限定范围的搜索和差异检查核对重复 DDL、字段顺序、默认值及文档引用；SQL 或源码事实与文档冲突时必须明确指出并复核。

## 组合与交付

- 需要标准 CRUD 时，继续读取 `AICoding/skills/be-vita-crud-generation/SKILL.md`。
- 同时修改 Java 持久化代码时，继续读取 `AICoding/skills/be-vita-java-conventions/SKILL.md`。
- 交付时说明变更的 SQL 入口、文档联动、适用的表类型及使用的例外；未执行数据库验证时必须明确说明。
