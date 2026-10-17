package com.now.nowbot.mapper

import com.now.nowbot.entity.Alias
import com.now.nowbot.entity.AliasTitle
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface AliasRepository : JpaRepository<Alias, Long> {

    // 因为 value 字段是唯一且规范化后的，可以直接精确查找
    fun findByValue(value: String): Alias?

    // 批量按别名值查询，供导入时一次性取回已存在的别名
    fun findAllByValueIn(values: Collection<String>): List<Alias>

    // 检查某个别名是否存在
    fun existsByValue(value: String): Boolean

    /**
     * 查询所有还没有通过审核的别名，并一次性把关联的标题加载出来
     * 供管理员审核时展示。
     */
    @Query("SELECT DISTINCT a FROM Alias a LEFT JOIN FETCH a.titles WHERE a.approved = false ORDER BY a.id")
    fun findAllUnapproved(): List<Alias>
}

interface TitleRepository : JpaRepository<AliasTitle, Long> {

    @Query("SELECT DISTINCT t FROM AliasTitle t LEFT JOIN FETCH t.aliases WHERE t.id = :id")
    fun findByIdWithAliases(@Param("id") id: Long): AliasTitle?

    @Query("SELECT DISTINCT t FROM AliasTitle t LEFT JOIN FETCH t.aliases WHERE t.name = :name")
    fun findByNameWithAliases(@Param("name") name: String): AliasTitle?

    /**
     * 批量按标题名查询标题及其别名，一次查询取回所有标题，避免逐个标题查询造成的 N+1。
     */
    @Query("SELECT DISTINCT t FROM AliasTitle t LEFT JOIN FETCH t.aliases WHERE t.name IN :names")
    fun findByNameInWithAliases(@Param("names") names: Collection<String>): List<AliasTitle>

    fun findByName(name: String): AliasTitle?

    @Query("SELECT DISTINCT t FROM AliasTitle t JOIN t.aliases a WHERE a.value = :aliasValue")
    fun findByAliasValue(@Param("aliasValue") aliasValue: String): List<AliasTitle>

    @Query("SELECT DISTINCT t FROM AliasTitle t LEFT JOIN FETCH t.aliases")
    override fun findAll(): List<AliasTitle>
}