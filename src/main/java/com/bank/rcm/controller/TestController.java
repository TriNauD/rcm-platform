package com.bank.rcm.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.bank.rcm.entity.RegulatoryInventory;
import com.bank.rcm.repository.RegulatoryInventoryRepository;
import com.bank.rcm.service.ComplianceService;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;

@RestController
@RequestMapping("/api/test")
public class TestController {

    @Autowired
    private RegulatoryInventoryRepository repository;

    @Autowired
    private ComplianceService complianceService;

    @GetMapping("/check")
    public List<RegulatoryInventory> getAll() {
        return repository.findAll();
    }

    @PostMapping("/upload-mock")
    public String triggerUpload() {
        return complianceService.processExcelUpload();
    }

}
