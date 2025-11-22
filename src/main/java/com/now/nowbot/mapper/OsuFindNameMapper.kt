package com.now.nowbot.mapper

import com.now.nowbot.entity.OsuNameToIDLite
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.JpaSpecificationExecutor
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.transaction.annotation.Transactional

interface OsuFindNameMapper : JpaRepository<OsuNameToIDLite, Long>, JpaSpecificationExecutor<OsuNameToIDLite> {
    override fun <S : OsuNameToIDLite> saveAll(iterable: Iterable<S>): List<S>

    @Modifying @Transactional @Query("delete from OsuNameToIDLite o where o.name = :name")
    fun deleteByName(name: String?)

    @Modifying @Transactional @Query("delete from OsuNameToIDLite o where o.userID = :userID")
    fun deleteByUserID(userID: Long?)

    @Query("select count(*) from OsuNameToIDLite o where o.userID = :userID")
    fun countByUserID(userID: Long?): Int

    @Query(""" 
        SELECT o.userID FROM OsuNameToIDLite o WHERE o.name ILIKE :name ORDER BY o.index ASC LIMIT 1
    """)
    fun getUserIDByUsernameIgnoreCase(name: String?): Long?

    @Query("SELECT o.name FROM OsuNameToIDLite o WHERE o.userID = :userID ORDER BY o.index ASC LIMIT 1")
    fun getUsername(userID: Long?): String?
}
