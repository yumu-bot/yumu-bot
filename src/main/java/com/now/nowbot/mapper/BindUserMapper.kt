package com.now.nowbot.mapper;

import com.now.nowbot.entity.OsuBindUserLite;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;
import java.util.Optional;


public interface BindUserMapper extends JpaRepository<OsuBindUserLite, Long>, JpaSpecificationExecutor<OsuBindUserLite> {

    @Query("select u from OsuBindUserLite u where u.osuID = :osuID order by u.id limit 1")
    Optional<OsuBindUserLite> getByOsuID(Long osuID);

    @Query("""
        select u from OsuBindUserLite u
        where u.osuID = :osuID and u.refreshToken is not null
        order by u.id desc limit 1
        """)
    Optional<OsuBindUserLite> getFirstByOsuID(Long osuID);

    Optional<OsuBindUserLite> getByOsuNameLike(String osuName);

    @Modifying
    @Transactional
    @Query(value = """
            with del as (
                select id, row_number() over (partition by osu_id=:osuID order by id desc ) as row_num
                from osu_bind_user
                where osu_id=:osuID
            )
            delete from osu_bind_user using del where osu_bind_user.id = del.id and del.row_num >1;
            """, nativeQuery = true)
    void deleteOutdatedByOsuID(Long osuID);

    @Modifying
    @Transactional
    @Query("update OsuBindUserLite o set o.accessToken = :accessToken,o.refreshToken = :refreshToken, o.time = :time where o.osuID=:osuID")
    void updateToken(Long osuID, String accessToken, String refreshToken, Long time);

    @Modifying
    @Transactional
    @Query("update OsuBindUserLite o set o.modeValue = :modeValue where o.osuID = :osuID ")
    void updateMode(Long osuID, Byte modeValue);

    @Modifying
    @Transactional
    @Query("delete OsuBindUserLite o where o.osuID = :osuID ")
    void deleteByOsuID(Long osuID);

    @Modifying
    @Transactional
    @Query("update OsuBindUserLite o set o.accessToken = null , o.refreshToken = null , o.time = null, o.updateCount = 0 where o.osuID = :osuID ")
    void backupBindByOsuID(Long osuID);

    @Modifying
    @Transactional
    @Query(value = "update osu_bind_user set update_count = update_count + 1 where id=:id", nativeQuery = true)
    void addUpdateCount(Long id);


    @Modifying
    @Transactional
    @Query(value = "update osu_bind_user set update_count = 0 where id=:id", nativeQuery = true)
    void clearUpdateCount(Long id);

    @Query("select u from OsuBindUserLite u where u.osuID in (:osuID)")
    List<OsuBindUserLite> getAllByOsuID(Collection<Long> osuID);

    @Modifying
    @Transactional
    @Query("delete QQBindLite q where q.osuUser.osuID = :osuID ")
    void deleteQQByOsuID(Long osuID);

    @Modifying
    @Transactional
    @Query("delete DiscordBindLite d where d.osuUser.osuID = :osuID ")
    void deleteDCByOsuID(Long osuID);

    @Query("select u from OsuBindUserLite u where u.time > 5000 and u.time < :now and u.updateCount = 0 order by u.time limit 50")
    List<OsuBindUserLite> getOldBindUser(Long now);

    @Query("select u from OsuBindUserLite u where u.updateCount > 0 and u.time > 5000 order by u.time limit 50")
    List<OsuBindUserLite> getOldBindUserHasWrong(Long now);

    @Query("select u from OsuBindUserLite u where u.time > 5000 and u.time < :now and u.updateCount = 0 order by u.time limit 1")
    Optional<OsuBindUserLite> getOneOldBindUser(Long now);

    @Query("select u from OsuBindUserLite u where u.updateCount > 0 and u.time > 5000 order by u.time limit 1")
    Optional<OsuBindUserLite> getOneOldBindUserHasWrong(Long now);

    @Transactional
    default void deleteAllByOsuID(Long osuID){
        deleteQQByOsuID(osuID);
        deleteDCByOsuID(osuID);
        deleteByOsuID(osuID);
    }

    int countAllByOsuID(long osuID);

    @Transactional
    default <S extends OsuBindUserLite> S checkSave(S entity) {
        if (entity.getId() == null && countAllByOsuID(entity.getOsuID()) > 0) {
            deleteOutdatedByOsuID(entity.getOsuID());
        }

        return save(entity);
    }

    @Query("select u.osuID from OsuBindUserLite u order by u.osuID limit 50 offset :offset")
    List<Long> getAllBindUserIdLimit50(int offset);
}
