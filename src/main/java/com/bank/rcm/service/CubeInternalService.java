package com.bank.rcm.service;

import java.util.Random;

import org.springframework.stereotype.Service;

@Service
public class CubeInternalService {
    public boolean validateWithCube(String complianceId) {
        try {
            // 模拟调用外部cube接口，每次查询需要200ms
            Thread.sleep(200);
            if (complianceId == null) {
                return false;
            }
            // 模拟结果valid/invalid，90%为valid数据
            return new Random().nextInt(100) < 90;
        } catch (Exception e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }
}
