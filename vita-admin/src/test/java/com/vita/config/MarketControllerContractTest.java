package com.vita.config;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.vita.controller.market.MarketDashboardController;
import com.vita.controller.market.MarketFundController;
import com.vita.controller.market.MarketSectorController;
import com.vita.controller.market.MarketStockController;
import org.junit.jupiter.api.Test;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
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

    @Test
    void everyMarketRequestParamShouldDeclareExplicitName() {
        for (Class<?> controller : marketControllers()) {
            for (Method method : controller.getDeclaredMethods()) {
                for (Parameter parameter : method.getParameters()) {
                    RequestParam requestParam = parameter.getAnnotation(RequestParam.class);
                    if (requestParam == null) {
                        continue;
                    }
                    assertThat(requestParam.name())
                            .as(controller.getSimpleName() + "#" + method.getName())
                            .isNotBlank();
                }
            }
        }
    }

    @Test
    void integerRequestParamsShouldNotDependOnCompilerParameterMetadata() throws NoSuchMethodException {
        assertRequestParamName(MarketFundController.class, "trend", 0, "days",
                Integer.class, String.class);
        assertRequestParamName(MarketStockController.class, "monitorPage", 1, "pageNum",
                String.class, Integer.class);
    }

    @Test
    void marketDefaultsShouldUseShanghaiIndexAndDynamicHeatmapMetric() throws NoSuchMethodException {
        Method trend = MarketFundController.class.getDeclaredMethod("trend", Integer.class, String.class);
        RequestParam indexCode = trend.getParameters()[1].getAnnotation(RequestParam.class);
        assertThat(indexCode.defaultValue()).isEqualTo("000001");

        Method heatmap = MarketSectorController.class.getDeclaredMethod(
                "heatmap", String.class, String.class, String.class, String.class);
        RequestParam areaMetric = heatmap.getParameters()[1].getAnnotation(RequestParam.class);
        assertThat(areaMetric.required()).isFalse();
    }

    private void assertPermission(String methodName, String permission, Class<?>... parameters)
            throws NoSuchMethodException {
        Method method = MarketStockController.class.getDeclaredMethod(methodName, parameters);
        assertThat(method.getAnnotation(SaCheckPermission.class).value()).containsExactly(permission);
    }

    private void assertRequestParamName(Class<?> controller, String methodName, int parameterIndex,
                                        String expectedName, Class<?>... parameterTypes)
            throws NoSuchMethodException {
        Method method = controller.getDeclaredMethod(methodName, parameterTypes);
        RequestParam requestParam = method.getParameters()[parameterIndex].getAnnotation(RequestParam.class);
        assertThat(requestParam).isNotNull();
        assertThat(requestParam.name()).isEqualTo(expectedName);
    }

    private List<Class<?>> marketControllers() {
        return List.of(
                MarketDashboardController.class,
                MarketSectorController.class,
                MarketFundController.class,
                MarketStockController.class);
    }
}
