package com.bank.rcm.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.bank.rcm.constant.AppConstants;
import com.bank.rcm.dto.ProcessResult;
import com.bank.rcm.dto.StepBDto;
import com.bank.rcm.entity.ControlExpectation;
import com.bank.rcm.mapper.RcmMappingEntity;
import com.bank.rcm.repository.ControlExpectationRepository;
import com.bank.rcm.repository.RcmMappingRepository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class ComplianceWriterService {
    // ============ 常量定义 ============
    /** CEAM ID 拆分分隔符 */
    private static final String CEAM_SPLIT_CHAR = AppConstants.CEAM_SPLIT_CHAR;

    @Autowired
    private RcmMappingRepository rcmMappingRepository;

    @Autowired
    private ControlExpectationRepository cesRepository;

    @PersistenceContext
    private EntityManager entityManager;

    /**
     * 处理并批量保存Step B数据
     * <p>该方法采用独立事务执行，先根据预检结果过滤合法数据，然后进行实体组装，
     * 最后批量入库。通过EntityManager的flush和clear操作优化内存使用，避免大量数据
     * 导致的内存溢出。</p>
     *
     * @param batch 待处理的数据批次，通常为100条记录
     * @param result 处理结果对象，用于统计成功/失败的数据数量
     * @param preCheckMap ObligationId的预检结果映射表，value为true表示合法，false表示不合法
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void processAndSaveBatch(List<StepBDto> batch, ProcessResult result, Map<String, Boolean> preCheckMap) {
        List<ControlExpectation> cesList = new ArrayList<>();
        List<RcmMappingEntity> mappingList = new ArrayList<>();

        // 遍历批次中的每条数据进行处理
        for (StepBDto data : batch) {
            // 根据预检结果过滤：获取当前ObligationId的合法性标记，若为null或false则跳过
            Boolean isValid = preCheckMap.get(data.getObligationId());
            if (isValid == null || !isValid) {
                continue;
            }
            // 对合法数据进行实体组装，将数据转换为数据库实体对象
            processEntity(data, cesList, mappingList, result);
        }

        // 批量保存ces实体
        cesRepository.saveAll(cesList);
        // 批量保存RcmMappingEntity实体（RCM映射关系表）
        rcmMappingRepository.saveAll(mappingList);

        // 清空临时列表，便于垃圾回收
        cesList.clear();
        mappingList.clear();

        // 将持久化上下文中的所有变更立即同步到数据库，确保数据已提交
        entityManager.flush();
        // 清除Hibernate一级缓存中的所有实体对象，释放内存空间，防止大批量数据处理时的内存溢出
        entityManager.clear();
    }

    /**
     * 组装Step B DTO为数据库实体对象
     * <p>该方法不直接操作数据库，只进行数据转换和组装：
     * <ul>
     *   <li>处理ControlExpectation实体：如果CES存在则更新，否则新增</li>
     *   <li>处理RcmMappingEntity实体：根据CEAM ID列表拆分并创建多条映射关系</li>
     *   <li>更新处理结果统计：记录新增/更新/映射关系数量</li>
     * </ul>
     * </p>
     *
     * @param data 单条Step B DTO数据
     * @param cesList ControlExpectation实体列表（输出参数，收集组装结果）
     * @param mappingList RcmMappingEntity实体列表（输出参数，收集组装结果）
     * @param result 处理结果对象（输出参数，累计统计数据）
     */
    private void processEntity(StepBDto data, List<ControlExpectation> cesList, List<RcmMappingEntity> mappingList,
            ProcessResult result) {
        // 校验CES（ControlExpectation）是否已存在
        Optional<ControlExpectation> cesOpt = cesRepository.findByCesId(data.getCesId());
        ControlExpectation cesEntity;
        
        if (cesOpt.isPresent()) {
            // CES已存在：则更新其描述信息，并统计更新数
            cesEntity = cesOpt.get();
            cesEntity.setCesDesc(data.getCesStatement());
            result.addUpdated();
        } else {
            // CES不存在：则创建新对象，并统计新增数
            cesEntity = new ControlExpectation();
            cesEntity.setCesId(data.getCesId());
            cesEntity.setCesDesc(data.getCesStatement());
            result.addInserted();
        }
        // 将组装好的CES实体添加到列表中，待后续批量保存
        cesList.add(cesEntity);

        // 处理CEAM ID列表：按管道符(|)进行分割，为每个CEAM创建一个RCM映射关系
        if (data.getCeamIds() != null && !data.getCeamIds().isEmpty()) {
            String[] ceamArray = data.getCeamIds().split(CEAM_SPLIT_CHAR);
            for (String ceamId : ceamArray) {
                // 为每个CEAM创建一个新的映射实体
                RcmMappingEntity mapping = new RcmMappingEntity();
                mapping.setObId(data.getObligationId());
                mapping.setCesId(data.getCesId());
                mapping.setCeamId(ceamId.trim());

                // 将映射关系添加到列表中，待后续批量保存
                mappingList.add(mapping);
            }
            // 累计统计创建的映射关系数量
            result.addMappings(ceamArray.length);
        }
    }

}
