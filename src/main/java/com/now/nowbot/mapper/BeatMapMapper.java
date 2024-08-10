package com.now.nowbot.mapper;

import com.now.nowbot.entity.BeatmapLite;
import com.now.nowbot.entity.MapSetLite;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

public interface BeatMapMapper extends JpaRepository<BeatmapLite, Long>, JpaSpecificationExecutor<BeatmapLite> {
    void deleteBeatmapLiteById(Long id);
    @Query("select b.mapSet from BeatmapLite b where b.id = :bid")
    java.util.Optional<MapSetLite> getMapSetByBid(Long bid);
}
