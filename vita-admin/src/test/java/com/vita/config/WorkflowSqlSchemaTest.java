package com.vita.config;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @BelongsProject: be-vita
 * @BelongsPackage: com.vita.config
 * @Author: znk
 * @CreateTime: 2026-07-14
 * @Description: Warm-Flow 数据库脚本回归测试
 * @Version: 1.0
 */
class WorkflowSqlSchemaTest {

    private static final Pattern FLOW_TABLE_PATTERN = Pattern.compile(
            "(?i)CREATE\\s+TABLE\\s+`(flow_[a-z_]+)`"
    );

    private static final Set<String> REQUIRED_FLOW_TABLES = Set.of(
            "flow_definition",
            "flow_node",
            "flow_skip",
            "flow_instance",
            "flow_task",
            "flow_his_task",
            "flow_user",
            "flow_form"
    );

    /**
     * 初始化脚本必须包含 Warm-Flow 1.8.8 运行所需表。
     *
     * @throws IOException 脚本读取异常
     */
    @Test
    void initScriptShouldContainAllWarmFlowTables() throws IOException {
        String sql = Files.readString(resolveRepositoryFile("sql/init/workflow.sql"));
        Matcher matcher = FLOW_TABLE_PATTERN.matcher(sql);
        Set<String> tableNames = new java.util.HashSet<>();
        while (matcher.find()) {
            tableNames.add(matcher.group(1).toLowerCase());
        }

        assertThat(tableNames).containsExactlyInAnyOrderElementsOf(REQUIRED_FLOW_TABLES);
    }

    /**
     * 兼容升级脚本必须幂等且不得删除已有流程数据。
     *
     * @throws IOException 脚本读取异常
     */
    @Test
    void upgradeScriptShouldBeIdempotentAndNonDestructive() throws IOException {
        String sql = Files.readString(resolveRepositoryFile(
                "sql/upgrade/20260714_warm_flow_1_8_8_schema_upgrade.sql"));
        String upperSql = sql.toUpperCase();

        assertThat(upperSql).contains("CREATE TABLE IF NOT EXISTS `FLOW_FORM`");
        assertThat(upperSql).doesNotContain("DROP TABLE", "TRUNCATE", "DELETE FROM");
    }

    private Path resolveRepositoryFile(String relativePath) {
        Path current = Path.of("").toAbsolutePath();
        while (current != null) {
            Path candidate = current.resolve(relativePath);
            if (Files.isRegularFile(candidate)) {
                return candidate;
            }
            current = current.getParent();
        }
        throw new IllegalStateException("无法定位仓库文件：" + relativePath);
    }
}
