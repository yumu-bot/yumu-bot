package com.now.nowbot.mapper;

import com.now.nowbot.entity.PerformancePlusLite;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface PerformancePlusLiteRepository extends JpaRepository<PerformancePlusLite, Long>, JpaSpecificationExecutor<PerformancePlusLite> {
    // 通过id批量查询已有的PerformancePlusLite
    @Query("select p from PerformancePlusLite p where p.id in (:ids) and p.type = 0")
    List<PerformancePlusLite> findScorePPP(Iterable<Long> ids);

    @Query("select p from PerformancePlusLite p where p.id in (:ids) and p.type = 1")
    List<PerformancePlusLite> findBeatMapPPP(Iterable<Long> ids);

    @Query("select p from PerformancePlusLite p where p.id = :id and p.type = 1")
    Optional<PerformancePlusLite> findBeatMapPPPById(Long id);
}