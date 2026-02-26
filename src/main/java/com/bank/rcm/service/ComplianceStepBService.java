package com.bank.rcm.service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
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

import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class ComplianceStepBService {

    @Autowired
    private ExcelService excelService;

    @Autowired
    private CubeInternalService cubeInternalService;

    @Autowired
    private ComplianceWriterService complianceWriterService;

    @Resource(name = "cubeCheckExecutor")
    private Executor cubeCheckExecutor;

    @MonitorPerformance("EasyExcel-Multithread-Process")
    public String processStepBFile(MultipartFile file) throws IOException {
        ProcessResult result = new ProcessResult();

        // 1. 收集数据
        Set<String> allUniqueObIds = new HashSet<>();// 扫描文件，得到所有唯一的obid，使用 Set 而非 List，将 O(n) 的包含检查降为 O(1)
        List<StepBDto> allRecords = new ArrayList<>();

        collectDataAndIds(file, allUniqueObIds, allRecords);

        if (CollectionUtils.isEmpty(allUniqueObIds)) {
            log.warn("文件解析完成，但未提取到任何有效的 ObligationId");
            return result.toSummaryString(); // 直接返回，省去后续所有开销
        }

        // 2. 异步校验
        Map<String, Boolean> preCheckMap = preCheckObAsync(allUniqueObIds);

        // 3. 业务处理，批量入库
        dipatchSaveTasks(allRecords, preCheckMap, result);

        return result.toSummaryString();
    }

    // 流式读取excel文件
    private void collectDataAndIds(MultipartFile file, Set<String> allUniqueObIds, List<StepBDto> allRecords)
            throws IOException {
        // 流式读取文件
        excelService.readExcelInStream(
                file.getInputStream(),
                StepBDto.class,
                data -> {
                    allRecords.add(data);
                    allUniqueObIds.add(data.getObligationId());
                });
    }

    // 开线程池异步并发请求cube，得到ob合法性map
    public Map<String, Boolean> preCheckObAsync(Set<String> obIds) {
        Map<String, Boolean> resultMap = new ConcurrentHashMap<>(obIds.size());// 指定size，防止后续扩容

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

    // 批量分发保存结果
    private void dipatchSaveTasks(List<StepBDto> allRecords, Map<String, Boolean> preCheckMap,
            ProcessResult result)
            throws IOException {
        final int BATCH_SIZE = 100;

        for (int i = 0; i < allRecords.size(); i += BATCH_SIZE) {
            int end = Math.min(i + BATCH_SIZE, allRecords.size());
            List<StepBDto> batch = allRecords.subList(i, end);

            // 派发任务。每一批次独立事务
            complianceWriterService.processAndSaveBatch(batch, result, preCheckMap);
        }
    }

}
