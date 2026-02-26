package com.bank.rcm.aspect;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import com.bank.rcm.annotation.MonitorPerformance;
import com.bank.rcm.constant.AppConstants;

@Aspect
@Component
@Slf4j
public class PerformanceAspect {
    @Around("@annotation(monitorPerformance)")
    public Object measureTime(ProceedingJoinPoint joinPoint, MonitorPerformance monitorPerformance) throws Throwable {
        long startTime = System.currentTimeMillis();
        try {
            // 执行原方法逻辑
            return joinPoint.proceed();
        } finally {
            long endTime = System.currentTimeMillis();
            long duration = endTime - startTime;

            if (duration <= AppConstants.PERF_THRESHOLD_INFO) {
                log.info("{} 任务: [{}], 耗时: {} ms",
                        AppConstants.LOG_MONITOR_PREFIX, monitorPerformance.value(), duration);
            } else if (duration <= AppConstants.PERF_THRESHOLD_WARN) {
                log.warn("{} 任务: [{}], 耗时: {} ms",
                        AppConstants.LOG_MONITOR_PREFIX, monitorPerformance.value(), duration);
            } else {
                String extraInfo = getExtraContext(joinPoint.getArgs());
                log.error("{} 任务: [{}], 耗时: {} ms {}",
                        AppConstants.LOG_MONITOR_PREFIX, monitorPerformance.value(), duration, extraInfo);
            }

        }
    }

    private String getExtraContext(Object[] args) {
        for (Object arg : args) {
            if (arg instanceof MultipartFile file) {
                return String.format("| 文件: %s [%d bytes]",
                        file.getOriginalFilename(), file.getSize());
            }
        }
        return "";
    }

}