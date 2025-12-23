package com.bank.rcm.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.bank.rcm.entity.RegulatoryInventory;
import com.bank.rcm.repository.RegulatoryInventoryRepository;
import com.bank.rcm.service.ExcelService;
import org.springframework.web.bind.annotation.PostMapping;

@RestController
@RequestMapping("/api/compliance")
public class ComplianceController {
    @Autowired
    private ExcelService excelService;

    @Autowired
    private RegulatoryInventoryRepository repository;

    @PostMapping("/upload")
    public String postMethodName(@RequestParam("file") MultipartFile file) {
        try {
            // 调用解析
            List<RegulatoryInventory> data = excelService.parseRegulatoryExcel(file);
            // 保存数据
            repository.saveAll(data);
            return "上传成功，共导入 " + data.size() + " 条记录！";
        } catch (Exception e) {
            return "上传失败： " + e.getMessage();
        }
    }

}
