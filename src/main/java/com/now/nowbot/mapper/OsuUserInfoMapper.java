package com.now.nowbot.mapper;

import com.now.nowbot.entity.OsuUserInfoArchiveLite;
import com.now.nowbot.model.enums.OsuMode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

public interface OsuUserInfoMapper extends JpaRepository<OsuUserInfoArchiveLite, Long>, JpaSpecificationExecutor<OsuUserInfoArchiveLite> {
    OsuUserInfoArchiveLite getFirstByOsuIDOrderByTimeDesc(long osuid);

    default Optional<OsuUserInfoArchiveLite> selectDayLast(Long osuId, OsuMode mode, LocalDate date) {
        return selectDayLast(osuId, mode, LocalDateTime.of(date, LocalTime.MIN), LocalDateTime.of(date, LocalTime.MAX));
    }

    default List<OsuUserInfoArchiveLite.InfoArchive> getArchiveByUidYesterday(List<Long> uid) {
        var time = LocalDate.now().minusDays(1);
        return getArchiveByUid(uid, LocalDateTime.of(time, LocalTime.MIN), LocalDateTime.of(time, LocalTime.MAX));
    }

    @Query(value = """
                select distinct on (osu_id, mode)
                osu_id, mode as mode_short, play_count
                from (
                    select osu_id, mode, play_count from osu_user_info_archive
                    where osu_id in (:uid) and time between :time1 and :time2 and pp > 0
                    order by play_count desc
                ) as shadow
                limit 1
                """, nativeQuery = true)
    List<OsuUserInfoArchiveLite.InfoArchive> getArchiveByUid(List<Long> uid, LocalDateTime time1, LocalDateTime time2);

    @Query("""
           select o from OsuUserInfoArchiveLite o
           where o.osuID = :osuId and o.mode = :mode and (o.time between :time1 and :time2)
           order by o.time desc limit 1
           """)
    Optional<OsuUserInfoArchiveLite> selectDayLast(Long osuId, OsuMode mode, LocalDateTime time1, LocalDateTime time2);

    @Query("select o from OsuUserInfoArchiveLite o where o.osuID = :osuId and o.mode = :mode order by o.time desc limit 1")
    Optional<OsuUserInfoArchiveLite> selectLast(Long osuId, OsuMode mode);

}
