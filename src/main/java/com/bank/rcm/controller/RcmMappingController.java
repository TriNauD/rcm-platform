package com.bank.rcm.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.bank.rcm.service.ComplianceStepBService;
import org.springframework.web.bind.annotation.PostMapping;

@RestController
@RequestMapping("/api/mapping")
public class RcmMappingController {
    @Autowired
    private ComplianceStepBService complianceStepBService;

    @PostMapping("/upload")
    public String postMethodName(@RequestParam("file") MultipartFile file) {
        try {
            return complianceStepBService.processStepBFile(file);
        } catch (Exception e) {
            return "上传失败： " + e.getMessage();
        }
    }

}
