package com.vita.poi.excel.service.impl;

import com.alibaba.excel.write.metadata.fill.FillConfig;
import com.vita.core.exception.ServiceException;
import com.vita.poi.excel.model.request.ExcelFillRequest;
import com.vita.poi.excel.property.PoiExcelProperty;
import com.vita.poi.excel.support.ExcelFillSupport;
import com.vita.poi.excel.support.ExcelRequestValidator;
import com.vita.poi.excel.support.ExcelTestSupport;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DefaultExcelFillServiceTest {

    private DefaultExcelFillService service;

    @BeforeEach
    void setUp() {
        PoiExcelProperty property = ExcelTestSupport.defaultProperty();
        service = new DefaultExcelFillService(property, new ExcelRequestValidator(), new ExcelFillSupport());
    }

    @Test
    void shouldFillObjectTemplate() throws Exception {
        byte[] template = createWorkbook(workbook -> {
            XSSFSheet sheet = workbook.createSheet("Sheet1");
            Row row = sheet.createRow(0);
            row.createCell(0).setCellValue("姓名");
            row.createCell(1).setCellValue("{name}");
        });
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        service.fill(new ExcelFillRequest()
                .setTemplateInputStream(new ByteArrayInputStream(template))
                .setOutputStream(outputStream)
                .setObjectData(Map.of("name", "Alice")));

        try (XSSFWorkbook workbook = ExcelTestSupport.openWorkbook(outputStream.toByteArray())) {
            assertThat(workbook.getSheetAt(0).getRow(0).getCell(1).getStringCellValue()).isEqualTo("Alice");
        }
    }

    @Test
    void shouldFillListTemplate() throws Exception {
        byte[] template = createWorkbook(workbook -> {
            XSSFSheet sheet = workbook.createSheet("Sheet1");
            Row header = sheet.createRow(0);
            header.createCell(0).setCellValue("姓名");
            header.createCell(1).setCellValue("年龄");
            Row row = sheet.createRow(1);
            row.createCell(0).setCellValue("{.name}");
            row.createCell(1).setCellValue("{.age}");
        });
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        service.fill(new ExcelFillRequest()
                .setTemplateInputStream(new ByteArrayInputStream(template))
                .setOutputStream(outputStream)
                .setListData(List.of(Map.of("name", "Alice", "age", 18), Map.of("name", "Bob", "age", 20)))
                .setFillConfig(FillConfig.builder().forceNewRow(Boolean.TRUE).build()));

        try (XSSFWorkbook workbook = ExcelTestSupport.openWorkbook(outputStream.toByteArray())) {
            assertThat(workbook.getSheetAt(0).getRow(1).getCell(0).getStringCellValue()).isEqualTo("Alice");
            assertThat(workbook.getSheetAt(0).getRow(2).getCell(0).getStringCellValue()).isEqualTo("Bob");
        }
    }

    @Test
    void shouldFillObjectAndListInOrder() throws Exception {
        byte[] template = createWorkbook(workbook -> {
            XSSFSheet sheet = workbook.createSheet("Sheet1");
            Row titleRow = sheet.createRow(0);
            titleRow.createCell(0).setCellValue("{title}");
            Row listRow = sheet.createRow(2);
            listRow.createCell(0).setCellValue("{.name}");
            listRow.createCell(1).setCellValue("{.age}");
        });
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        service.fill(new ExcelFillRequest()
                .setTemplateInputStream(new ByteArrayInputStream(template))
                .setOutputStream(outputStream)
                .setObjectData(Map.of("title", "用户列表"))
                .setListData(List.of(Map.of("name", "Alice", "age", 18), Map.of("name", "Bob", "age", 20)))
                .setFillConfig(FillConfig.builder().forceNewRow(Boolean.TRUE).build()));

        try (XSSFWorkbook workbook = ExcelTestSupport.openWorkbook(outputStream.toByteArray())) {
            assertThat(workbook.getSheetAt(0).getRow(0).getCell(0).getStringCellValue()).isEqualTo("用户列表");
            assertThat(workbook.getSheetAt(0).getRow(2).getCell(0).getStringCellValue()).isEqualTo("Alice");
            assertThat(workbook.getSheetAt(0).getRow(3).getCell(0).getStringCellValue()).isEqualTo("Bob");
        }
    }

    @Test
    void shouldFailWhenTemplateMissing() {
        assertThatThrownBy(() -> service.fill(new ExcelFillRequest()
                .setOutputStream(new ByteArrayOutputStream())
                .setObjectData(Map.of("name", "Alice"))))
                .isInstanceOf(ServiceException.class)
                .hasMessageContaining("templateInputStream");
    }

    @Test
    void shouldFillSpecifiedSheetOnly() throws Exception {
        byte[] template = createWorkbook(workbook -> {
            XSSFSheet firstSheet = workbook.createSheet("First");
            firstSheet.createRow(0).createCell(0).setCellValue("unchanged");
            XSSFSheet targetSheet = workbook.createSheet("Target");
            targetSheet.createRow(0).createCell(0).setCellValue("{name}");
        });
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        service.fill(new ExcelFillRequest()
                .setTemplateInputStream(new ByteArrayInputStream(template))
                .setOutputStream(outputStream)
                .setSheetName("Target")
                .setObjectData(Map.of("name", "Alice")));

        try (XSSFWorkbook workbook = ExcelTestSupport.openWorkbook(outputStream.toByteArray())) {
            assertThat(workbook.getSheet("First").getRow(0).getCell(0).getStringCellValue()).isEqualTo("unchanged");
            assertThat(workbook.getSheet("Target").getRow(0).getCell(0).getStringCellValue()).isEqualTo("Alice");
        }
    }

    private byte[] createWorkbook(WorkbookCustomizer customizer) throws Exception {
        try (XSSFWorkbook workbook = new XSSFWorkbook(); ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            customizer.customize(workbook);
            workbook.write(outputStream);
            return outputStream.toByteArray();
        }
    }

    @FunctionalInterface
    private interface WorkbookCustomizer {
        void customize(XSSFWorkbook workbook) throws Exception;
    }
}
