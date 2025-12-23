package com.bank.rcm.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.bank.rcm.entity.RegulatoryInventory;

@Repository
public interface RegulatoryInventoryRepository extends JpaRepository<RegulatoryInventory, Long> {

}