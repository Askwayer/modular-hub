---
name: be-vita-crud-generation
description: 为 be-vita 新增或变更的仓库自建表生成标准 CRUD 骨架，并基于 vita-generator 输出做业务改造；不用于无需标准 CRUD、纯关联表或第三方内置表。
---

# be-vita CRUD 生成

## 规则入口

先读取 `AICoding/rules/be-vita-ai-codegen-rules.md` 的“3.8 标准 CRUD 生成约束”“6. 新建表默认字段 DDL 规范”“7.4 vita-generator”和“8. 交付检查清单”。不得绕过生成器从零手写风格不一致的标准骨架。

## 工作流

1. 检查工作树、目标表、目标模块、现有同类 CRUD、生成器配置和 `vita-generator/src/main/resources/templates/`，避免覆盖或混入用户已有修改。
2. 确认任务确实需要标准 Entity、DTO、VO、Mapper、Service、Controller 或 Model 骨架。纯关联表、第三方组件内置表和明确不需要标准 CRUD 的场景不触发生成流程。
3. 涉及 DDL 时先读取 `AICoding/skills/be-vita-database-design/SKILL.md`；涉及 Java 输出及业务改造时读取 `AICoding/skills/be-vita-java-conventions/SKILL.md`。
4. 在仓库根目录按现有生成器配置执行：

   ```bash
   mvn -pl vita-generator -DskipTests exec:java -Dexec.mainClass=com.vita.config.CodeGenerator
   ```

5. 将模板输出作为起点，逐项审查目标路径、包名、命名、统一返回、分页、Mapper 查询、事务和异常处理，再进行必要的业务补充、裁剪或重构。
6. 核对生成前后的差异，不接受无关文件、盲目覆盖、占位内容或模板之外的第二套目录与基础对象。
7. 运行受影响模块的编译和测试，并同步主规则要求的 SQL 与中央文档。

## 交付要求

说明生成目标、模板后的业务改造、受影响文件和验证结果。若因环境原因未实际执行生成器、编译或测试，必须明确说明，不能把手写近似骨架描述为模板生成结果。
