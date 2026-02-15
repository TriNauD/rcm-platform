package com.bank.rcm.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ProcessResult {
    private int totalInserted = 0;
    private int totalUpdated = 0;
    private int mappingCount = 0;

    public void addInserted() {
        this.totalInserted++;
    }

    public void addUpdated() {
        this.totalUpdated++;
    }

    public void addMappings(int count) {
        this.mappingCount += count;
    }

    public String toSummaryString() {
        return String.format(
                "Summary: Updated %d CES, Inserted %d CES, Created %d Mappings.",
                this.totalUpdated, this.totalInserted, this.mappingCount);
    }
}
