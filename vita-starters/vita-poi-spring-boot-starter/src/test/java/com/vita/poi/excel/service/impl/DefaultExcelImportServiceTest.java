package com.vita.poi.excel.service.impl;

import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.ExcelWriter;
import com.alibaba.excel.annotation.ExcelProperty;
import com.alibaba.excel.write.metadata.WriteSheet;
import com.vita.poi.excel.model.request.ExcelImportRequest;
import com.vita.poi.excel.model.result.ExcelImportResult;
import com.vita.poi.excel.processor.ExcelImportProcessor;
import com.vita.poi.excel.property.PoiExcelProperty;
import com.vita.poi.excel.support.ExcelReadSupport;
import com.vita.poi.excel.support.ExcelRequestValidator;
import com.vita.poi.excel.support.ExcelTestSupport;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

class DefaultExcelImportServiceTest {

    private PoiExcelProperty property;
    private DefaultExcelImportService service;

    @BeforeEach
    void setUp() {
        property = ExcelTestSupport.defaultProperty();
        service = new DefaultExcelImportService(property, new ExcelRequestValidator(), new ExcelReadSupport());
    }

    @Test
    void shouldImportSingleSheetSuccessfully() {
        byte[] content = ExcelTestSupport.writeSheet(
                List.of(new UserRow("Alice", 18), new UserRow("Bob", 20)),
                UserRow.class,
                "Users"
        );
        List<UserRow> savedRows = new ArrayList<>();

        ExcelImportResult result = service.importExcel(importRequest(content, UserRow.class), new ExcelImportProcessor<>() {
            @Override
            public void saveBatch(List<UserRow> rows, com.vita.poi.excel.model.context.ExcelBatchContext context) {
                savedRows.addAll(rows);
            }
        });

        assertThat(result.getTotalRows()).isEqualTo(2);
        assertThat(result.getSuccessRows()).isEqualTo(2);
        assertThat(result.getFailedRows()).isEqualTo(0);
        assertThat(savedRows).extracting(UserRow::getName).containsExactly("Alice", "Bob");
    }

    @Test
    void shouldPreferSheetNoOverSheetName() {
        byte[] content = writeTwoSheetWorkbook();
        List<UserRow> savedRows = new ArrayList<>();

        ExcelImportRequest<UserRow> request = importRequest(content, UserRow.class)
                .setSheetNo(0)
                .setSheetName("SecondSheet");

        ExcelImportResult result = service.importExcel(request, new ExcelImportProcessor<>() {
            @Override
            public void saveBatch(List<UserRow> rows, com.vita.poi.excel.model.context.ExcelBatchContext context) {
                savedRows.addAll(rows);
            }
        });

        assertThat(result.getSuccessRows()).isEqualTo(1);
        assertThat(savedRows).extracting(UserRow::getName).containsExactly("First");
    }

    @Test
    void shouldRespectMultiHeadRowNumber() {
        byte[] content = ExcelTestSupport.writeSheet(
                List.of(new MultiHeadUserRow("Alice", 18)),
                MultiHeadUserRow.class,
                "Users"
        );
        List<MultiHeadUserRow> savedRows = new ArrayList<>();

        ExcelImportResult result = service.importExcel(
                importRequest(content, MultiHeadUserRow.class).setHeadRowNumber(2),
                new ExcelImportProcessor<>() {
                    @Override
                    public void saveBatch(List<MultiHeadUserRow> rows,
                                          com.vita.poi.excel.model.context.ExcelBatchContext context) {
                        savedRows.addAll(rows);
                    }
                }
        );

        assertThat(result.getSuccessRows()).isEqualTo(1);
        assertThat(savedRows).extracting(MultiHeadUserRow::getName).containsExactly("Alice");
    }

    @Test
    void shouldTrimDtoValueAndRawDataWhenEnabled() {
        byte[] content = writePlainWorkbook(List.<String[]>of(new String[]{" Alice ", "18"}));
        AtomicReference<String> capturedRawName = new AtomicReference<>();
        List<UserRow> savedRows = new ArrayList<>();

        ExcelImportResult result = service.importExcel(importRequest(content, UserRow.class), new ExcelImportProcessor<>() {
            @Override
            public List<String> validate(UserRow row, com.vita.poi.excel.model.context.ExcelRowContext context) {
                capturedRawName.set(context.getRawData().get(0));
                assertThat(row.getName()).isEqualTo("Alice");
                return List.of();
            }

            @Override
            public void saveBatch(List<UserRow> rows, com.vita.poi.excel.model.context.ExcelBatchContext context) {
                savedRows.addAll(rows);
            }
        });

        assertThat(result.getSuccessRows()).isEqualTo(1);
        assertThat(capturedRawName.get()).isEqualTo("Alice");
        assertThat(savedRows.get(0).getName()).isEqualTo("Alice");
    }

    @Test
    void shouldTreatMultipleValidateMessagesAsSingleFailedRow() {
        byte[] content = ExcelTestSupport.writeSheet(List.of(new UserRow("", -1)), UserRow.class, "Users");

        ExcelImportResult result = service.importExcel(importRequest(content, UserRow.class), new ExcelImportProcessor<>() {
            @Override
            public List<String> validate(UserRow row, com.vita.poi.excel.model.context.ExcelRowContext context) {
                return List.of("name is blank", "age must be positive");
            }

            @Override
            public void saveBatch(List<UserRow> rows, com.vita.poi.excel.model.context.ExcelBatchContext context) {
            }
        });

        assertThat(result.getTotalRows()).isEqualTo(1);
        assertThat(result.getSuccessRows()).isEqualTo(0);
        assertThat(result.getFailedRows()).isEqualTo(1);
        assertThat(result.getErrors()).hasSize(2);
        assertThat(result.getErrors()).allSatisfy(error -> assertThat(error.getFieldName()).isNull());
    }

    @Test
    void shouldConvertWholeBatchToFailedRowsWhenSaveBatchThrows() {
        byte[] content = ExcelTestSupport.writeSheet(
                List.of(new UserRow("Alice", 18), new UserRow("Bob", 20)),
                UserRow.class,
                "Users"
        );

        ExcelImportResult result = service.importExcel(
                importRequest(content, UserRow.class).setBatchSize(2),
                new ExcelImportProcessor<>() {
                    @Override
                    public void saveBatch(List<UserRow> rows, com.vita.poi.excel.model.context.ExcelBatchContext context) {
                        throw new IllegalStateException("save batch failed");
                    }
                }
        );

        assertThat(result.getSuccessRows()).isEqualTo(0);
        assertThat(result.getFailedRows()).isEqualTo(2);
        assertThat(result.getErrors()).hasSize(2);
        assertThat(result.getErrors()).allSatisfy(error -> assertThat(error.getMessage()).contains("save batch failed"));
    }

    @Test
    void shouldCaptureConvertExceptionAndContinueReading() {
        byte[] content = writePlainWorkbook(List.<String[]>of(
                new String[]{"BadAge", "abc"},
                new String[]{"GoodAge", "20"}
        ));
        List<UserRow> savedRows = new ArrayList<>();

        ExcelImportResult result = service.importExcel(importRequest(content, UserRow.class), new ExcelImportProcessor<>() {
            @Override
            public void saveBatch(List<UserRow> rows, com.vita.poi.excel.model.context.ExcelBatchContext context) {
                savedRows.addAll(rows);
            }
        });

        assertThat(result.getTotalRows()).isEqualTo(2);
        assertThat(result.getSuccessRows()).isEqualTo(1);
        assertThat(result.getFailedRows()).isEqualTo(1);
        assertThat(result.getErrors()).hasSize(1);
        assertThat(result.getErrors().get(0).getFieldName()).isEqualTo("age");
        assertThat(savedRows).extracting(UserRow::getName).containsExactly("GoodAge");
    }

    @Test
    void shouldTruncateErrorDetailsAfterConfiguredLimit() {
        property.setMaxErrorRows(2);
        property.normalizeAndValidate();
        service = new DefaultExcelImportService(property, new ExcelRequestValidator(), new ExcelReadSupport());
        byte[] content = ExcelTestSupport.writeSheet(
                List.of(new UserRow("", 1), new UserRow("", 2), new UserRow("", 3)),
                UserRow.class,
                "Users"
        );

        ExcelImportResult result = service.importExcel(importRequest(content, UserRow.class), new ExcelImportProcessor<>() {
            @Override
            public List<String> validate(UserRow row, com.vita.poi.excel.model.context.ExcelRowContext context) {
                return List.of("invalid row");
            }

            @Override
            public void saveBatch(List<UserRow> rows, com.vita.poi.excel.model.context.ExcelBatchContext context) {
            }
        });

        assertThat(result.getFailedRows()).isEqualTo(3);
        assertThat(result.getErrors()).hasSize(2);
        assertThat(result.getTruncated()).isTrue();
    }

    private <T> ExcelImportRequest<T> importRequest(byte[] content, Class<T> headClass) {
        return new ExcelImportRequest<T>()
                .setInputStream(new ByteArrayInputStream(content))
                .setFileName("users.xlsx")
                .setHeadClass(headClass);
    }

    private byte[] writeTwoSheetWorkbook() {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        try (ExcelWriter writer = EasyExcel.write(outputStream, UserRow.class).build()) {
            WriteSheet firstSheet = EasyExcel.writerSheet(0, "FirstSheet").head(UserRow.class).build();
            WriteSheet secondSheet = EasyExcel.writerSheet(1, "SecondSheet").head(UserRow.class).build();
            writer.write(List.of(new UserRow("First", 18)), firstSheet);
            writer.write(List.of(new UserRow("Second", 19)), secondSheet);
        }
        return outputStream.toByteArray();
    }

    private byte[] writePlainWorkbook(List<String[]> rows) {
        try (XSSFWorkbook workbook = new XSSFWorkbook(); ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            XSSFSheet sheet = workbook.createSheet("Users");
            Row header = sheet.createRow(0);
            header.createCell(0).setCellValue("姓名");
            header.createCell(1).setCellValue("年龄");
            for (int i = 0; i < rows.size(); i++) {
                Row row = sheet.createRow(i + 1);
                row.createCell(0).setCellValue(rows.get(i)[0]);
                row.createCell(1).setCellValue(rows.get(i)[1]);
            }
            workbook.write(outputStream);
            return outputStream.toByteArray();
        } catch (Exception exception) {
            throw new IllegalStateException(exception);
        }
    }

    public static class UserRow {
        @ExcelProperty("姓名")
        private String name;

        @ExcelProperty("年龄")
        private Integer age;

        public UserRow() {
        }

        public UserRow(String name, Integer age) {
            this.name = name;
            this.age = age;
        }

        public String getName() {
            return name;
        }

        public Integer getAge() {
            return age;
        }

        public void setName(String name) {
            this.name = name;
        }

        public void setAge(Integer age) {
            this.age = age;
        }
    }

    public static class MultiHeadUserRow {
        @ExcelProperty({"基本信息", "姓名"})
        private String name;

        @ExcelProperty({"基本信息", "年龄"})
        private Integer age;

        public MultiHeadUserRow() {
        }

        public MultiHeadUserRow(String name, Integer age) {
            this.name = name;
            this.age = age;
        }

        public String getName() {
            return name;
        }

        public Integer getAge() {
            return age;
        }

        public void setName(String name) {
            this.name = name;
        }

        public void setAge(Integer age) {
            this.age = age;
        }
    }
}
