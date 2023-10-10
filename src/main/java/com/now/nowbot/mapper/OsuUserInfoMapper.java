package com.now.nowbot.mapper;

import com.now.nowbot.entity.OsuUserModeScoreLite;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Component;

@Component
public interface OsuUserInfoMapper extends JpaRepository<OsuUserModeScoreLite, Long>, JpaSpecificationExecutor<OsuUserModeScoreLite> {
    OsuUserModeScoreLite getFirstByOsuIDOrderByTimeDesc(long osuid);

    @Query("from OsuUserModeScoreLite o where o.osuID = :osuId and o.time > :time order by o.time")
    void deleteByName(Long osuId, Long time);

}
