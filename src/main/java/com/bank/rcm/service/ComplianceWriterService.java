package com.bank.rcm.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.bank.rcm.dto.ProcessResult;
import com.bank.rcm.dto.StepBDto;
import com.bank.rcm.entity.ControlExpectation;
import com.bank.rcm.mapper.RcmMappingEntity;
import com.bank.rcm.repository.ControlExpectationRepository;
import com.bank.rcm.repository.RcmMappingRepository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
 
@Service
public class ComplianceWriterService {

    @Autowired
    private RcmMappingRepository rcmMappingRepository;

    @Autowired
    private ControlExpectationRepository cesRepository;

    @PersistenceContext
    private EntityManager entityManager;

    // 遍历组装每一条数据后，批量保存
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void processAndSaveBatch(List<StepBDto> batch, ProcessResult result, Map<String, Boolean> preCheckMap) {
        List<ControlExpectation> cesList = new ArrayList<>();
        List<RcmMappingEntity> mappingList = new ArrayList<>();

        for (StepBDto data : batch) {
            // 如果非法则跳过
            if (!preCheckMap.get(data.getObligationId())) {
                continue;
            }
            // 组装待写入的数据
            processEntity(data, cesList, mappingList, result);
        }

        // 批量写入数据
        cesRepository.saveAll(cesList);
        rcmMappingRepository.saveAll(mappingList);

        cesList.clear();
        mappingList.clear();

        // 刷新并清除
        entityManager.flush(); // 将缓存中的变更立即同步到数据库
        entityManager.clear(); // 清除一级缓存，释放内存空间
    }

    // 组装数据，放进ces和mapping的list里，不操作数据库
    private void processEntity(StepBDto data, List<ControlExpectation> cesList, List<RcmMappingEntity> mappingList,
            ProcessResult result) {
        // 校验CES，存入list，结果中计数
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
        cesList.add(cesEntity);

        // split拆分CEAM，存入list，结果中计数
        if (data.getCeamIds() != null && !data.getCeamIds().isEmpty()) {
            String[] ceamArray = data.getCeamIds().split("\\|");
            for (String ceamId : ceamArray) {
                RcmMappingEntity mapping = new RcmMappingEntity();
                mapping.setObId(data.getObligationId());
                mapping.setCesId(data.getCesId());
                mapping.setCeamId(ceamId.trim());

                mappingList.add(mapping);
            }
            result.addMappings(ceamArray.length);
        }
    }

}
