package com.bank.rcm.mapper;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

@Entity
@Data
@Table(name = "ob_ceam_mapping")
public class ObCeamMapping {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String obligationId;
    
    // 存储格式为 "CEAM1|CEAM2|CEAM3"
    @Column(name = "ceam_ids", length = 1000)
    private String ceamIds;
}
