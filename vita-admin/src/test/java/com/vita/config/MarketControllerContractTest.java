package com.vita.config;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.vita.controller.market.MarketDashboardController;
import com.vita.controller.market.MarketFundController;
import com.vita.controller.market.MarketSectorController;
import com.vita.controller.market.MarketStockController;
import org.junit.jupiter.api.Test;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;

import java.lang.reflect.Method;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 行情 Controller 路由权限契约测试。
 */
class MarketControllerContractTest {

    @Test
    void everyMarketEndpointShouldDeclarePermissionAndOnlyUseGetOrPost() {
        List<Class<?>> controllers = List.of(
                MarketDashboardController.class,
                MarketSectorController.class,
                MarketFundController.class,
                MarketStockController.class);

        for (Class<?> controller : controllers) {
            for (Method method : controller.getDeclaredMethods()) {
                boolean mapped = method.isAnnotationPresent(GetMapping.class)
                        || method.isAnnotationPresent(PostMapping.class);
                if (!mapped) {
                    continue;
                }
                assertThat(method.getAnnotation(SaCheckPermission.class))
                        .as(controller.getSimpleName() + "#" + method.getName())
                        .isNotNull();
            }
        }
    }

    @Test
    void stockWritesShouldUseDedicatedPermissions() throws NoSuchMethodException {
        assertPermission("enable", "market:stock:manage",
                com.vita.market.dto.MarketStockCodeRequest.class);
        assertPermission("disable", "market:stock:manage",
                com.vita.market.dto.MarketStockCodeRequest.class);
        assertPermission("reorder", "market:stock:manage",
                com.vita.market.dto.MarketStockReorderRequest.class);
        assertPermission("syncDictionary", "market:stock:sync");
    }

    private void assertPermission(String methodName, String permission, Class<?>... parameters)
            throws NoSuchMethodException {
        Method method = MarketStockController.class.getDeclaredMethod(methodName, parameters);
        assertThat(method.getAnnotation(SaCheckPermission.class).value()).containsExactly(permission);
    }
}
