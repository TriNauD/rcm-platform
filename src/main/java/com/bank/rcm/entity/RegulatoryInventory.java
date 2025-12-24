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

    @Column(name = "publication_id", unique = true, nullable = false)
    private String publicationId;

    private String publicationName;

    private String regulator;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt = LocalDateTime.now();

    private String complianceStatus;

    @Column(length = 1000)
    private String validationResult;
}
