package com.now.nowbot.mapper;

import com.now.nowbot.entity.BeatmapLite;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface BeatMapMapper extends JpaRepository<BeatmapLite, Long>, JpaSpecificationExecutor<BeatmapLite> {
    void deleteBeatmapLiteById(Long id);
}
