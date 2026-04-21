# Repository Guidelines

## Project Structure & Module Organization
`be-vita` is a Maven multi-module backend repository. Core modules are `vita-common` (shared infrastructure and base models), `vita-service` (business services), `vita-admin` (admin app, `/admin/api`), `vita-aichat` (AI Chat app, `/aichat/api`), `vita-generator` (code generator), and `vita-starters` (profile-driven starter aggregator for `ai`, `captcha`, `file`, `i18n`, `poi`). Keep package roots under `com.vita.*` and preserve the main dependency chain: `vita-admin -> vita-service -> vita-common`, `vita-aichat -> vita-common`.

## Build, Test, and Development Commands
- `mvn clean compile`: compile the full repository.
- `mvn clean package -DskipTests`: package all modules quickly.
- `mvn test`: run the full test suite.
- `mvn -pl vita-admin -am spring-boot:run`: run the admin application.
- `mvn -pl vita-aichat -am spring-boot:run`: run the AI Chat application.
- `mvn -pl vita-service -am test`: run service-layer tests.
- `mvn -pl vita-generator -DskipTests exec:java -Dexec.mainClass=com.vita.config.CodeGenerator`: run code generation.
- `mvn -f vita-starters/pom.xml -Pstarter-captcha test`: test the captcha starter profile.

## Coding, Rules, and Naming
Use 4-space indentation, UTF-8, `PascalCase` for classes, `camelCase` for methods and fields, and `UPPER_SNAKE_CASE` for constants. Keep controller logic thin, business orchestration in services, and single-table SQL details in mappers. Detailed AI and code generation constraints live only in `AICoding/rules/be-vita-ai-codegen-rules.md`; `README.md`, `CLAUDE.md`, and this file are summaries, not parallel rule sources.

## Database and Documentation Sync
For new tables, follow the default DDL suffix unless the table is a pure association table: `tenant_id`, `create_time`, `update_time`, `is_deleted`, `create_by_id`, `create_by`, `update_by_id`, `update_by`, `version`, `remark`. Standard ordering is `id -> tenant_id -> business fields -> audit suffix`. `sys_user` is the representative sample; `pwd_update_date` is business-specific, not part of the global suffix.

Any change touching authentication, RBAC, logging, AI Chat, initialization SQL, or DDL conventions must update the relevant docs in `docs/system/`, `docs/aichat/`, `docs/starter/`, plus scripts in `sql/init/` or `sql/upgrade/` when applicable. Runtime logs are written under `logs/vita-admin/` and `logs/vita-aichat/`.
