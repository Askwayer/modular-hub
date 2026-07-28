package com.vita.market.support;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class AkToolsFunctionContractTest {

    @Test
    void marketClientShouldOnlyUsePublicFunctionsAndExcludeRemovedEndpoints() throws IOException {
        Path source = repositoryRoot().resolve(
                "vita-service/src/main/java/com/vita/market/support/AkToolsMarketClient.java");
        String code = Files.readString(source);

        assertThat(code).doesNotContain("/api/market/");
        assertThat(code).doesNotContain("\"index_zh_a_spot\"");
        assertThat(code).doesNotContain("\"index_zh_a_hist\"");
        assertThat(code).contains(
                "stock_zh_index_spot_sina",
                "index_zh_a_hist_min_em",
                "stock_zh_index_daily_tx",
                "stock_market_fund_flow");
    }

    private Path repositoryRoot() {
        Path current = Path.of("").toAbsolutePath();
        while (current != null) {
            if (Files.isDirectory(current.resolve("vita-service"))
                    && Files.isRegularFile(current.resolve("pom.xml"))) {
                return current;
            }
            current = current.getParent();
        }
        throw new IllegalStateException("无法定位仓库根目录");
    }
}
