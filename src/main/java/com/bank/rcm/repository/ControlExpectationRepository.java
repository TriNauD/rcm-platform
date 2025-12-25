package com.bank.rcm.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.bank.rcm.entity.ControlExpectation;


@Repository
public interface ControlExpectationRepository extends JpaRepository<ControlExpectation, Long> {
    Optional<ControlExpectation> findByCesId(String cesId);
}
