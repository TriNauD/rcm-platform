package com.bank.rcm.service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.web.multipart.MultipartFile;

import com.bank.rcm.annotation.MonitorPerformance;
import com.bank.rcm.dto.ProcessResult;
import com.bank.rcm.dto.StepBDto;
import com.bank.rcm.entity.ControlExpectation;
import com.bank.rcm.mapper.RcmMappingEntity;
import com.bank.rcm.repository.ControlExpectationRepository;
import com.bank.rcm.repository.RcmMappingRepository;

import jakarta.annotation.Resource;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class ComplianceStepBService {

    @Autowired
    private ExcelService excelService;

    @Autowired
    private CubeInternalService cubeInternalService;

    @Autowired
    private RcmMappingRepository rcmMappingRepository;

    @Autowired
    private ControlExpectationRepository cesRepository;

    @PersistenceContext
    private EntityManager entityManager;

    @Resource(name = "cubeCheckExecutor")
    private Executor cubeCheckExecutor;

    @MonitorPerformance("EasyExcel-Multithread-Process")
    public String processStepBFile(MultipartFile file) throws IOException {
        ProcessResult result = new ProcessResult();

        // 扫描文件，得到所有唯一的obid
        Set<String> allUniqueObIds = collectUniqueObIds(file);

        if (CollectionUtils.isEmpty(allUniqueObIds)) {
            log.warn("文件解析完成，但未提取到任何有效的 ObligationId");
            return result.toSummaryString(); // 直接返回，省去后续所有开销
        }

        // 异步校验
        Map<String, Boolean> preCheckMap = preCheckObAsync(allUniqueObIds);

        // 业务处理，批量入库
        processAndSaveWithResults(file, preCheckMap, result);

        return result.toSummaryString();
    }

    private Set<String> collectUniqueObIds(MultipartFile file) throws IOException {

        Set<String> uniqueObIds = new HashSet<>();// 使用 Set 而非 List，将 O(n) 的包含检查降为 O(1)

        excelService.readExcelInStream(
                file.getInputStream(),
                StepBDto.class,
                data -> {
                    uniqueObIds.add(data.getObligationId());
                });

        return uniqueObIds;
    }

    // 开线程池异步并发请求cube，得到ob合法性map
    public Map<String, Boolean> preCheckObAsync(Set<String> obIds) {
        Map<String, Boolean> resultMap = new ConcurrentHashMap<>(obIds.size());//指定size，防止后续扩容

        if (obIds == null || obIds.isEmpty()) {
            return resultMap;
        }

        List<CompletableFuture<Void>> futures = obIds.stream()
                .map(id -> CompletableFuture.runAsync(() -> {
                    try {
                        // 外部接口调用
                        boolean isValid = cubeInternalService.validateWithCube(id);
                        resultMap.put(id, isValid);
                    } catch (Exception e) {
                        log.error("OB校验异常 ID: {}", id, e);
                        resultMap.put(id, false); // 默认失败或根据业务定
                    }
                }, cubeCheckExecutor))
                .collect(Collectors.toList());

        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

        return resultMap;
    }

    // 批量保存结果
    private void processAndSaveWithResults(MultipartFile file, Map<String, Boolean> preCheckMap, ProcessResult result)
            throws IOException {
        // 用于保存DTO用于批处理
        List<StepBDto> batchBuffer = new ArrayList<>();
        final int BATCH_SIZE = 100;

        // excelService每在stream读到一个StepBDto，都把它作为参数丢进调用的方法去处理
        excelService.readExcelInStream(
                file.getInputStream(),
                StepBDto.class,
                data -> {
                    batchBuffer.add(data);
                    if (batchBuffer.size() >= BATCH_SIZE) {
                        processAndSaveBatch(batchBuffer, result, preCheckMap);
                        batchBuffer.clear();
                    }
                });

        // 收尾最后未满100条的
        if (!batchBuffer.isEmpty()) {
            processAndSaveBatch(batchBuffer, result, preCheckMap);
            batchBuffer.clear();
        }
    }

    private void processAndSaveBatch(List<StepBDto> batch, ProcessResult result, Map<String, Boolean> preCheckMap) {
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

        // // 刷新并清除
        // entityManager.flush(); // 将缓存中的变更立即同步到数据库
        // entityManager.clear(); // 清除一级缓存，释放内存空间
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
