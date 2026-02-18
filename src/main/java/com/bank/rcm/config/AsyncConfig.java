package com.bank.rcm.config;

import java.util.concurrent.Executor;

import java.util.concurrent.ThreadPoolExecutor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration
public class AsyncConfig {
    @Bean(name = "cubeCheckExecutor")
    public Executor cubeCheckExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();

        // 核心线程数：根据你的 Cube 接口性能定。
        // 如果 Cube 接口能承受，可以设为 10-20。
        executor.setCorePoolSize(15);

        // 最大线程数
        executor.setMaxPoolSize(30);

        // 队列容量：防止请求瞬间爆发撑爆内存
        executor.setQueueCapacity(500);

        // 线程前缀：在 JConsole 监控时，你能一眼看到 "CubeAsync-xx" 的线程在跳动
        executor.setThreadNamePrefix("CubeAsync-");

        // 拒绝策略：当池子和队列都满了，让调用者线程（主线程）自己执行，起到降速作用
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());

        // 等待所有任务结束后再关闭线程池
        executor.setWaitForTasksToCompleteOnShutdown(true);

        executor.initialize();

        return executor;
    }
}
