package com.bank.rcm.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.bank.rcm.entity.CeamDefinition;
import java.util.Optional;


@Repository
public interface CeamRepository extends JpaRepository<CeamDefinition, Long> {
    Optional<CeamDefinition> findByCeamId(String ceamId);
}
