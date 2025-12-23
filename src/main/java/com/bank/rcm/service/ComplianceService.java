package com.bank.rcm.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.bank.rcm.entity.RegulatoryInventory;
import com.bank.rcm.repository.RegulatoryInventoryRepository;

import jakarta.transaction.Transactional;

@Service
public class ComplianceService {
    @Autowired
    private ExcelService excelService;

    @Autowired
    private RegulatoryInventoryRepository repo;

    @Transactional
    public String processExcelUpload() {
        List<RegulatoryInventory> excelResList = excelService.mockParseExcel();
        
        for (RegulatoryInventory excelRes : excelResList ) {
            System.out.println("正在同步校验： " + excelRes.getPublicationId());
            repo.save(excelRes);
        }
        return "成功处理 " + excelResList.size() + " 条数据！";
    }
}
