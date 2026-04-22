package com.vita.poi.excel.config;

import com.vita.poi.excel.property.PoiExcelProperty;
import com.vita.poi.excel.service.ExcelExportService;
import com.vita.poi.excel.service.ExcelFillService;
import com.vita.poi.excel.service.ExcelImportService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

class PoiExcelAutoConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(PoiExcelAutoConfiguration.class));

    @Test
    void shouldLoadBeansWhenEnabled() {
        contextRunner
                .withPropertyValues("vita.poi.excel.enabled=true")
                .run(context -> {
                    assertThat(context).hasSingleBean(PoiExcelProperty.class);
                    assertThat(context).hasSingleBean(ExcelImportService.class);
                    assertThat(context).hasSingleBean(ExcelExportService.class);
                    assertThat(context).hasSingleBean(ExcelFillService.class);
                });
    }

    @Test
    void shouldSkipBeansWhenDisabled() {
        contextRunner
                .withPropertyValues("vita.poi.excel.enabled=false")
                .run(context -> {
                    assertThat(context).doesNotHaveBean(ExcelImportService.class);
                    assertThat(context).doesNotHaveBean(ExcelExportService.class);
                    assertThat(context).doesNotHaveBean(ExcelFillService.class);
                });
    }

    @Test
    void shouldRespectCustomImportServiceBean() {
        ExcelImportService customService = new ExcelImportService() {
            @Override
            public <T> com.vita.poi.excel.model.result.ExcelImportResult importExcel(
                    com.vita.poi.excel.model.request.ExcelImportRequest<T> request,
                    com.vita.poi.excel.processor.ExcelImportProcessor<T> processor) {
                return new com.vita.poi.excel.model.result.ExcelImportResult();
            }
        };

        contextRunner
                .withPropertyValues("vita.poi.excel.enabled=true")
                .withBean(ExcelImportService.class, () -> customService)
                .run(context -> {
                    assertThat(context.getBean(ExcelImportService.class)).isSameAs(customService);
                    assertThat(context).hasSingleBean(ExcelExportService.class);
                    assertThat(context).hasSingleBean(ExcelFillService.class);
                });
    }
}
