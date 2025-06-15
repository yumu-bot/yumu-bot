package com.now.nowbot.mapper

import com.now.nowbot.entity.OsuNameToIdLite
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.JpaSpecificationExecutor
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.transaction.annotation.Transactional

interface OsuFindNameMapper : JpaRepository<OsuNameToIdLite?, Long?>, JpaSpecificationExecutor<OsuNameToIdLite?> {
    override fun <S : OsuNameToIdLite?> saveAll(iterable: Iterable<S>): List<S>

    @Modifying @Transactional @Query("delete from OsuNameToIdLite o where o.name = :name")
    fun deleteByName(name: String?)

    @Modifying @Transactional @Query("delete from OsuNameToIdLite o where o.uid = :userID")
    fun deleteByUserID(userID: Long?)

    @Query("select count(*) from OsuNameToIdLite o where o.uid = :userID")
    fun countByUserID(userID: Long?): Int

    fun getFirstByNameOrderByIndex(name: String?): OsuNameToIdLite?
}
