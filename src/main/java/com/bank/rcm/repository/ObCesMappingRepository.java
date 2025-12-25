package com.bank.rcm.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.bank.rcm.mapper.ObCesMapping;
import java.util.List;

@Repository
public interface ObCesMappingRepository extends JpaRepository<ObCesMapping, Long> {
    // 根据 Obligation ID 查询所有的 CES 映射关系
    List<ObCesMapping> findByObligationId(String obligationId);

    // 在 Update 模式下，可能需要先删除旧的关系再建立新的
    void deleteByObligationId(String obligationId);

    // 检查某个特定的映射是否存在
    boolean existsByObligationIdAndCesId(String obligationId, String cesId);
}
