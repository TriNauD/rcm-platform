package com.bank.rcm.dto;

import lombok.Data;

@Data
public class StepBDto {
    private String obligationId; 
    private String cesId;
    private String cesStatement;
    private String ceamIds; // "CEAM1|CEAM2"
}
