package com.bank.rcm.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

@Entity
@Table(name = "regulatory_inventory")
@Data
public class RegulatoryInventory {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String publicationId;

    private String publicationName;

    private String regulator;

    @Column(name = "regulator_tier")
    private String regulatorTier;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt = LocalDateTime.now();
}
