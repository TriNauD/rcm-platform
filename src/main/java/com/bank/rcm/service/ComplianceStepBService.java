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
import com.bank.rcm.constant.AppConstants;
import com.bank.rcm.dto.ProcessResult;
import com.bank.rcm.dto.StepBDto;
import com.google.common.collect.Lists;

import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class ComplianceStepBService {
    // ============ 常量定义 ============
    /** 批量处理每批记录数 */
    private static final int BATCH_SIZE = AppConstants.BATCH_SIZE;

    @Autowired
    private ExcelService excelService;

    @Autowired
    private CubeInternalService cubeInternalService;

    @Autowired
    private ComplianceWriterService complianceWriterService;

    @Resource(name = "cubeCheckExecutor")
    private Executor cubeCheckExecutor;

    /**
     * 处理Step B合规验证文件
     * <p>流程：收集文件数据 -> 异步校验OB合法性 -> 批量入库处理</p>
     *
     * @param file 待处理的Excel文件
     * @return 处理结果摘要字符串，包含成功/失败数据统计
     * @throws IOException 文件读取异常时抛出
     */
    @MonitorPerformance("EasyExcel-Multithread-Process")
    public String processStepBFile(MultipartFile file) throws IOException {
        // 创建处理结果对象，用于收集本次处理的统计数据（成功、失败、映射关系等）
        ProcessResult result = new ProcessResult();

        // ============ 第一步：收集数据 ============
        // 使用Set存储唯一的ObligationId，避免List的O(n)包含检查，提升性能至O(1)
        Set<String> allUniqueObIds = new HashSet<>();
        // 使用List存储所有读取的行数据，保持原始顺序便于后续批量处理
        List<StepBDto> allRecords = new ArrayList<>();

        // 执行流式读取，从Excel文件中提取数据和UniqueObId
        collectDataAndIds(file, allUniqueObIds, allRecords);

        // 检查是否成功提取到有效的ObligationId，若为空则无需进行后续校验和入库
        if (CollectionUtils.isEmpty(allUniqueObIds)) {
            log.warn("OB日志：文件解析完成，但未提取到任何有效的 ObligationId");
            return result.toSummaryString(); // 直接返回，省去后续所有开销
        }

        // ============ 第二步：异步校验 ============
        // 调用线程池异步并发校验所有ObligationId的合法性，返回校验结果映射表
        Map<String, Boolean> preCheckMap = preCheckObAsync(allUniqueObIds);

        // ============ 第三步：业务处理和批量入库 ============
        // 根据预检验结果筛选合法数据，进行实体组装和批量数据库入库处理
        dispatchSaveTasks(allRecords, preCheckMap, result);

        // 返回本次处理的结果摘要（包含新增、更新、映射关系等统计数据）
        return result.toSummaryString();
    }

    /**
     * 流式读取Excel文件，收集数据和唯一的ObligationId
     * <p>使用流式读取方式降低内存占用，通过Set去重ObId以实现O(1)的包含检查</p>
     *
     * @param file 待读取的Excel文件
     * @param allUniqueObIds 存储所有唯一的ObligationId，使用Set类型便于去重
     * @param allRecords 存储所有读取的记录数据
     * @throws IOException 文件读取异常时抛出
     */
    private void collectDataAndIds(MultipartFile file, Set<String> allUniqueObIds, List<StepBDto> allRecords)
            throws IOException {
        // 使用ExcelService的流式读取方式处理Excel文件，避免大文件一次性加载到内存
        // 为每一行数据执行Consumer回调函数，实现对流的即时处理
        excelService.readExcelInStream(
                file.getInputStream(),
                StepBDto.class,
                data -> {
                    allRecords.add(data);
                    allUniqueObIds.add(data.getObligationId());
                });
    }

    /**
     * 异步并发校验ObligationId合法性
     * <p>使用线程池异步并发调用外部Cube接口验证ObligationId的合法性。
     * 异常情况下默认标记为不合法(false)，并记录错误日志便于后续排查。</p>
     *
     * @param obIds 待校验的ObligationId集合
     * @return 校验结果映射表，key为ObligationId，value为合法性标志(true=合法, false=不合法)
     */
    public Map<String, Boolean> preCheckObAsync(Set<String> obIds) {
        // 初始化并发哈希表，指定初始容量为obIds的大小，防止扩容时的性能开销
        // ConcurrentHashMap保证在并发环境下的线程安全性（多个异步线程并发写入）
        Map<String, Boolean> resultMap = new ConcurrentHashMap<>(obIds.size());

        if (obIds == null || obIds.isEmpty()) {
            return resultMap;
        }

        // 将Set中的每个ObligationId转换为一个异步任务（CompletableFuture）
        List<CompletableFuture<Void>> futures = obIds.stream()
                .map(id -> CompletableFuture.runAsync(() -> {
                    try {
                        // 调用外部服务Cube接口验证当前ObligationId的合法性
                        boolean isValid = cubeInternalService.validateWithCube(id);
                        // 将校验结果存储到结果映射表中（true=合法, false=不合法）
                        resultMap.put(id, isValid);
                    } catch (Exception e) {
                        // 异常捕获：若校验过程中发生任何异常，则记录错误日志
                        log.error("OB报错：校验异常 ID {}", id, e);
                        // 异常情况下默认标记为不合法(false)，确保不合法数据不会入库
                        resultMap.put(id, false);
                    }
                }, cubeCheckExecutor))  // 指定线程池执行器为cubeCheckExecutor，实现异步并发
                .collect(Collectors.toList());

        // 等待所有异步任务全部完成，使用allOf和join的组合确保所有校验操作执行完毕
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

        return resultMap;
    }

    /**
     * 批量分发处理和保存任务
     * <p>将所有记录按指定批大小(BATCH_SIZE)进行分片处理，每批次独立事务执行，
     * 结合预检验结果进行业务处理和数据库入库。</p>
     *
     * @param allRecords 全量待处理的记录列表
     * @param preCheckMap ObligationId的预检验结果映射表
     * @param result 处理结果对象，用于统计成功/失败数据
     * @throws IOException 数据写入异常时抛出
     */
    private void dispatchSaveTasks(List<StepBDto> allRecords, Map<String, Boolean> preCheckMap,
            ProcessResult result)
            throws IOException {
        // 使用Google Guava的Lists.partition方法将全量数据按BATCH_SIZE分片
        List<List<StepBDto>> batches = Lists.partition(allRecords, BATCH_SIZE);

        // 遍历每个批次，逐批次投递到业务处理服务
        for (List<StepBDto> batch : batches) {
            // 调用ComplianceWriterService处理单个批次
            // 该方法内部采用独立事务(REQUIRES_NEW)执行，确保批次间的事务隔离
            complianceWriterService.processAndSaveBatch(batch, result, preCheckMap);
        }

        log.info("{} 所有 {} 批次分发完毕，共计 {} 条记录", AppConstants.LOG_OB_PREFIX, batches.size(), allRecords.size());
    }

}
