package com.now.nowbot.mapper

import com.now.nowbot.entity.Alias
import com.now.nowbot.entity.AliasTitle
import org.springframework.data.domain.Pageable
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

    /**
     * 按别名创建时间倒序取含有多个别名的标题，供 `!aj` 无参数时展示。
     *
     * 标题的排序依据是该标题下最新创建的别名时间；只有一个别名的标题无法调整顺序，
     * 直接在查询里排除，这样即使最近更新的标题大多是单别名，也能凑满 [Pageable] 请求的数量。
     *
     * 这里刻意不 JOIN FETCH aliases：分页和集合抓取一起用会导致 Hibernate 在内存里分页，
     * 实际返回的标题数量可能少于 limit 条。别名由 DAO 在事务内逐个补取。
     */
    @Query(
        """
        SELECT t FROM AliasTitle t
        JOIN t.aliases a
        GROUP BY t
        HAVING COUNT(a) > 1
        ORDER BY MAX(a.createdAt) DESC, t.id DESC
        """
    )
    fun findRecentTitles(pageable: Pageable): List<AliasTitle>

    @Query("SELECT DISTINCT t FROM AliasTitle t JOIN t.aliases a WHERE a.value = :aliasValue")
    fun findByAliasValue(@Param("aliasValue") aliasValue: String): List<AliasTitle>

    @Query("SELECT DISTINCT t FROM AliasTitle t LEFT JOIN FETCH t.aliases")
    override fun findAll(): List<AliasTitle>
}