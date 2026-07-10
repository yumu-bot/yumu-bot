package com.now.nowbot.mapper

import com.now.nowbot.entity.OsuBindUserLite
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.JpaSpecificationExecutor
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.transaction.annotation.Transactional

interface BindUserMapper : JpaRepository<OsuBindUserLite, Long>, JpaSpecificationExecutor<OsuBindUserLite> {
    @Query("select u from OsuBindUserLite u where u.userID = :userID order by u.id limit 1")
    fun getByOsuID(userID: Long?): OsuBindUserLite?

    @Query("select u from OsuBindUserLite u where u.refreshToken = :refreshToken order by u.id limit 1")
    fun getByRefreshToken(refreshToken: String?): OsuBindUserLite?

    @Query(
        """
        select u from OsuBindUserLite u
        where u.userID = :userID and u.refreshToken is not null
        order by u.id desc limit 1
        """
    )
    fun getFirstByOsuID(userID: Long?): OsuBindUserLite?

    @Query("select u from OsuBindUserLite u where u.username ILIKE CONCAT('%', :username, '%') order by u.username asc")
    fun getByOsuNameLike(username: String?): List<OsuBindUserLite>

    @Modifying @Transactional @Query(
        value = """
            with del as (
                select id, row_number() over (partition by osu_id=:userID order by id desc ) as row_num
                from osu_bind_user
                where osu_id= :userID
            )
            delete from osu_bind_user using del where osu_bind_user.id = del.id and del.row_num >1;
            """, nativeQuery = true
    )
    fun deleteOutdatedByOsuID(userID: Long?)

    @Modifying @Transactional
    @Query("update OsuBindUserLite o set o.accessToken = :accessToken,o.refreshToken = :refreshToken, o.time = :time where o.userID=:userID")
    fun updateToken(userID: Long?, accessToken: String?, refreshToken: String?, time: Long?)

    @Modifying
    @Transactional
    @Query(value = """
    UPDATE osu_bind_user
    SET osu_id = :#{#user.userID}, 
        osu_name = :#{#user.username}, 
        access_token = :#{#user.accessToken}, 
        refresh_token = :#{#user.refreshToken}, 
        time = :#{#user.time} 
    WHERE id = :#{#user.id}
""", nativeQuery = true)
    fun update(@Param("user") user: OsuBindUserLite)

    @Modifying @Transactional @Query("update OsuBindUserLite o set o.modeValue = :modeValue where o.userID = :userID ")
    fun updateMode(userID: Long?, modeValue: Byte?)

    @Modifying @Transactional @Query("delete OsuBindUserLite o where o.userID = :userID ") fun deleteByOsuID(userID: Long?)

    @Modifying @Transactional
    @Query("update OsuBindUserLite o set o.accessToken = null , o.refreshToken = null , o.time = null, o.updateCount = 0 where o.userID = :userID ")
    fun downgradeBind(userID: Long?)

    @Modifying @Transactional
    @Query(value = "update osu_bind_user set update_count = update_count + 1 where id=:id", nativeQuery = true)
    fun addUpdateCount(id: Long?)

    @Modifying @Transactional
    @Query(value = "update osu_bind_user set update_count = 0 where id=:id", nativeQuery = true)
    fun clearUpdateCount(id: Long?)

    @Query("select u from OsuBindUserLite u where u.userID in (:userID)")
    fun getAllByOsuID(userID: Collection<Long>): List<OsuBindUserLite>

    @Modifying @Transactional @Query("delete QQBindLite q where q.osuUser.userID = :userID ")
    fun deleteQQByOsuID(userID: Long?)

    @Modifying @Transactional @Query("delete DiscordBindLite d where d.osuUser.userID = :userID ") fun deleteDCByOsuID(
        userID: Long?
    )

    @Query("select u from OsuBindUserLite u where u.time > 5000 and u.time < :now and u.updateCount = 0 order by u.time limit 50")
    fun getOldBindUser(now: Long?): List<OsuBindUserLite>

    @Query("select u from OsuBindUserLite u where u.updateCount > 0 and u.time > 5000 order by u.time limit 50")
    fun getOldBindUserHasWrong(now: Long?): List<OsuBindUserLite>

    @Query("select u from OsuBindUserLite u where u.time > 5000 and u.time < :now and u.updateCount = 0 order by u.time limit 1")
    fun getEarliestBindUser(now: Long?): OsuBindUserLite?

    @Query("select u from OsuBindUserLite u where u.updateCount > 0 and u.time > 5000 order by u.time limit 1")
    fun getEarliestSuspiciousBindUser(now: Long?): OsuBindUserLite?

    @Transactional
    fun deleteAllByUserID(userID: Long?) {
        deleteQQByOsuID(userID)
        deleteDCByOsuID(userID)
        deleteByOsuID(userID)
    }

    fun countAllByUserID(userID: Long): Int

    @Query("select u from OsuBindUserLite u order by u.userID limit 50 offset :offset")
    fun getAllBindUserLimit50(offset: Int): List<OsuBindUserLite>
}
