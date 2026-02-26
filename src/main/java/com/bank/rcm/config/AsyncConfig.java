package com.bank.rcm.config;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import com.bank.rcm.constant.AppConstants;

/**
 * 异步任务执行线程池配置
 * <p>为Cube接口的异步校验提供独立的线程池，实现并发性能优化</p>
 */
@Configuration
public class AsyncConfig {
    /**
     * 创建Cube异步校验线程池
     * <p>配置参数说明：
     * <ul>
     *   <li>核心线程数：根据Cube接口吞吐量配置，默认15</li>
     *   <li>最大线程数：防止高峰期资源溢出，默认30</li>
     *   <li>队列容量：缓冲突发流量，默认500</li>
     *   <li>拒绝策略：队列满时由调用线程执行，起到自动降速作用</li>
     * </ul>
     * </p>
     */
    @Bean(name = "cubeCheckExecutor")
    public Executor cubeCheckExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();

        // 核心线程数：根据Cube接口性能和系统资源定
        executor.setCorePoolSize(AppConstants.EXECUTOR_CUBE_CORE_SIZE);

        // 最大线程数：防止请求瞬间爆发撑爆内存
        executor.setMaxPoolSize(AppConstants.EXECUTOR_CUBE_MAX_SIZE);

        // 队列容量：防止请求瞬间爆发撑爆内存
        executor.setQueueCapacity(AppConstants.EXECUTOR_CUBE_QUEUE_CAPACITY);

        // 线程名前缀：便于在JConsole中识别和监控Cube异步线程
        executor.setThreadNamePrefix(AppConstants.EXECUTOR_CUBE_THREAD_PREFIX);

        // 拒绝策略：当线程池和队列都满时，由调用者线程执行，起到自动降速作用
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());

        // 关闭策略：等待所有任务完成后再关闭线程池
        executor.setWaitForTasksToCompleteOnShutdown(true);

        executor.initialize();

        return executor;
    }
}
