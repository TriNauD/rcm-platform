package com.bank.rcm.service;

import java.util.Random;

import org.springframework.stereotype.Service;

@Service
public class CubeInternalSerice {
    public boolean validateWithCube(String publicationId) {
        try {
            // mock calling service
            Thread.sleep(300);
            if (publicationId == null || !publicationId.startsWith("PUB-")) {
                return false;
            }
            return new Random().nextBoolean();
        } catch (Exception e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }
}
