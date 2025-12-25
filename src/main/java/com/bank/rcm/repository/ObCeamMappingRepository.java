package com.bank.rcm.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.bank.rcm.mapper.ObCeamMapping;
import com.google.common.base.Optional;


@Repository
public interface ObCeamMappingRepository extends JpaRepository<ObCeamMapping, Long> {
    // 根据 Obligation ID 获取关联的 CEAM 路径字符串
    Optional<ObCeamMapping> findByObligationId(String obligationId);

    // 删除特定 Obligation 的所有 CEAM 映射
    void deleteByObligationId(String obligationId);
}
