package com.now.nowbot.mapper

import com.now.nowbot.entity.OsuNameToIDLite
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.JpaSpecificationExecutor
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.transaction.annotation.Transactional

interface OsuFindNameMapper : JpaRepository<OsuNameToIDLite, Long>, JpaSpecificationExecutor<OsuNameToIDLite> {
    override fun <S : OsuNameToIDLite> saveAll(iterable: Iterable<S>): List<S>

    @Modifying @Transactional
    @Query("delete from OsuNameToIDLite o where o.name = :name")
    fun deleteByName(name: String?)

    @Modifying @Transactional
    @Query("delete from OsuNameToIDLite o where o.userID = :userID")
    fun deleteByUserID(userID: Long?)

    @Query("select count(*) from OsuNameToIDLite o where o.userID = :userID")
    fun countByUserID(userID: Long?): Int

    // 💡 修改重点：将 ILIKE 改为 LOWER(o.name) = LOWER(:name)，完美吃上 idx_osu_name_id_lower_name 函数索引
    @Query(""" 
        SELECT o.userID FROM OsuNameToIDLite o 
        WHERE LOWER(o.name) = LOWER(:name) 
        ORDER BY o.index ASC LIMIT 1
    """)
    fun getUserIDByUsernameIgnoreCase(name: String?): Long?

    @Query("SELECT o.name FROM OsuNameToIDLite o WHERE o.userID = :userID ORDER BY o.index ASC LIMIT 1")
    fun getUsername(userID: Long?): String?
}