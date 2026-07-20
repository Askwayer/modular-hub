package com.vita.config;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @BelongsProject: be-vita
 * @BelongsPackage: com.vita.config
 * @Author: znk
 * @CreateTime: 2026-07-20
 * @Description: 工作流运行时代码、JOIN 和数据库边界回归测试
 * @Version: 1.0
 */
class WorkflowRuntimeBoundaryTest {

    private static final List<String> ENGINE_ENTITY_NAMES = List.of(
            "FlowDefinition", "FlowNode", "FlowSkip", "FlowInstance",
            "FlowTask", "FlowHisTask", "FlowUser", "FlowForm"
    );

    @Test
    void generatedEngineCrudShouldBeRemovedAndBlocked() throws IOException {
        Path repository = resolveRepositoryRoot();
        Path workflowJava = repository.resolve("vita-workflow/src/main/java/com/vita/workflow");
        for (String entityName : ENGINE_ENTITY_NAMES) {
            assertThat(workflowJava.resolve("controller/" + entityName + "Controller.java")).doesNotExist();
            assertThat(workflowJava.resolve("flow/entity/" + entityName + ".java")).doesNotExist();
            assertThat(workflowJava.resolve("flow/service/I" + entityName + "Service.java")).doesNotExist();
            assertThat(workflowJava.resolve("flow/mapper/" + entityName + "Mapper.java")).doesNotExist();
        }

        String generator = Files.readString(repository.resolve(
                "vita-generator/src/main/java/com/vita/config/CodeGenerator.java"));
        assertThat(generator).contains("private static final String[] TABLE_NAMES = {\"workflow_category\"}");
        assertThat(generator).contains("WARM_FLOW_ENGINE_TABLES", "validateGeneratedTables()");
    }

    @Test
    void joinShouldOnlyExistInWorkflowReadOnlyMapper() throws IOException {
        Path repository = resolveRepositoryRoot();
        try (var paths = Files.walk(repository)) {
            List<Path> joinFiles = paths
                    .filter(Files::isRegularFile)
                    .filter(path -> path.toString().endsWith(".xml") || path.toString().endsWith(".sql"))
                    .filter(path -> !path.toString().contains("/target/"))
                    .filter(this::containsJoin)
                    .toList();

            assertThat(joinFiles).allMatch(path -> repository.relativize(path).toString()
                    .equals("vita-workflow/src/main/resources/mapper/workflow/WorkflowRuntimeQueryMapper.xml"));
        }

        String mapperXml = Files.readString(repository.resolve(
                "vita-workflow/src/main/resources/mapper/workflow/WorkflowRuntimeQueryMapper.xml"));
        assertThat(mapperXml.toLowerCase(Locale.ROOT))
                .contains(" join ")
                .doesNotContain("<insert", "<update", "<delete");
    }

    @Test
    void workflowSqlShouldPreserveEngineIndexesAndOnlyUpgradePermissions() throws IOException {
        Path repository = resolveRepositoryRoot();
        String initSql = Files.readString(repository.resolve("sql/init/workflow.sql"));
        String upgradeSql = Files.readString(repository.resolve(
                "sql/upgrade/20260720_workflow_runtime_phase2_upgrade.sql"));

        assertThat(initSql).doesNotContain(
                "uk_flow_instance_business_id",
                "idx_flow_instance_create_by_update",
                "idx_flow_task_instance",
                "idx_flow_his_task_approver_update"
        );
        assertThat(upgradeSql)
                .doesNotContain("ALTER TABLE", "information_schema.statistics", "SIGNAL SQLSTATE")
                .contains(
                "system:workflow:definition:view",
                "system:workflow:definition:manage",
                "system:workflow:instance:view"
        );
    }

    private boolean containsJoin(Path path) {
        try {
            String normalized = Files.readString(path).toLowerCase(Locale.ROOT)
                    .replaceAll("\\s+", " ");
            return normalized.matches(".*\\b(left |right |inner |outer |cross )?join\\b.*");
        } catch (IOException ex) {
            throw new IllegalStateException("读取文件失败：" + path, ex);
        }
    }

    private Path resolveRepositoryRoot() {
        Path current = Path.of("").toAbsolutePath();
        while (current != null) {
            if (Files.isRegularFile(current.resolve("pom.xml"))
                    && Files.isDirectory(current.resolve("vita-workflow"))) {
                return current;
            }
            current = current.getParent();
        }
        throw new IllegalStateException("无法定位仓库根目录");
    }
}
