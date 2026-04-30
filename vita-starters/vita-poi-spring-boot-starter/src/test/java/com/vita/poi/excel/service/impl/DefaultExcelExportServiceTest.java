package com.vita.poi.excel.service.impl;

import com.alibaba.excel.annotation.ExcelProperty;
import com.alibaba.excel.write.handler.SheetWriteHandler;
import com.vita.core.exception.ServiceException;
import com.vita.poi.excel.model.request.ExcelExportRequest;
import com.vita.poi.excel.model.request.ExcelPagedExportRequest;
import com.vita.poi.excel.property.PoiExcelProperty;
import com.vita.poi.excel.support.ExcelRequestValidator;
import com.vita.poi.excel.support.ExcelTestSupport;
import com.vita.poi.excel.support.ExcelWriteSupport;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DefaultExcelExportServiceTest {

    private DefaultExcelExportService service;
    private PoiExcelProperty property;

    @BeforeEach
    void setUp() {
        property = ExcelTestSupport.defaultProperty();
        service = new DefaultExcelExportService(property, new ExcelRequestValidator(), new ExcelWriteSupport());
    }

    @Test
    void shouldExportAndReadBackData() {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        service.export(new ExcelExportRequest<ExportRow>()
                .setOutputStream(outputStream)
                .setHeadClass(ExportRow.class)
                .setSheetName("Users")
                .setData(List.of(new ExportRow("Alice", 18, "Tech"), new ExportRow("Bob", 20, "Ops"))));

        List<ExportRow> rows = ExcelTestSupport.readSheet(outputStream.toByteArray(), ExportRow.class);
        assertThat(rows).extracting(ExportRow::getName).containsExactly("Alice", "Bob");
        assertThat(rows).extracting(ExportRow::getDepartment).containsExactly("Tech", "Ops");
    }

    @Test
    void shouldApplyIncludeColumnFieldNames() throws Exception {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        service.export(new ExcelExportRequest<ExportRow>()
                .setOutputStream(outputStream)
                .setHeadClass(ExportRow.class)
                .setSheetName("Users")
                .setIncludeColumnFieldNames(Set.of("name", "department"))
                .setData(List.of(new ExportRow("Alice", 18, "Tech"))));

        try (XSSFWorkbook workbook = ExcelTestSupport.openWorkbook(outputStream.toByteArray())) {
            Row header = workbook.getSheetAt(0).getRow(0);
            Row data = workbook.getSheetAt(0).getRow(1);
            assertThat(header.getPhysicalNumberOfCells()).isEqualTo(2);
            assertThat(header.getCell(0).getStringCellValue()).isEqualTo("姓名");
            assertThat(header.getCell(1).getStringCellValue()).isEqualTo("部门");
            assertThat(data.getCell(0).getStringCellValue()).isEqualTo("Alice");
            assertThat(data.getCell(1).getStringCellValue()).isEqualTo("Tech");
        }
    }

    @Test
    void shouldExportByPageInProviderOrder() {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        service.exportByPage(new ExcelPagedExportRequest<ExportRow>()
                .setOutputStream(outputStream)
                .setHeadClass(ExportRow.class)
                .setSheetName("Users")
                .setPageSize(2)
                .setPageProvider((pageNo, pageSize) -> switch (pageNo) {
                    case 1 -> List.of(new ExportRow("A", 1, "D1"), new ExportRow("B", 2, "D2"));
                    case 2 -> List.of(new ExportRow("C", 3, "D3"));
                    default -> List.of();
                }));

        List<ExportRow> rows = ExcelTestSupport.readSheet(outputStream.toByteArray(), ExportRow.class);
        assertThat(rows).extracting(ExportRow::getName).containsExactly("A", "B", "C");
    }

    @Test
    void shouldRejectNullPageProviderResult() {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        assertThatThrownBy(() -> service.exportByPage(new ExcelPagedExportRequest<ExportRow>()
                .setOutputStream(outputStream)
                .setHeadClass(ExportRow.class)
                .setSheetName("Users")
                .setPageProvider((pageNo, pageSize) -> null)))
                .isInstanceOf(ServiceException.class)
                .hasMessageContaining("must not return null");
    }

    @Test
    void shouldInvokeWriteHandlersAndCustomizers() {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        AtomicBoolean workbookCustomized = new AtomicBoolean(false);
        AtomicBoolean sheetCustomized = new AtomicBoolean(false);
        AtomicInteger handlerCounter = new AtomicInteger();

        service.export(new ExcelExportRequest<ExportRow>()
                .setOutputStream(outputStream)
                .setHeadClass(ExportRow.class)
                .setSheetName("Users")
                .setData(List.of(new ExportRow("Alice", 18, "Tech")))
                .setWriteWorkbookCustomizer(builder -> workbookCustomized.set(true))
                .setWriteSheetCustomizer(builder -> sheetCustomized.set(true))
                .setWriteHandlers(List.of(new SheetWriteHandler() {
                    @Override
                    public void afterSheetCreate(com.alibaba.excel.write.metadata.holder.WriteWorkbookHolder writeWorkbookHolder,
                                                 com.alibaba.excel.write.metadata.holder.WriteSheetHolder writeSheetHolder) {
                        handlerCounter.incrementAndGet();
                    }
                })));

        assertThat(workbookCustomized.get()).isTrue();
        assertThat(sheetCustomized.get()).isTrue();
        assertThat(handlerCounter.get()).isGreaterThan(0);
    }

    @Test
    void shouldFailWhenIncludeAndExcludeConflict() {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        assertThatThrownBy(() -> service.export(new ExcelExportRequest<ExportRow>()
                .setOutputStream(outputStream)
                .setHeadClass(ExportRow.class)
                .setIncludeColumnFieldNames(Set.of("name"))
                .setExcludeColumnFieldNames(Set.of("department"))))
                .isInstanceOf(ServiceException.class)
                .hasMessageContaining("cannot both be set");
    }

    public static class ExportRow {
        @ExcelProperty("姓名")
        private String name;

        @ExcelProperty("年龄")
        private Integer age;

        @ExcelProperty("部门")
        private String department;

        public ExportRow() {
        }

        public ExportRow(String name, Integer age, String department) {
            this.name = name;
            this.age = age;
            this.department = department;
        }

        public String getName() {
            return name;
        }

        public Integer getAge() {
            return age;
        }

        public String getDepartment() {
            return department;
        }

        public void setName(String name) {
            this.name = name;
        }

        public void setAge(Integer age) {
            this.age = age;
        }

        public void setDepartment(String department) {
            this.department = department;
        }
    }
}
