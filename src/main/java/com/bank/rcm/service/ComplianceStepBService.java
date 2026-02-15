package com.bank.rcm.service;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.bank.rcm.dto.StepBDto;
import com.bank.rcm.mapper.RcmMappingEntity;
import com.bank.rcm.repository.RcmMappingRepository;

import jakarta.transaction.Transactional;

@Service
public class ComplianceStepBService {

    @Autowired
    private ExcelService excelService;

    @Autowired
    private RcmMappingRepository rcmMappingRepository;

    @Transactional
    public String processStepBFile(MultipartFile file) throws Exception {
        int toInsertRowCount = 0;

        List<StepBDto> rawData = excelService.parseStepBExcel(file);

        for (StepBDto stepBDto : rawData) {
            List<String> ceamList = Arrays.stream(stepBDto.getCeamIds().split("\\|"))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .collect(Collectors.toList());
            List<RcmMappingEntity> rcmMappingEntities = ceamList.stream()
                    .map(ceamId -> {
                        RcmMappingEntity entity = new RcmMappingEntity();
                        entity.setObId(stepBDto.getObligationId());
                        entity.setCesId(stepBDto.getCesId());
                        entity.setCeamId(ceamId);
                        return entity;
                    }).collect(Collectors.toList());
            toInsertRowCount += rcmMappingEntities.size();
            rcmMappingRepository.saveAll(rcmMappingEntities);
        }

        return String.format("处理完成：新增 %d 条。", toInsertRowCount);
    }
}
