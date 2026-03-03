package com.bank.rcm.repository;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.bank.rcm.entity.ControlExpectation;


@Repository
public interface ControlExpectationRepository extends JpaRepository<ControlExpectation, Long> {
    Optional<ControlExpectation> findByCesId(String cesId);

    List<ControlExpectation> findAllByCesIdIn(Set<String> cesIds);
}
