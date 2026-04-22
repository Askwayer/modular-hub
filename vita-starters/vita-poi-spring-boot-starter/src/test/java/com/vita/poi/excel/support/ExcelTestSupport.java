package com.vita.poi.excel.support;

import com.alibaba.excel.EasyExcel;
import com.vita.poi.excel.property.PoiExcelProperty;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Collection;
import java.util.List;

/**
 * Excel 测试辅助工具。
 */
public final class ExcelTestSupport {

    private ExcelTestSupport() {
    }

    public static PoiExcelProperty defaultProperty() {
        PoiExcelProperty property = new PoiExcelProperty();
        property.normalizeAndValidate();
        return property;
    }

    public static <T> byte[] writeSheet(Collection<T> data, Class<T> headClass, String sheetName) {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        EasyExcel.write(outputStream, headClass)
                .sheet(sheetName)
                .doWrite(data);
        return outputStream.toByteArray();
    }

    public static <T> List<T> readSheet(byte[] content, Class<T> headClass) {
        return EasyExcel.read(new ByteArrayInputStream(content))
                .head(headClass)
                .sheet()
                .doReadSync();
    }

    public static XSSFWorkbook openWorkbook(byte[] content) throws IOException {
        return new XSSFWorkbook(new ByteArrayInputStream(content));
    }
}
