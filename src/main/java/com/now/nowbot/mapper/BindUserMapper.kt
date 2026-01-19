package com.now.nowbot.mapper

import com.now.nowbot.entity.OsuBindUserLite
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.JpaSpecificationExecutor
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.transaction.annotation.Transactional

interface BindUserMapper : JpaRepository<OsuBindUserLite, Long>, JpaSpecificationExecutor<OsuBindUserLite> {
    @Query("select u from OsuBindUserLite u where u.osuID = :osuID order by u.id limit 1")
    fun getByOsuID(osuID: Long?): OsuBindUserLite?

    @Query(
        """
        select u from OsuBindUserLite u
        where u.osuID = :osuID and u.refreshToken is not null
        order by u.id desc limit 1
        """
    )
    fun getFirstByOsuID(osuID: Long?): OsuBindUserLite?

    fun getByOsuNameLike(osuName: String?): OsuBindUserLite?

    @Modifying @Transactional @Query(
        value = """
            with del as (
                select id, row_number() over (partition by osu_id=:osuID order by id desc ) as row_num
                from osu_bind_user
                where osu_id=:osuID
            )
            delete from osu_bind_user using del where osu_bind_user.id = del.id and del.row_num >1;
            """, nativeQuery = true
    )
    fun deleteOutdatedByOsuID(osuID: Long?)

    @Modifying @Transactional
    @Query("update OsuBindUserLite o set o.accessToken = :accessToken,o.refreshToken = :refreshToken, o.time = :time where o.osuID=:osuID")
    fun updateToken(osuID: Long?, accessToken: String?, refreshToken: String?, time: Long?)

    @Modifying @Transactional @Query("update OsuBindUserLite o set o.modeValue = :modeValue where o.osuID = :osuID ")
    fun updateMode(osuID: Long?, modeValue: Byte?)

    @Modifying @Transactional @Query("delete OsuBindUserLite o where o.osuID = :osuID ") fun deleteByOsuID(osuID: Long?)

    @Modifying @Transactional
    @Query("update OsuBindUserLite o set o.accessToken = null , o.refreshToken = null , o.time = null, o.updateCount = 0 where o.osuID = :osuID ")
    fun backupBindByOsuID(osuID: Long?)

    @Modifying @Transactional
    @Query(value = "update osu_bind_user set update_count = update_count + 1 where id=:id", nativeQuery = true)
    fun addUpdateCount(id: Long?)

    @Modifying @Transactional
    @Query(value = "update osu_bind_user set update_count = 0 where id=:id", nativeQuery = true)
    fun clearUpdateCount(id: Long?)

    @Query("select u from OsuBindUserLite u where u.osuID in (:osuID)")
    fun getAllByOsuID(osuID: Collection<Long>): List<OsuBindUserLite>

    @Modifying @Transactional @Query("delete QQBindLite q where q.osuUser.osuID = :osuID ")
    fun deleteQQByOsuID(osuID: Long?)

    @Modifying @Transactional @Query("delete DiscordBindLite d where d.osuUser.osuID = :osuID ") fun deleteDCByOsuID(
        osuID: Long?
    )

    @Query("select u from OsuBindUserLite u where u.time > 5000 and u.time < :now and u.updateCount = 0 order by u.time limit 50")
    fun getOldBindUser(now: Long?): List<OsuBindUserLite>

    @Query("select u from OsuBindUserLite u where u.updateCount > 0 and u.time > 5000 order by u.time limit 50")
    fun getOldBindUserHasWrong(now: Long?): List<OsuBindUserLite>

    @Query("select u from OsuBindUserLite u where u.time > 5000 and u.time < :now and u.updateCount = 0 order by u.time limit 1")
    fun getOneOldBindUser(now: Long?): OsuBindUserLite?

    @Query("select u from OsuBindUserLite u where u.updateCount > 0 and u.time > 5000 order by u.time limit 1")
    fun getOneOldBindUserHasWrong(now: Long?): OsuBindUserLite?

    @Transactional
    fun deleteAllByOsuID(osuID: Long?) {
        deleteQQByOsuID(osuID)
        deleteDCByOsuID(osuID)
        deleteByOsuID(osuID)
    }

    fun countAllByOsuID(osuID: Long): Int

    @Query("select u.osuID from OsuBindUserLite u order by u.osuID limit 50 offset :offset")
    fun getBindUserIDs(offset: Int): List<Long>

    // 查找 osuID 大于 lastID 的前 50 个，按 osuID 升序排列
    @Query("select u.osuID from OsuBindUserLite u where u.osuID > :lastID order by u.osuID limit 50")
    fun getNextBindUserIDs(lastID: Long): List<Long>
}
