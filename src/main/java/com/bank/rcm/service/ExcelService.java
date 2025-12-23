package com.bank.rcm.service;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.bank.rcm.entity.RegulatoryInventory;

@Service
public class ExcelService {
    public List<RegulatoryInventory> mockParseExcel() {
        List<RegulatoryInventory> list = new ArrayList<>();

        RegulatoryInventory row1 = new RegulatoryInventory();
        row1.setPublicationId("PUBN-row1");
        row1.setPublicationName("row1");
        list.add(row1);

        RegulatoryInventory row2 = new RegulatoryInventory();
        row2.setPublicationId("PUBN-row2");
        row2.setPublicationName("row2");
        list.add(row2);

        return list;
    }

    public List<RegulatoryInventory> parseRegulatoryExcel(MultipartFile file) throws Exception {
        List<RegulatoryInventory> list = new ArrayList<>();

        try (InputStream is = file.getInputStream();
                Workbook workbook = new XSSFWorkbook(is)) {
            Sheet sheet = workbook.getSheetAt(0);

            for (Row row : sheet) {
                // 跳过表头
                if (row.getRowNum() == 0) {
                    continue;
                }
                RegulatoryInventory inventory = new RegulatoryInventory();
                inventory.setPublicationId(getCellValue(row.getCell(0)));
                inventory.setPublicationName(getCellValue(row.getCell(1)));
                inventory.setRegulator(getCellValue(row.getCell(2)));
                inventory.setRegulatorTier(getCellValue(row.getCell(3)));
                list.add(inventory);
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
