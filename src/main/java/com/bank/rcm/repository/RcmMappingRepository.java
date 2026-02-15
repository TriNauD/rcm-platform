package com.bank.rcm.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.bank.rcm.mapper.RcmMappingEntity;

@Repository
public interface RcmMappingRepository extends JpaRepository<RcmMappingEntity, Long> {

}
