package com.bank.rcm.service;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.bank.rcm.annotation.MonitorPerformance;
import com.bank.rcm.dto.StepBDto;
import com.bank.rcm.entity.ControlExpectation;
import com.bank.rcm.mapper.RcmMappingEntity;
import com.bank.rcm.repository.ControlExpectationRepository;
import com.bank.rcm.repository.RcmMappingRepository;

import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class ComplianceStepBService {

    @Autowired
    private ExcelService excelService;

    @Autowired
    private CubeInternalSerice cubeInternalSerice;

    @Autowired
    private RcmMappingRepository rcmMappingRepository;

    @Autowired
    private ControlExpectationRepository cesRepository;

    @Transactional
    @MonitorPerformance("POI-Legacy-Mode")
    // 性能最差的版本，用poi，单线程，一条一条校验、插入，4000行（8000插入）耗时xxx
    public String processStepBFileWithPoiSingleThread(MultipartFile file) throws Exception {
        int totalInserted = 0;
        int totalUpdated = 0;
        int mappingCount = 0;

        // 1. POI 模式：一次性加载 4000 行到内存（内存隐患点）
        List<StepBDto> rawData = excelService.parseStepBExcel(file);

        for (StepBDto data : rawData) {
            // 2. 校验 OB (CUBE 接口同步调用 - 最大的性能黑洞)
            boolean isObValid = cubeInternalSerice.validateWithCube(data.getObligationId());
            if (!isObValid) {
                log.warn("Invalid OB ID: {}", data.getObligationId());
                continue;
            }

            // 3. 校验并保存 CES (每一行都查一次库，存一次库)
            Optional<ControlExpectation> cesOpt = cesRepository.findByCesId(data.getCesId());
            ControlExpectation cesEntity;
            if (cesOpt.isPresent()) {
                cesEntity = cesOpt.get();
                cesEntity.setCesDesc(data.getCesStatement());
                totalUpdated++;
            } else {
                cesEntity = new ControlExpectation();
                cesEntity.setCesId(data.getCesId());
                cesEntity.setCesDesc(data.getCesStatement());
                totalInserted++;
            }
            cesRepository.save(cesEntity); // 频繁写库

            // 4. 拆分 CEAM 并保存映射 (循环内处理多对多)
            if (data.getCeamIds() != null && !data.getCeamIds().isEmpty()) {
                String[] ceamArray = data.getCeamIds().split("\\|");
                for (String ceamId : ceamArray) {
                    RcmMappingEntity mapping = new RcmMappingEntity();
                    mapping.setObId(data.getObligationId());
                    mapping.setCesId(data.getCesId());
                    mapping.setCeamId(ceamId.trim());
                    rcmMappingRepository.save(mapping); // 又是频繁写库，还是在二层循环里写
                    mappingCount++;
                }
            }
        }

        return String.format("Legacy Mode Summary: Updated %d CES, Inserted %d CES, Created %d Mappings.",
                totalUpdated, totalInserted, mappingCount);
    }
}
