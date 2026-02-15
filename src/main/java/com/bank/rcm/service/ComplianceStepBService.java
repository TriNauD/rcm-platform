package com.bank.rcm.service;

import java.io.IOException;
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

    @MonitorPerformance("EasyExcel-Initial-Migration")
    public String processStepBFileWithEasyExcel(MultipartFile file) throws IOException {
        ProcessResult result = new ProcessResult();

        // excelService每在stream读到一个StepBDto，都把它作为参数丢进processSingleRow去处理
        excelService.readExcelInStream(
                file.getInputStream(),
                StepBDto.class,
                data -> {
                    processSingleRow(data, result);
                });

        // 解析完成后，返回汇总信息
        return result.toSummaryString();
    }

    @Transactional
    public void processSingleRow(StepBDto data, ProcessResult res) {
        // 校验 OB (CUBE 接口同步调用 - 最大的性能黑洞)
        boolean isObValid = cubeInternalSerice.validateWithCube(data.getObligationId());
        if (!isObValid) {
            log.warn("Invalid OB ID: {}", data.getObligationId());
            return;
        }

        // 校验并保存 CES (每一行都查一次库，存一次库)
        Optional<ControlExpectation> cesOpt = cesRepository.findByCesId(data.getCesId());
        ControlExpectation cesEntity;
        if (cesOpt.isPresent()) {
            cesEntity = cesOpt.get();
            cesEntity.setCesDesc(data.getCesStatement());
            res.addUpdated();
        } else {
            cesEntity = new ControlExpectation();
            cesEntity.setCesId(data.getCesId());
            cesEntity.setCesDesc(data.getCesStatement());
            res.addInserted();
            ;
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
            res.addMappings(ceamArray.length);
        }
    }

}
