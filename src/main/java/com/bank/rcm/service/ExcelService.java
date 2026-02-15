package com.bank.rcm.service;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.context.AnalysisContext;
import com.alibaba.excel.event.AnalysisEventListener;
import com.bank.rcm.dto.StepBDto;
import com.bank.rcm.entity.ComplianceInventory;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class ExcelService {

    // 泛式，增加可扩展性，以后换成别的DTO也能使
    // 原数据流/dto/具体处理方法
    public <T> void readExcelInStream(InputStream inputStream, Class<T> clazz, Consumer<T> rowProcessor) {
        EasyExcel.read(inputStream, clazz, new AnalysisEventListener<T>() {
            @Override
            public void invoke(T data, AnalysisContext context) {
                // 调用传入的处理逻辑
                rowProcessor.accept(data);
            }

            @Override
            public void doAfterAllAnalysed(AnalysisContext context) {
                log.info("Excel 读取完成");
            }
        }).sheet().doRead();
    }

    public List<ComplianceInventory> mockParseExcel() {
        List<ComplianceInventory> list = new ArrayList<>();

        ComplianceInventory row1 = new ComplianceInventory();
        row1.setObligationId("PUBN-row1");
        row1.setObligationName("row1");
        list.add(row1);

        ComplianceInventory row2 = new ComplianceInventory();
        row2.setObligationId("PUBN-row2");
        row2.setObligationName("row2");
        list.add(row2);

        return list;
    }

    public List<ComplianceInventory> parseRegulatoryExcel(MultipartFile file) throws Exception {
        List<ComplianceInventory> list = new ArrayList<>();

        try (InputStream is = file.getInputStream();
                Workbook workbook = new XSSFWorkbook(is)) {
            Sheet sheet = workbook.getSheetAt(0);

            for (Row row : sheet) {
                // 跳过表头
                if (row.getRowNum() == 0) {
                    continue;
                }
                ComplianceInventory inventory = new ComplianceInventory();
                inventory.setObligationId(getCellValue(row.getCell(0)));
                inventory.setObligationName(getCellValue(row.getCell(1)));
                inventory.setRegulator(getCellValue(row.getCell(2)));
                list.add(inventory);
            }

        }
        return list;
    }

    public List<StepBDto> parseStepBExcel(MultipartFile file) throws Exception {
        List<StepBDto> list = new ArrayList<>();

        try (InputStream is = file.getInputStream();
                Workbook workbook = new XSSFWorkbook(is)) {
            Sheet sheet = workbook.getSheetAt(0);

            for (Row row : sheet) {
                if (row.getRowNum() == 0) {
                    continue;
                }
                StepBDto stepBDto = new StepBDto();
                stepBDto.setObligationId(getCellValue(row.getCell(0)));
                stepBDto.setCesId(getCellValue(row.getCell(1)));
                stepBDto.setCesStatement(getCellValue(row.getCell(2)));
                stepBDto.setCeamIds(getCellValue(row.getCell(3)));
                list.add(stepBDto);
            }
        }

        return list;
    }

    private String getCellValue(Cell cell) {
        if (cell == null) {
            return "";
        }
        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue();
            case NUMERIC:
                return String.valueOf((int) cell.getNumericCellValue());
            default:
                return "";
        }
    }
}
