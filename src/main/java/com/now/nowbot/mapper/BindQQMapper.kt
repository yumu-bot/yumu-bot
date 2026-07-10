package com.now.nowbot.mapper

import com.now.nowbot.entity.bind.QQBindLite
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.JpaSpecificationExecutor
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.transaction.annotation.Transactional

interface BindQQMapper : JpaRepository<QQBindLite, Long>, JpaSpecificationExecutor<QQBindLite> {
    @Query("select qb from QQBindLite qb where qb.osuUser.id = :id")
    fun findByID(id: Long): QQBindLite?

    @Query("select qb from QQBindLite qb where qb.osuUser.userID in (:userIDs)")
    fun findAllByUserID(userIDs: Iterable<Long>): List<QQBindLite>

    @Query("select qb from QQBindLite qb where qb.osuUser.userID = :userID")
    fun findByUserID(userID: Long): QQBindLite?

    @Query("select count(o) from OsuBindUserLite o where o.userID = :userID")
    fun countByUserID(userID: Long): Int

    @Modifying @Transactional @Query("delete from QQBindLite qb where qb.osuUser.userID = :userID and qb.qq != :qq")
    fun deleteOutdatedBind(userID: Long, qq: Long): Int

    @Modifying @Transactional @Query("delete from QQBindLite qb where qb.osuUser.userID = :userID")
    fun unBind(userID: Long)

    @Query("select qb.qq as qid, qb.osuUser.userID as uid, qb.osuUser.username as name from QQBindLite qb where qb.qq in (:qq)")
    fun findAllUserByQQ(qq: Iterable<Long>): List<QQBindLite.QQUser>
}
