package com.now.nowbot.mapper;

import com.now.nowbot.entity.BeatMapFileLite;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;

public interface BeatMapFileRepository extends JpaRepository<BeatMapFileLite, Long> , JpaSpecificationExecutor<BeatMapFileLite> {
    Optional<BeatMapFileLite> findBeatMapFileRepositoriesByBid(long bid);
}
