package com.now.nowbot.mapper;

import com.now.nowbot.entity.OsuBindUserLite;
import com.now.nowbot.model.enums.OsuMode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;


public interface BindUserMapper extends JpaRepository<OsuBindUserLite, Long>, JpaSpecificationExecutor<OsuBindUserLite> {


    Optional<OsuBindUserLite> getByOsuId(Long osuId);

    Optional<OsuBindUserLite> getByOsuNameLike(String osuName);

    @Modifying
    @Transactional
    @Query(value = """
            with del as (
                select id, row_number() over (partition by osu_id=:uid order by id desc ) as row_num
                from osu_bind_user
                where osu_id=:uid
            )
            delete from osu_bind_user using del where osu_bind_user.id = del.id and del.row_num >1;
            """, nativeQuery = true)
    void deleteOldByOsuId(Long uid);

    @Modifying
    @Transactional
    @Query("update OsuBindUserLite o set o.accessToken = :accessToken,o.refreshToken = :refreshToken, o.time = :time where o.osuId=:uid")
    void updateToken(Long uid, String accessToken, String refreshToken, Long time);

    @Modifying
    @Transactional
    @Query("update OsuBindUserLite o set o.mainMode = :mode where o.osuId = :uid ")
    void updateMode(Long uid, OsuMode mode);


    @Modifying
    @Transactional
    @Query("delete OsuBindUserLite o where o.osuId = :uid ")
    void deleteByOsuId(Long uid);

    @Modifying
    @Transactional
    @Query("update OsuBindUserLite o set o.accessToken = null , o.refreshToken = null , o.time = null, o.updateCount = 0 where o.osuId = :uid ")
    void backupBindByOsuId(Long uid);

    @Modifying
    @Transactional
    @Query(value = "update osu_bind_user set update_count = update_count + 1 where id=:id", nativeQuery = true)
    void addUpdateCount(Long id);


    @Modifying
    @Transactional
    @Query(value = "update osu_bind_user set update_count = 0 where id=:id", nativeQuery = true)
    void clearUpdateCount(Long id);

    @Modifying
    @Transactional
    @Query("delete QQBindLite q where q.osuUser.osuId = :uid ")
    void deleteQQByOsuId(Long uid);
    @Modifying
    @Transactional
    @Query("delete DiscordBindLite d where d.osuUser.osuId = :uid ")
    void deleteDCByOsuId(Long uid);

    @Query("select u from OsuBindUserLite u where u.time > 5000 and u.time < :now and u.updateCount = 0 order by u.time limit 50")
    List<OsuBindUserLite> getOldBindUser(Long now);

    @Query("select u from OsuBindUserLite u where u.updateCount > 0 and u.time > 5000 order by u.time limit 50")
    List<OsuBindUserLite> getOldBindUserHasWrong(Long now);

    @Transactional
    default void deleteAllByOsuId(Long uid){
        deleteQQByOsuId(uid);
        deleteDCByOsuId(uid);
        deleteByOsuId(uid);
    }

    int countAllByOsuId(long osuId);

    @Transactional
    default <S extends OsuBindUserLite> S checkSave(S entity) {
        if (entity.getID() == null && countAllByOsuId(entity.getOsuID()) > 0) {
            deleteOldByOsuId(entity.getOsuID());
        }

        return save(entity);
    }
}
