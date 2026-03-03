package com.bank.rcm.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class StepBDto {
    private String obligationId;
    private String cesId;
    private String cesStatement;
    private String ceamIds; // "CEAM1|CEAM2"

    public StepBDto(String obligationId) {
        this.obligationId = obligationId;
    }

}
