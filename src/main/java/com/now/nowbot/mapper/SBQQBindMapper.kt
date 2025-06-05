package com.now.nowbot.mapper

import com.now.nowbot.entity.bind.SBQQBindLite.QQUser
import com.now.nowbot.entity.bind.SBQQBindLite
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.JpaSpecificationExecutor
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.transaction.annotation.Transactional

interface SBQQBindMapper: JpaRepository<SBQQBindLite, Long>, JpaSpecificationExecutor<SBQQBindLite> {

    @Query("select qq from SBQQBindLite qb where qb.bindUserLite.userID = :userID")
    fun findByUserID(userID: Long?): SBQQBindLite?

    @Query("select count(o) from SBBindUserLite o where o.userID = :userID") fun countByUserID(userID: Long?): Int

    @Modifying @Transactional @Query("delete from SBQQBindLite qb where qb.bindUserLite.userID = :userID and qb.qq != :qq")
    fun deleteOtherBind(userID: Long, qq: Long)

    @Modifying @Transactional @Query("delete from SBQQBindLite qb where qb.qq = :qq")
    fun unBindQQ(qq: Long?)
    
    @Modifying @Transactional @Query("delete from SBQQBindLite qb where qb.bindUserLite.userID = :userID")
    fun unBind(userID: Long)

    @Query("select qq.qq as qid, qq.bindUserLite.userID as uid, qq.bindUserLite.username as name from SBQQBindLite qq where qq.qq in (:qq)")
    fun findAllUserByQQ(qq: Collection<Long>?): List<QQUser>
}