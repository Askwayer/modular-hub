package com.vita.config;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 行情初始化与升级 SQL 边界测试。
 */
class MarketSqlContractTest {

    @Test
    void upgradeShouldBeIdempotentAndShouldNotUseJoin() throws IOException {
        String sql = Files.readString(repositoryRoot().resolve("sql/upgrade/market_upgrade.sql"));
        String upper = sql.toUpperCase(Locale.ROOT);

        assertThat(upper).contains("CREATE TABLE IF NOT EXISTS `MARKET_STOCK`");
        assertThat(upper).contains("MARKET:DASHBOARD:VIEW");
        assertThat(upper).contains("MARKET:STOCK:MANAGE");
        assertThat(upper).contains("MARKET:STOCK:SYNC");
        assertThat(upper).doesNotContain(" JOIN ");
        assertThat(upper).doesNotContain("DROP TABLE");
    }

    @Test
    void initShouldContainAllThreeMarketTables() throws IOException {
        String sql = Files.readString(repositoryRoot().resolve("sql/init/market.sql"));

        assertThat(sql).contains(
                "CREATE TABLE `market_stock`",
                "CREATE TABLE `market_stock_fund_point`",
                "CREATE TABLE `market_daily_summary`");
    }

    @Test
    void marketMenuShouldAlwaysUseImmersiveLayout() throws IOException {
        String initSql = Files.readString(repositoryRoot().resolve("sql/init/insert.sql"));
        String upgradeSql = Files.readString(repositoryRoot().resolve("sql/upgrade/market_upgrade.sql"));

        assertThat(initSql)
                .contains("'Market', '/market', 'MarketLayout'")
                .doesNotContain("'Market', '/market', 'Layout'");
        assertThat(upgradeSql)
                .contains("'Market', '/market', 'MarketLayout'")
                .contains("SET `component_path` = 'MarketLayout'")
                .doesNotContain("'Market', '/market', 'Layout'");
    }

    private Path repositoryRoot() {
        Path current = Path.of("").toAbsolutePath();
        while (current != null) {
            if (Files.isDirectory(current.resolve("vita-admin"))
                    && Files.isRegularFile(current.resolve("pom.xml"))) {
                return current;
            }
            current = current.getParent();
        }
        throw new IllegalStateException("无法定位仓库根目录");
    }
}
