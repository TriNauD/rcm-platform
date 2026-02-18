package com.bank.rcm.service;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.bank.rcm.entity.ComplianceInventory;
import com.bank.rcm.repository.ObligationInventoryRepository;

import jakarta.transaction.Transactional;

@Service
public class ComplianceService {
    @Autowired
    private ExcelService excelService;

    @Autowired
    private CubeInternalService cubeInternalService;

    @Autowired
    private ObligationInventoryRepository repo;

    @Transactional
    public String processComplianceFile(MultipartFile file) throws Exception {
        int toUpdateRowCount = 0, toInsertRowCount = 0;

        // parse excel
        List<ComplianceInventory> rawData = excelService.parseRegulatoryExcel(file);

        // process all data
        for (ComplianceInventory data : rawData) {
            Optional<ComplianceInventory> existOpt = repo.findByObligationId(data.getObligationId());
            ComplianceInventory targetEntity;
            if (existOpt.isPresent()) {
                targetEntity = existOpt.get();
                targetEntity.setObligationName(data.getObligationName());
                targetEntity.setRegulator(data.getRegulator());
                toUpdateRowCount++;
            } else {
                targetEntity = data;
                toInsertRowCount++;
            }

            // validate with cube
            boolean isValidCompliance = cubeInternalService.validateWithCube(data.getObligationId());
            if (isValidCompliance) {
                data.setObligationStatus("COMPLIANT");
                data.setValidationResult("CUBE validation passed.");
            } else {
                data.setObligationStatus("NON_COMPLIANT");
                data.setValidationResult("CUBE validation failed: Invalid ID format or mapping missing.");
            }

            // save
            repo.save(targetEntity);
        }

        return String.format("处理完成：更新 %d 条，新增 %d 条。", toUpdateRowCount, toInsertRowCount);
    }
}
