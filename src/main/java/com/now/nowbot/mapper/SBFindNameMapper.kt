package com.now.nowbot.mapper

import com.now.nowbot.entity.SBNameToIDLite
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.JpaSpecificationExecutor
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.transaction.annotation.Transactional

interface SBFindNameMapper : JpaRepository<SBNameToIDLite, Long>, JpaSpecificationExecutor<SBNameToIDLite> {
    override fun <S : SBNameToIDLite?> saveAll(iterable: Iterable<S>): List<S>

    @Modifying @Transactional @Query("delete from SBNameToIDLite o where o.name = :name")
    fun deleteByName(name: String?)

    @Modifying @Transactional @Query("delete from SBNameToIDLite o where o.userID = :userID")
    fun deleteByUserID(userID: Long?)

    @Query("select count(*) from SBNameToIDLite o where o.userID = :userID")
    fun countByUserID(userID: Long?): Int

    @Query(""" 
        SELECT o.userID FROM SBNameToIDLite o WHERE o.name ILIKE :name ORDER BY o.index ASC LIMIT 1
    """)
    fun getUserIDByUsernameIgnoreCase(name: String?): Long?

    @Query("SELECT o.name FROM SBNameToIDLite o WHERE o.userID = :userID ORDER BY o.index ASC LIMIT 1")
    fun getUsername(userID: Long?): String?
}
