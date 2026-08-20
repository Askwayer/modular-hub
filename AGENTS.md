# Repository Guidelines

## Project Structure
`be-vita` is a Java 17 Maven multi-module backend. `vita-common` contains shared infrastructure and base models; `vita-service` owns system business services; `vita-workflow` owns workflow contracts, domain logic, and Warm-Flow integration. `vita-admin`, `vita-openapi`, and `vita-scheduler` are Spring Boot entry modules. `vita-generator` provides CRUD generation, and `vita-starters` aggregates the `captcha`, `file`, `i18n`, and `poi` starters.

Preserve these dependency chains: `vita-admin -> vita-service -> vita-common`, `vita-admin -> vita-workflow -> vita-service -> vita-common`, `vita-openapi -> vita-service -> vita-common`, and `vita-scheduler -> vita-service -> vita-common`. Do not move workflow contracts or implementations into `vita-common` or `vita-service`.

## Build and Development Commands
- `mvn clean compile`: compile all modules.
- `mvn test`: run the full test suite.
- `mvn clean package -DskipTests`: package without tests.
- `mvn -pl vita-admin -am spring-boot:run`: run the admin app.
- `mvn -pl vita-openapi -am spring-boot:run`: run the public API app.
- `mvn -pl vita-scheduler -am spring-boot:run`: run the scheduler app.
- `mvn -pl vita-generator -DskipTests exec:java -Dexec.mainClass=com.vita.config.CodeGenerator`: generate CRUD scaffolding.
- `./tools/codegraph/codegraph.sh init`: initialize or synchronize the optional root CodeGraph index.

## Coding and Verification
Use UTF-8, 4-space indentation, `PascalCase` classes, `camelCase` members, and `UPPER_SNAKE_CASE` constants. Keep controllers thin, business orchestration in services, and single-table persistence in mappers. New standard CRUD must start from `vita-generator`. Add concise Chinese comments only for key rules, transactions, state changes, concurrency, and integrations.

Before implementation, inspect the target directory, technology stack, `git status --short --branch`, local conventions, related docs, Git history, and current code. Start with exact `rg` searches, read only needed ranges, and bound long logs, SQL, Mapper XML, build output, and diffs. Compilation, tests, and source code remain authoritative.

New non-association tables use `id -> tenant_id -> business fields -> audit suffix`, including `create_time`, `update_time`, `is_deleted`, creator/updater fields, `version`, and `remark`. Detailed rules live only in `AICoding/rules/be-vita-ai-codegen-rules.md`.

## Documentation and CodeGraph
Use `docs/README.md` to navigate centralized requirements, architecture, and development guides. Reference existing sections instead of copying them. Do not place business code in `tools/` or detailed designs in Maven modules.

All repository CodeGraph operations must use `./tools/codegraph/codegraph.sh`; related scripts may exist only under `tools/codegraph/`. Use exact symbols and verify dynamic calls with source reads. Never delete or bypass this mechanism, create module-level aliases, or commit `.codegraph/` and personal MCP configuration. Setup and rollback are documented in `docs/development/codegraph-integration.md`.
