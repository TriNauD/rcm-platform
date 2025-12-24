package com.bank.rcm.service;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.bank.rcm.entity.RegulatoryInventory;
import com.bank.rcm.repository.RegulatoryInventoryRepository;

import jakarta.transaction.Transactional;

@Service
public class ComplianceService {
    @Autowired
    private ExcelService excelService;

    @Autowired
    private CubeInternalSerice cubeInternalSerice;

    @Autowired
    private RegulatoryInventoryRepository repo;

    @Transactional
    public String processComplianceFile(MultipartFile file) throws Exception {
        int toUpdateRowCount = 0, toInsertRowCount = 0;

        // parse excel
        List<RegulatoryInventory> rawData = excelService.parseRegulatoryExcel(file);

        // process all data
        for (RegulatoryInventory data : rawData) {
            Optional<RegulatoryInventory> existOpt = repo.findByPublicationId(data.getPublicationId());
            RegulatoryInventory targetEntity;
            if (existOpt.isPresent()) {
                targetEntity = existOpt.get();
                targetEntity.setPublicationName(data.getPublicationName());
                targetEntity.setComplianceStatus(data.getComplianceStatus());
                targetEntity.setRegulator(data.getRegulator());
                toUpdateRowCount++;
            } else {
                targetEntity = data;
                toInsertRowCount++;
            }

            // validate with cube
            boolean isValidCompliance = cubeInternalSerice.validateWithCube(data.getPublicationId());
            if (isValidCompliance) {
                data.setComplianceStatus("COMPLIANT");
                data.setValidationResult("CUBE validation passed.");
            } else {
                data.setComplianceStatus("NON_COMPLIANT");
                data.setValidationResult("CUBE validation failed: Invalid ID format or mapping missing.");
            }

            // save
            repo.save(targetEntity);
        }

        return String.format("处理完成：更新 %d 条，新增 %d 条。", toUpdateRowCount, toInsertRowCount);
    }
}
