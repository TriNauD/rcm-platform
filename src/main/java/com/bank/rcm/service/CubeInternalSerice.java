package com.bank.rcm.service;

import java.util.Random;

import org.springframework.stereotype.Service;

@Service
public class CubeInternalSerice {
    public boolean validateWithCube(String complianceId) {
        try {
            // mock calling cube service
            Thread.sleep(300);
            if (complianceId == null) {
                return false;
            }
            // mock cube validate result
            return new Random().nextBoolean();
        } catch (Exception e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }
}
