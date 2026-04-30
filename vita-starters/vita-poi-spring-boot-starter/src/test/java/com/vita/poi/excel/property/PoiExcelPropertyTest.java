package com.vita.poi.excel.property;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PoiExcelPropertyTest {

    @Test
    void shouldUseDefaultValues() {
        PoiExcelProperty property = new PoiExcelProperty();

        property.normalizeAndValidate();

        assertThat(property.isEnabled()).isTrue();
        assertThat(property.getDefaultHeadRowNumber()).isEqualTo(1);
        assertThat(property.getDefaultImportBatchSize()).isEqualTo(100);
        assertThat(property.getDefaultExportPageSize()).isEqualTo(5000);
        assertThat(property.getMaxErrorRows()).isEqualTo(200);
        assertThat(property.isTrimCellValue()).isTrue();
        assertThat(property.isIgnoreEmptyRow()).isTrue();
        assertThat(property.isAutoCloseStream()).isFalse();
        assertThat(property.isInMemory()).isFalse();
        assertThat(property.isUseDefaultStyle()).isTrue();
        assertThat(property.getDefaultSheetName()).isEqualTo("Sheet1");
    }

    @Test
    void shouldFailWhenNumericConfigInvalid() {
        PoiExcelProperty property = new PoiExcelProperty();
        property.setDefaultImportBatchSize(0);

        assertThatThrownBy(property::normalizeAndValidate)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("default-import-batch-size");
    }

    @Test
    void shouldAllowDisabledPropertyAndFallbackSheetName() {
        PoiExcelProperty property = new PoiExcelProperty();
        property.setEnabled(false);
        property.setDefaultSheetName("   ");

        property.normalizeAndValidate();

        assertThat(property.isEnabled()).isFalse();
        assertThat(property.getDefaultSheetName()).isEqualTo("Sheet1");
    }
}
