package com.now.nowbot.mapper

import com.now.nowbot.entity.bind.QQBindLite
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.JpaSpecificationExecutor
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.transaction.annotation.Transactional

interface BindQQMapper : JpaRepository<QQBindLite, Long>, JpaSpecificationExecutor<QQBindLite> {
    @Query("select qq from QQBindLite qb where qb.osuUser.osuID = :userID")
    fun findByOsuID(userID: Long): QQBindLite?

    @Query("select count(o) from OsuBindUserLite o where o.osuID = :userID") fun countByUserID(osuID: Long): Int

    @Modifying @Transactional @Query("delete from QQBindLite qb where qb.osuUser.osuID = :userID and qb.qq != :qq")
    fun deleteOutdatedBind(userID: Long, qq: Long): Int

    @Modifying @Transactional @Query("delete from QQBindLite qb where qb.osuUser.osuID = :userID")
    fun unBind(userID: Long)

    @Query("select qb.qq as qid, qb.osuUser.osuID as uid, qb.osuUser.osuName as name from QQBindLite qb where qb.qq in (:qq)")
    fun findAllUserByQQ(qq: Collection<Long>): List<QQBindLite.QQUser>
}
