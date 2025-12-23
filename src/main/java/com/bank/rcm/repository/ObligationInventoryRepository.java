package com.bank.rcm.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.bank.rcm.entity.ComplianceInventory;

@Repository
public interface ObligationInventoryRepository extends JpaRepository<ComplianceInventory, Long> {
    Optional<ComplianceInventory> findByObligationId(String obligationId);
}