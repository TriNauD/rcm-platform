package com.bank.rcm.mapper;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

@Data
@Entity
@Table(name = "rcm_mapping")
public class RcmMappingEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(name = "ob_id", nullable = false)
    private String obId;

    @Column(name = "ces_id", nullable = false)
    private String cesId;

    @Column(name = "ceam_id")
    private String ceamId;

    @Column(name = "mapping_type")
    private Integer mappingType;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

}
