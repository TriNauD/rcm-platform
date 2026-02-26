package com.bank.rcm.service;

import java.util.Random;

import org.springframework.stereotype.Service;

import com.bank.rcm.constant.AppConstants;

import lombok.extern.slf4j.Slf4j;

/**
 * 外部Cube服务集成
 * <p>负责调用外部Cube接口进行ObligationId合法性验证</p>
 */
@Service
@Slf4j
public class CubeInternalService {
    /** 随机数生成器（避免频繁创建新实例） */
    private static final Random RANDOM = new Random();
    
    /**
     * 通过Cube接口验证ObligationId的合法性
     *
     * @param complianceId 待验证的ObligationId
     * @return true表示合法，false表示不合法
     */
    public boolean validateWithCube(String complianceId) {
        // 参数校验：若为空则直接返回不合法
        if (complianceId == null || complianceId.trim().isEmpty()) {
            log.warn("{} 输入的ObligationId为空", AppConstants.LOG_CUBE_PREFIX);
            return false;
        }
        
        try {
            // 模拟调用外部Cube接口，每次查询需要指定延迟时间
            Thread.sleep(AppConstants.CUBE_CALL_DELAY_MS);
            // 模拟结果valid/invalid，按配置的概率返回valid数据
            return RANDOM.nextInt(100) < AppConstants.VALID_DATA_PROBABILITY;
        } catch (Exception e) {
            log.error("{} ObligationId验证异常: {}", AppConstants.LOG_CUBE_PREFIX, complianceId, e);
            Thread.currentThread().interrupt();
            return false;
        }
    }
}
