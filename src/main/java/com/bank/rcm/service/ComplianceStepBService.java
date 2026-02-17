package com.bank.rcm.service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.bank.rcm.annotation.MonitorPerformance;
import com.bank.rcm.dto.ProcessResult;
import com.bank.rcm.dto.StepBDto;
import com.bank.rcm.entity.ControlExpectation;
import com.bank.rcm.mapper.RcmMappingEntity;
import com.bank.rcm.repository.ControlExpectationRepository;
import com.bank.rcm.repository.RcmMappingRepository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
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

    @PersistenceContext
    private EntityManager entityManager;

    @Transactional
    @MonitorPerformance("EasyExcel-Batch-Process")
    public String processStepBFileWithEasyExcel(MultipartFile file) throws IOException {
        ProcessResult result = new ProcessResult();

        // 用于保存DTO用于批处理
        List<StepBDto> batchBuffer = new ArrayList<>();
        final int BATCH_SIZE = 100;
        // 缓存已查询过的ob，无需重复调cube重复校验
        Map<String, Boolean> obCache = new HashMap<>();

        // excelService每在stream读到一个StepBDto，都把它作为参数丢进processSingleRow去处理
        excelService.readExcelInStream(
                file.getInputStream(),
                StepBDto.class,
                data -> {
                    batchBuffer.add(data);
                    if (batchBuffer.size() >= BATCH_SIZE) {
                        processAndSaveBatch(batchBuffer, result, obCache);
                        batchBuffer.clear();
                    }
                });

        // 收尾最后未满100条的
        if (!batchBuffer.isEmpty()) {
            processAndSaveBatch(batchBuffer, result, obCache);
            batchBuffer.clear();
        }

        // 解析完成后，返回汇总信息
        return result.toSummaryString();
    }

    private void processAndSaveBatch(List<StepBDto> batch, ProcessResult result, Map<String, Boolean> obCache) {
        List<ControlExpectation> cesList = new ArrayList<>();
        List<RcmMappingEntity> mappingList = new ArrayList<>();

        for (StepBDto data : batch) {
            // 如果没有则调cube，否则
            boolean isValid = obCache.computeIfAbsent(data.getObligationId(),
                    id -> cubeInternalSerice.validateWithCube(id));
            if (!isValid) {
                continue;
            }
            // 组装待写入的数据
            processEntity(data, cesList, mappingList, result);
        }

        // 批量写入数据
        cesRepository.saveAll(cesList);
        rcmMappingRepository.saveAll(mappingList);

        // 刷新并清除
        entityManager.flush(); // 将缓存中的变更立即同步到数据库
        entityManager.clear(); // 清除一级缓存，释放内存空间

        cesList.clear();
        mappingList.clear();
    }

    private void processEntity(StepBDto data, List<ControlExpectation> cesList, List<RcmMappingEntity> mappingList,
            ProcessResult result) {
        // 校验并保存 CES (每一行都查一次库，存一次库)
        Optional<ControlExpectation> cesOpt = cesRepository.findByCesId(data.getCesId());
        ControlExpectation cesEntity;
        if (cesOpt.isPresent()) {
            cesEntity = cesOpt.get();
            cesEntity.setCesDesc(data.getCesStatement());
            result.addUpdated();
        } else {
            cesEntity = new ControlExpectation();
            cesEntity.setCesId(data.getCesId());
            cesEntity.setCesDesc(data.getCesStatement());
            result.addInserted();
        }
        cesRepository.save(cesEntity); // 频繁写库

        // 拆分 CEAM 并保存映射 (循环内处理多对多)
        if (data.getCeamIds() != null && !data.getCeamIds().isEmpty()) {
            String[] ceamArray = data.getCeamIds().split("\\|");
            for (String ceamId : ceamArray) {
                RcmMappingEntity mapping = new RcmMappingEntity();
                mapping.setObId(data.getObligationId());
                mapping.setCesId(data.getCesId());
                mapping.setCeamId(ceamId.trim());
                rcmMappingRepository.save(mapping); // 又是频繁写库
            }
            result.addMappings(ceamArray.length);
        }
    }

}
