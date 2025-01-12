package com.now.nowbot.mapper;

import com.now.nowbot.entity.BeatmapLite;
import com.now.nowbot.entity.MapSetLite;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import java.util.Collection;
import java.util.List;

public interface BeatMapMapper extends JpaRepository<BeatmapLite, Long>, JpaSpecificationExecutor<BeatmapLite> {
    void deleteBeatmapLiteById(Long id);
    @Query("select b.mapSet from BeatmapLite b where b.id = :bid")
    java.util.Optional<MapSetLite> getMapSetByBid(Long bid);

    @Query(value = """
        select id as id, hit_length as length from osu_beatmap where id in (:id)
        """, nativeQuery = true)
    List<BeatmapLite.BeatmapHitLengthResult> getBeatmapHitLength(Collection<Long> id);
}
