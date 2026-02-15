package com.bank.rcm.aspect;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import com.bank.rcm.annotation.MonitorPerformance;

@Aspect
@Component
@Slf4j
// 切面，对 @MonitorPerformance 标记的方法，进行执行时间测试，打出log
public class PerformanceAspect {

    // 拦截所有标记了 @MonitorPerformance 的方法
    @Around("@annotation(monitorPerformance)")
    public Object measureTime(ProceedingJoinPoint joinPoint, MonitorPerformance monitorPerformance) throws Throwable {
        long startTime = System.currentTimeMillis();
        try {
            // 执行原方法逻辑
            return joinPoint.proceed();
        } finally {
            long endTime = System.currentTimeMillis();
            long duration = endTime - startTime;

            if (duration <= 1000) {
                log.info("==== [OB 监控报告] 任务: [{}], 耗时: {} ms====",
                        monitorPerformance.value(), duration);
            } else if (duration > 1000 && duration < 3000) {
                log.warn("==== [OB 监控报告] 任务: [{}], 耗时: {} ms====",
                        monitorPerformance.value(), duration);
            } else {
                // 处理时间过长，报error，和更详细的信息
                // 获取额外信息，如文件信息
                String extraInfo = getExtraContext(joinPoint.getArgs());
                log.error("==== 任务: [{}], 耗时: {} ms {} ====",
                        monitorPerformance.value(), duration, extraInfo);
            }

        }
    }

    // 从参数堆里翻文件
    private String getExtraContext(Object[] args) {
        for (Object arg : args) {
            if (arg instanceof MultipartFile file) {
                return String.format(" | 文件: %s [%d bytes]",
                        file.getOriginalFilename(), file.getSize());
            }
        }
        return ""; // 如果不是文件方法，就返回空字符串，不产生噪音
    }

}