package com.now.nowbot.mapper;

import com.now.nowbot.entity.OsuUserInfoArchiveLite;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.Optional;

public interface OsuUserInfoMapper extends JpaRepository<OsuUserInfoArchiveLite, Long>, JpaSpecificationExecutor<OsuUserInfoArchiveLite> {
    OsuUserInfoArchiveLite getFirstByOsuIDOrderByTimeDesc(long osuid);

    @Query("select o from OsuUserInfoArchiveLite o where o.osuID = :osuId and (o.time between :time1 and :time2) order by o.time desc ")
    Optional<OsuUserInfoArchiveLite> selectDayLast(Long osuId, LocalDateTime time1, LocalDateTime time2);
    @Query("select o from OsuUserInfoArchiveLite o where o.osuID = :osuId order by o.time desc ")
    Optional<OsuUserInfoArchiveLite> selectLast(Long osuId);

}
