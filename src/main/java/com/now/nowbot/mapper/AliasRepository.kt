package com.now.nowbot.mapper

import com.now.nowbot.entity.Alias
import com.now.nowbot.entity.AliasTitle
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface AliasRepository : JpaRepository<Alias, Long> {

    // 因为 value 字段是唯一且规范化后的，可以直接精确查找
    fun findByValue(value: String): Alias?

    // 检查某个别名是否存在
    fun existsByValue(value: String): Boolean
}

interface TitleRepository : JpaRepository<AliasTitle, Long> {

    /**
     * 根据 ID 查询标题，并使用 JOIN FETCH 一次性把别名加载出来
     * 这样可以防止在事务外部访问 aliases 时抛出 LazyInitializationException
     */
    @Query("SELECT DISTINCT t FROM AliasTitle t LEFT JOIN FETCH t.aliases WHERE t.id = :id")
    fun findByIdWithAliases(@Param("id") id: Long): AliasTitle?

    /**
     * 根据标题名称查询（利用 Spring Data JPA 的方法名解析）
     * 注意：请确保 AliasTitle 实体类中有一个名为 name 的属性。
     * 如果属性名叫 titleName，请修改方法名为 findByTitleName(titleName: String)
     */
    fun findByName(name: String): AliasTitle?

    /**
     * 反向查询：通过别名（标准化后的值）查找所有包含该别名的标题列表
     */
    @Query("SELECT DISTINCT t FROM AliasTitle t JOIN t.aliases a WHERE a.value = :aliasValue")
    fun findByAliasValue(@Param("aliasValue") aliasValue: String): List<AliasTitle>

    /**
     * 批量查询多个标题及其别名，避免 N+1 问题
     */
    @Query("SELECT DISTINCT t FROM AliasTitle t LEFT JOIN FETCH t.aliases")
    override fun findAll(): List<AliasTitle>
}