package com.bank.rcm.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

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
        // 一次性查出该批次所有存在的 CES，避免在循环里 findByCesId
        Set<String> cesIds = batch.stream().map(StepBDto::getCesId).collect(Collectors.toSet());
        Map<String, ControlExpectation> existingCesMap = cesRepository.findAllByCesIdIn(cesIds)
                .stream().collect(Collectors.toMap(ControlExpectation::getCesId, c -> c));

        List<ControlExpectation> cesToSave = new ArrayList<>();
        List<RcmMappingEntity> mappingsToSave = new ArrayList<>();

        for (StepBDto data : batch) {
            if (!Boolean.TRUE.equals(preCheckMap.get(data.getObligationId())))
                continue;

            // 直接从内存 Map 拿，不再走数据库 IO
            ControlExpectation ces = existingCesMap.getOrDefault(data.getCesId(), null);
            // 对合法数据进行实体组装，将数据转换为数据库实体对象
            processEntity(data, ces, cesToSave, mappingsToSave, result);
        }

        // 配合 Step 7：确保 saveAll 走的是 JDBC Batch
        cesRepository.saveAll(cesToSave);
        rcmMappingRepository.saveAll(mappingsToSave);

        entityManager.flush();
        entityManager.clear(); // 保持内存纯净的关键
    }

    /**
     * 组装Step B DTO为数据库实体对象
     * <p>
     * 该方法不直接操作数据库，只进行数据转换和组装：
     * <ul>
     * <li>处理ControlExpectation实体：如果CES存在则更新，否则新增</li>
     * <li>处理RcmMappingEntity实体：根据CEAM ID列表拆分并创建多条映射关系</li>
     * <li>更新处理结果统计：记录新增/更新/映射关系数量</li>
     * </ul>
     * </p>
     *
     * @param data        单条Step B DTO数据
     * @param ces         数据库查询出的ces结果，如未查到则为null
     * @param cesList     ControlExpectation实体列表（输出参数，收集组装结果）
     * @param mappingList RcmMappingEntity实体列表（输出参数，收集组装结果）
     * @param result      处理结果对象（输出参数，累计统计数据）
     */
    private void processEntity(StepBDto data, ControlExpectation ces, List<ControlExpectation> cesList,
            List<RcmMappingEntity> mappingList,
            ProcessResult result) {
        // 校验CES（ControlExpectation）是否已存在
        if (ces != null) {
            // CES已存在：则更新其描述信息，并统计更新数
            ces.setCesDesc(data.getCesStatement());
            result.addUpdated();
        } else {
            // CES不存在：则创建新对象，并统计新增数
            ces = new ControlExpectation();
            ces.setCesId(data.getCesId());
            ces.setCesDesc(data.getCesStatement());
            result.addInserted();
        }
        // 将组装好的CES实体添加到列表中，待后续批量保存
        cesList.add(ces);

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
