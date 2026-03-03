package com.bank.rcm.service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.bank.rcm.annotation.MonitorPerformance;
import com.bank.rcm.constant.AppConstants;
import com.bank.rcm.dto.ProcessResult;
import com.bank.rcm.dto.StepBDto;

import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class ComplianceStepBService {
    // ============ 常量定义 ============
    /** 批量处理每批记录数 */
    private static final int QUEUE_SIZE = AppConstants.QUEUE_SIZE;
    private static final int BATCH_SIZE = AppConstants.BATCH_SIZE;
    private static final String POISON_PILL = AppConstants.POISON_PILL;

    @Autowired
    private CubeInternalService cubeInternalService;

    @Autowired
    private ComplianceWriterService complianceWriterService;

    @Resource(name = "cubeCheckExecutor")
    private Executor cubeCheckExecutor;

    @Autowired
    private ExcelService excelService;

    /**
     * 处理Step B合规验证文件
     * 
     * @param file 待处理的Excel文件
     * @return 处理结果摘要字符串，包含成功/失败数据统计
     * @throws IOException 文件读取异常时抛出
     */
    @MonitorPerformance("Zero-Memory-Streaming")
    public String processStepBFile(MultipartFile file) throws IOException {
        ProcessResult result = new ProcessResult();
        BlockingQueue<StepBDto> taskQueue = new LinkedBlockingQueue<>(QUEUE_SIZE);
        AtomicInteger rowCount = new AtomicInteger(0);

        // 启动消费者线程
        CompletableFuture<Void> consumerTask = CompletableFuture.runAsync(() -> {
            consumeQueue(taskQueue, result);
        }, cubeCheckExecutor);

        // 读取excel，放入taskQueue中
        try {
            excelService.readExcelInStream(file.getInputStream(), StepBDto.class, data -> {
                try {
                    int currentCount = rowCount.incrementAndGet();
                    // 每 1000 条打印一次日志
                    if (currentCount % 1000 == 0) {
                        log.info("[生产者监控] 当前行: {}, 队列剩余空间: {}",
                                currentCount, taskQueue.remainingCapacity());
                    }
                    // 每次读取都塞入队列
                    taskQueue.put(data);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException("生产者入队被中断", e);
                }
            });
        } finally {
            try {
                // 放入结束标志
                taskQueue.put(new StepBDto(AppConstants.POISON_PILL));
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        consumerTask.join();

        return result.toSummaryString();
    }

    private void consumeQueue(BlockingQueue<StepBDto> queue, ProcessResult result) {
        List<StepBDto> buffer = new ArrayList<>(BATCH_SIZE);
        long batchCount = 0;

        while (true) {
            // 打印当前队列堆积情况
            if (++batchCount % 10 == 0) { // 每处理 10 个 Batch 打印一次
                log.info("[消费者监控] 当前 taskQueue 堆积量: {}", queue.size());
            }
            try {
                // 可能会等几秒等数据进管道
                StepBDto data = queue.poll(5, TimeUnit.SECONDS);
                // 如果队列里没数据，就处理buffer里的内容
                if (data == null || POISON_PILL.equals(data.getObligationId())) {
                    if (!buffer.isEmpty()) {
                        processBatch(buffer, result);
                    }
                    break;
                }
                // 否则就一直加数据，直到凑够一个batch一起处理
                buffer.add(data);
                if (buffer.size() >= BATCH_SIZE) {
                    processBatch(buffer, result);
                    buffer.clear();
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }

    private void processBatch(List<StepBDto> batch, ProcessResult result) {
        Set<String> batchObIds = batch.stream().map(StepBDto::getObligationId).collect(Collectors.toSet());

        Map<String, Boolean> preCheckMap = preCheckObAsync(batchObIds);

        complianceWriterService.processAndSaveBatch(batch, result, preCheckMap);
    }

    /**
     * 异步并发校验ObligationId合法性
     * <p>
     * 使用线程池异步并发调用外部Cube接口验证ObligationId的合法性。
     * 异常情况下默认标记为不合法(false)，并记录错误日志便于后续排查。
     * </p>
     *
     * @param obIds 待校验的ObligationId集合
     * @return 校验结果映射表，key为ObligationId，value为合法性标志(true=合法, false=不合法)
     */
    public Map<String, Boolean> preCheckObAsync(Set<String> obIds) {
        long startTime = System.currentTimeMillis(); // 记录起始时间
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
                }, cubeCheckExecutor)) // 指定线程池执行器为cubeCheckExecutor，实现异步并发
                .collect(Collectors.toList());

        // 等待所有异步任务全部完成，使用allOf和join的组合确保所有校验操作执行完毕
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

        long duration = System.currentTimeMillis() - startTime;
        // 关键日志：如果 100 条数据的校验耗时超过了 1000ms，说明网络或接口是瓶颈
        log.info("[耗时监控] 批次校验完成，数量: {}, 耗时: {} ms", obIds.size(), duration);

        return resultMap;
    }
}
