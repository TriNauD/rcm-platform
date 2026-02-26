package com.bank.rcm.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.bank.rcm.service.ComplianceStepBService;
import org.springframework.web.bind.annotation.PostMapping;

import java.io.IOException;

@RestController
@RequestMapping("/api/mapping")
public class RcmMappingController {
    @Autowired
    private ComplianceStepBService complianceStepBService;

    /**
     * 上传并处理Step B合规验证文件
     *
     * @param file 待上传的Excel合规文件
     * @return 处理结果摘要或错误信息
     */
    @PostMapping("/upload")
    public String uploadStepBFile(@RequestParam("file") MultipartFile file) {
        // 校验文件有效性
        if (file == null || file.isEmpty()) {
            return "失败：文件为空";
        }
        
        try {
            return complianceStepBService.processStepBFile(file);
        } catch (IllegalArgumentException e) {
            // 参数验证异常
            return "失败：参数错误 - " + e.getMessage();
        } catch (IOException e) {
            // 文件读取异常
            return "失败：文件读取异常 - " + e.getMessage();
        } catch (Exception e) {
            // 其他异常
            return "失败：处理异常 - " + e.getMessage();
        }
    }

}
